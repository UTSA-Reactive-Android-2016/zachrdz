package com.csandroid.myfirstapp.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.csandroid.myfirstapp.R;
import com.csandroid.myfirstapp.adapters.MessageAdapter;
import com.csandroid.myfirstapp.db.LocalKeyPairDBHandler;
import com.csandroid.myfirstapp.db.MessageDBHandler;
import com.csandroid.myfirstapp.models.LocalKeyPair;
import com.csandroid.myfirstapp.models.Message;
import com.csandroid.myfirstapp.stages.NotificationStage;
import com.csandroid.myfirstapp.utils.Crypto;
import com.csandroid.myfirstapp.utils.Notification;
import com.csandroid.myfirstapp.utils.WebHelper;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity {

    LocalKeyPairDBHandler localKeyPairDB;
    LocalKeyPair localKeyPair;

    Crypto myCrypto;

    private RecyclerView recView;
    private List<Message> recList;
    private MessageAdapter mAdapter;
    private LinearLayoutManager llm;
    ScheduledFuture updateFuture;

    // Subscription holder
    private CompositeSubscription cs = new CompositeSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        if(null != getSupportActionBar()) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean loggedIn = prefs.getBoolean("loggedIn", false);

        // Setup recycler view/list
        this.setupRecyclerView();

        if(loggedIn){
            this.initServer();
        }

        this.updateLoginMsg(loggedIn);
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean loggedIn = prefs.getBoolean("loggedIn", false);

        switch(id){
            case R.id.action_settings :
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(settingsIntent);
                return true;
            case R.id.action_contacts :
                if(loggedIn) {
                    Intent contactsIntent = new Intent(MainActivity.this, ContactsActivity.class);
                    contactsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(contactsIntent);
                } else{
                    Toast.makeText(getApplicationContext(), "You must login first.", Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.action_compose :
                if(loggedIn) {
                    Intent composeIntent = new Intent(MainActivity.this, ComposeActivity.class);
                    composeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(composeIntent);
                } else{
                    Toast.makeText(getApplicationContext(), "You must login first.", Toast.LENGTH_LONG).show();
                }
                return true;
            default :
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        cs.unsubscribe();
        cs = null;
        cs = new CompositeSubscription();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean loggedIn = prefs.getBoolean("loggedIn", false);

        this.setupRecyclerView();
        this.initServer();
        this.updateLoginMsg(loggedIn);
    }

    @Override
    public void onPause(){
        super.onPause();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean loggedIn = prefs.getBoolean("loggedIn", false);

        cs.unsubscribe();
        if (updateFuture != null) {
            updateFuture.cancel(true);
            updateFuture = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cs.unsubscribe();
        cs = null;

        if (updateFuture != null) {
            updateFuture.cancel(true);
            updateFuture = null;
        }
    }

    public void initServer(){
        // Logged in User setup for server
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String username = prefs.getString("username","");
        String hostName = prefs.getString("serverName", "");
        String portNumber = prefs.getString("serverPort", "");
        PublicKey serverKey = Crypto.getPublicKeyFromString(prefs.getString("serverKey", ""));
        final String server = "http://" + hostName + ":" + portNumber;
        Boolean loggedIn = prefs.getBoolean("loggedIn", false);

        if(loggedIn) {

            localKeyPairDB = new LocalKeyPairDBHandler(this);
            localKeyPair = localKeyPairDB.getKeyPairByUsername(username);
            getPreferences(Context.MODE_PRIVATE).edit().putString(Crypto.prefPrivateKey,localKeyPair.getPrivateKey()).apply();
            getPreferences(Context.MODE_PRIVATE).edit().putString(Crypto.prefPublicKey,localKeyPair.getPublicKey()).apply();

            myCrypto = new Crypto(getPreferences(Context.MODE_PRIVATE));

            final TextView serverMsgBanner = (TextView) findViewById(R.id.serverMsg);

            Log.d("ZachLOG: ", "logged in");
            // Poll to continuously check that connection with server is alive and clean up msgs
            Subscription serverConn = Observable.interval(0, 1, TimeUnit.SECONDS, Schedulers.newThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(Long numTicks) {
                        try {
                            String response = WebHelper.StringGet(server + "/");
                            Log.d("LOG: ", "Server connection alive...");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (serverMsgBanner != null)
                                        serverMsgBanner.setVisibility(View.GONE);

                                    cleanUpMessages();

                                }
                            });
                        } catch (Exception e) {
                            // Connection could not be made
                            //e.printStackTrace();
                            Log.d("LOG: ", "Server connection dead...");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (serverMsgBanner != null)
                                        serverMsgBanner.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }
                });
            cs.add(serverConn);

            Subscription messageStatus = Observable.interval(0, 2, TimeUnit.SECONDS, Schedulers.newThread())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Long>() {
                        @Override
                        public void onCompleted() {
                        }

                        @Override
                        public void onError(Throwable e) {
                        }

                        @Override
                        public void onNext(Long numTicks) {
                            Observable.just(0)
                                    .subscribeOn(AndroidSchedulers.mainThread())
                                    .observeOn(Schedulers.io())
                                    .flatMap(new NotificationStage(server, username))
                                    .subscribe(new Observer<Notification>() {
                                        @Override
                                        public void onCompleted() {

                                        }

                                        @Override
                                        public void onError(Throwable e) {

                                        }

                                        @Override
                                        public void onNext(final Notification notification) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    handleMessageStatus(notification);
                                                }
                                            });
                                        }
                                    });
                            Log.d("POLL", "Polling Message Status...");
                        }
                    });
            cs.add(messageStatus);

        }

    }

    private List<Message> getMessageListFromDB() {
        MessageDBHandler db = new MessageDBHandler(this);
        return db.getMessages(localKeyPair.getId());
    }

    private void setupRecyclerView() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean loggedIn = prefs.getBoolean("loggedIn", false);

        if(loggedIn) {
            localKeyPairDB = new LocalKeyPairDBHandler(this);
            localKeyPair = localKeyPairDB.getKeyPairByUsername(prefs.getString("username",""));
            MessageDBHandler db = new MessageDBHandler(this);
            this.recList = getMessageListFromDB();

            // Clean up messages
            for (Message message : this.recList) {
                Boolean expired = ((int) (System.currentTimeMillis() / 1000L) > (message.getCreatedAt() + message.getTTL()));
                if (expired) {
                    Log.d("LOGG", "expired!!!");
                    db.deleteMessage(message);
                }
            }

            // Get fresh list
            this.recList = getMessageListFromDB();
        } else{
            this.recList = new ArrayList<>();
        }

        //Recycler view stuff
        recView = (RecyclerView) findViewById(R.id.main_cards_list);
        if(null != recView) {
            recView.setHasFixedSize(true);
            llm = new LinearLayoutManager(this);
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            recView.setLayoutManager(llm);

            this.mAdapter = new MessageAdapter(this.recList);
            recView.setAdapter(this.mAdapter);
            recView.invalidate();
            mAdapter.notifyDataSetChanged();
        }
    }

    private void handleMessageStatus(Notification notification){
        JSONObject message = null;

        // handle updating state here
        Log.d("LOG", "Next " + notification);
        if (notification instanceof Notification.Message) {
            message = ((Notification.Message) notification).message;
            handleMessage(message);
        }
    }

    private String decryptAES64ToString(String aes64, SecretKey aesKey) throws UnsupportedEncodingException {
        byte[] bytes = Base64.decode(aes64,Base64.NO_WRAP);
        if(bytes==null) return null;
        bytes = Crypto.decryptAES(bytes, aesKey);
        if(bytes==null) return null;
        return new String(bytes,"UTF-8");
    }

    private void handleMessage(JSONObject message){
        String LOG = "MESSAGE_HANDLER";
        Log.d(LOG,"Got message "+message);
        final  MessageDBHandler dbMessage = new MessageDBHandler(this);
        try{
            SecretKey aesKey = Crypto.getAESSecretKeyFromBytes(myCrypto.decryptRSA(Base64.decode(message.getString("aes-key"),Base64.NO_WRAP)));
            String sender = decryptAES64ToString(message.getString("sender"),aesKey);
            String recipient = decryptAES64ToString(message.getString("recipient"),aesKey);
            String body = decryptAES64ToString(message.getString("body"),aesKey);
            String subject = decryptAES64ToString(message.getString("subject-line"),aesKey);
            Long born = Long.parseLong(decryptAES64ToString(message.getString("born-on-date"),aesKey));
            Long ttl = Long.parseLong(decryptAES64ToString(message.getString("time-to-live"),aesKey));
            Log.d(LOG,sender+" says:");
            Log.d(LOG,subject+":");
            Log.d(LOG,body);
            Log.d(LOG,"ttl: "+ttl);

            Message newMessage;

            long adjustedTTL = ((born/1000L) + (ttl/1000L)) - (System.currentTimeMillis()/1000L);
            if(adjustedTTL < 0){
                // This message is already expired when received, so born date will be current system time.
                newMessage = new Message(localKeyPair.getId(),sender,subject,body, (int) (ttl/1000L));
            } else{
                // Born date will be what was specified in message.
                newMessage = new Message(localKeyPair.getId(),sender,subject,body, (int) (born/1000L), (int) (ttl/1000L));
            }

            // Grab the message Id, it is returned from db call after insertion. Add to recList.
            int messageId = dbMessage.addMessage(newMessage);
            recList.add(dbMessage.getMessage(messageId));
            mAdapter.notifyItemInserted(recList.size() - 1);
        } catch (Exception e) {
            Log.d(LOG,"Failed to parse message",e);
        }

    }

    public void cleanUpMessages(){
        MessageDBHandler db = new MessageDBHandler(getApplicationContext());
        int i = 0;
        ArrayList<Integer> indexesToRemove = new ArrayList<>();
        // Clean up messages
        for (Message message : recList) {
            Boolean expired = ((int) (System.currentTimeMillis() / 1000L) > (message.getCreatedAt() + message.getTTL()));
            if (expired) {
                db.deleteMessage(message);
                indexesToRemove.add(i);
            }
            i++;
        }

        for(int index:indexesToRemove){
            try {
                recList.remove(index);
                mAdapter.notifyItemRemoved(index);
                mAdapter.notifyItemRangeChanged(index, recList.size());
            } catch(IndexOutOfBoundsException e){
                Log.d("LOG", e.toString());
            }
        }
    }

    public void updateLoginMsg(boolean loggedIn){
        TextView notLoggedInView = (TextView) findViewById(R.id.not_logged_in);

        if(loggedIn && null != notLoggedInView) {
            notLoggedInView.setVisibility(View.GONE);

        } else if(null != notLoggedInView){
            notLoggedInView.setVisibility(View.VISIBLE);
        }
    }
}
