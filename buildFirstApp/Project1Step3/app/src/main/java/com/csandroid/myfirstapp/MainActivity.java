package com.csandroid.myfirstapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.csandroid.myfirstapp.adapters.MessageAdapter;
import com.csandroid.myfirstapp.api.core.ServerAPI;
import com.csandroid.myfirstapp.db.ContactDBHandler;
import com.csandroid.myfirstapp.db.LocalKeyPairDBHandler;
import com.csandroid.myfirstapp.db.MessageDBHandler;
import com.csandroid.myfirstapp.models.Contact;
import com.csandroid.myfirstapp.models.LocalKeyPair;
import com.csandroid.myfirstapp.models.Message;
import com.csandroid.myfirstapp.utils.Crypto;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    LocalKeyPairDBHandler localKeyPairDB;
    LocalKeyPair localKeyPair;
    List<Contact> contactsList;
    ServerAPI.Listener serverAPIListener;

    ServerAPI serverAPI;
    Crypto myCrypto;
    private final static int mInterval = 1000 * 3; // 3 seconds
    private Handler mHandler;

    private RecyclerView recView;
    private List<Message> recList;
    private MessageAdapter mAdapter;

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

        if(loggedIn) {
            this.initServerAPI();
            this.initMessageStatusPoll();
        }
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean loggedIn = prefs.getBoolean("loggedIn", false);

        if(loggedIn) {
            localKeyPairDB = new LocalKeyPairDBHandler(this);
            localKeyPair = localKeyPairDB.getKeyPairByUsername(prefs.getString("username",""));
            this.recList = getMessageListFromDB();

            // When the view is brought back into focus, reload messages
            // list to make sure user doesn't see stale data.
            this.mAdapter = new MessageAdapter(recList);
            recView.setAdapter(this.mAdapter);
            recView.invalidate();

            initServerAPI();
            initMessageStatusPoll();
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean loggedIn = prefs.getBoolean("loggedIn", false);

        if(loggedIn) {
            unregisterServerAPIListener();
            stopRepeatingTask();
        }
    }

    public void initServerAPI(){
        // Logged in User setup for serverAPI
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString("username","");
        String hostName = prefs.getString("serverName", "");
        String portNumber = prefs.getString("serverPort", "");

        localKeyPairDB = new LocalKeyPairDBHandler(this);
        localKeyPair = localKeyPairDB.getKeyPairByUsername(username);
        getPreferences(Context.MODE_PRIVATE).edit().putString(Crypto.prefPrivateKey,localKeyPair.getPrivateKey()).apply();
        getPreferences(Context.MODE_PRIVATE).edit().putString(Crypto.prefPublicKey,localKeyPair.getPublicKey()).apply();

        myCrypto = new Crypto(getPreferences(Context.MODE_PRIVATE));
        serverAPI = ServerAPI.getInstance(this.getApplicationContext(), myCrypto);
        serverAPI.setServerName(hostName);
        serverAPI.setServerPort(portNumber);

        this.registerServerAPIListener();
    }

    private void registerServerAPIListener(){
        final  MessageDBHandler dbMessage = new MessageDBHandler(this);
        final ContactDBHandler dbContact = new ContactDBHandler(this);

        serverAPI.registerListener(serverAPIListener = new ServerAPI.Listener() {
            @Override
            public void onCommandFailed(String commandName, VolleyError volleyError) {
                Toast.makeText(MainActivity.this,String.format("command %s failed!",commandName),
                        Toast.LENGTH_SHORT).show();
                volleyError.printStackTrace();
            }

            @Override
            public void onGoodAPIVersion() {}

            @Override
            public void onBadAPIVersion() {}

            @Override
            public void onRegistrationSucceeded() {}

            @Override
            public void onRegistrationFailed(String reason) {}

            @Override
            public void onLoginSucceeded() {}

            @Override
            public void onLoginFailed(String reason) {}

            @Override
            public void onLogoutSucceeded() { }

            @Override
            public void onLogoutFailed(String reason) {}

            @Override
            public void onUserInfo(ServerAPI.UserInfo info) {}

            @Override
            public void onUserNotFound(String username) {}

            @Override
            public void onContactLogin(String username) {}

            @Override
            public void onContactLogout(String username) {}

            @Override
            public void onSendMessageSucceeded(Object key) {}

            @Override
            public void onSendMessageFailed(Object key, String reason) {}

            @Override
            public void onMessageDelivered(String sender, String recipient, String subject, String body, long born_on_date, long time_to_live) {
                Message newMessage = new Message(localKeyPair.getId(),sender,subject,body, (int) (born_on_date/1000L), (int) (time_to_live/1000L));
                int messageId = dbMessage.addMessage(newMessage);
                recList.add(dbMessage.getMessage(messageId));
                mAdapter.notifyItemInserted(recList.size() - 1);
            }
        });
    }

    private void unregisterServerAPIListener(){
        serverAPI.unregisterListener(serverAPIListener);
    }

    public void initMessageStatusPoll(){
        mHandler = new Handler();
        startRepeatingTask();
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                doStartPushListener();
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    public void doStartPushListener() {
        // Logged in User setup for serverAPI
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString("username","");
        serverAPI.startPushListener(username);
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
                if ((message.getCreatedAt() + message.getTTL()) > (int) (System.currentTimeMillis() / 1000L)) {
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
            LinearLayoutManager llm = new LinearLayoutManager(this);
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            recView.setLayoutManager(llm);

            this.mAdapter = new MessageAdapter(this.recList);
            recView.setAdapter(this.mAdapter);
        }
    }
}
