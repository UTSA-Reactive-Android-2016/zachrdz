package com.csandroid.myfirstapp.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.csandroid.myfirstapp.R;
import com.csandroid.myfirstapp.db.ContactDBHandler;
import com.csandroid.myfirstapp.db.LocalKeyPairDBHandler;
import com.csandroid.myfirstapp.models.Contact;
import com.csandroid.myfirstapp.models.LocalKeyPair;
import com.csandroid.myfirstapp.stages.SendMessageStage;
import com.csandroid.myfirstapp.utils.Crypto;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class ComposeActivity extends AppCompatActivity {

    LocalKeyPairDBHandler localKeyPairDB;
    LocalKeyPair localKeyPair;
    Crypto myCrypto;

    private long ttlSelected;
    private CompositeSubscription cs = new CompositeSubscription();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Compose");

        if(null != getSupportActionBar()){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        this.initOnClickListeners();
        this.populateFields();
        this.initServer();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void initOnClickListeners(){
        final EditText toInput = (EditText) findViewById(R.id.msg_receiver);
        Button sendBtn = (Button) findViewById(R.id.button);
        ImageButton deleteBtn = (ImageButton) findViewById(R.id.imageButton4);
        ImageButton ttlBtn = (ImageButton) findViewById(R.id.imageButton5);
        final EditText composedMsg = (EditText) findViewById(R.id.msg_body);

        if(null != toInput) {
            toInput.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent composeIntent = new Intent(ComposeActivity.this, ContactsActivity.class);
                    composeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(composeIntent);
                }
            });
        }
        if(null != sendBtn && null != composedMsg && null != toInput) {
            sendBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    ContactDBHandler cdb = new ContactDBHandler(v.getContext());
                    Contact contact = cdb.getContactByUsername(toInput.getText().toString());
                    String message = composedMsg.getText().toString();

                    // A receiver is specified and message exists!
                    // Encrypt this message with their public key.
                    if(message.length() > 0 && null != contact &&
                            null != contact.getPublicKey() && contact.getPublicKey().length() > 0){
                        doSendMessageToUser();
                    } else if(message.length() > 0){
                        // Default message if not encrypted (i.e. non-existing contact specified)
                        String toastTitle = "Error: ";
                        String errMsg = "No valid recipient specified!";
                        Toast.makeText(v.getContext(), toastTitle + errMsg,
                                Toast.LENGTH_LONG).show();
                    } else{
                        // Default message if text not supplied
                        String toastTitle = "Error: ";
                        String errMsg = "You didn't type a message to send!";
                        Toast.makeText(v.getContext(), toastTitle + errMsg,
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        if(null != deleteBtn) {
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent composeIntent = new Intent(ComposeActivity.this, MainActivity.class);
                    composeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(composeIntent);
                }
            });
        }
        if(null != ttlBtn) {
            ttlBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final CharSequence colors[] = new CharSequence[]{"5", "15", "30", "60"};

                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    builder.setTitle("Pick a TTL for this message (seconds):");
                    builder.setItems(colors, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // the user clicked on colors[which]
                            ttlSelected = Integer.parseInt(colors[which].toString());
                            dialog.cancel();
                        }
                    });
                    builder.show();
                }
            });
        }
    }

    private void populateFields(){
        ttlSelected = 0;
        EditText toInput = (EditText) findViewById(R.id.msg_receiver);

        //Get the bundle
        Bundle bundle = getIntent().getExtras();
        if(null != bundle && null != toInput){
            String replyToUsername = bundle.getString("reply_to_username");
            String replyToId = bundle.getString("contact_id");

            ContactDBHandler db = new ContactDBHandler(this);
            if(null != replyToUsername){
                Contact contact = db.getContactByUsername(replyToUsername);
                toInput.setText(contact.getUsername());
            }
            if(null != replyToId){
                Contact contact = db.getContact(Integer.parseInt(replyToId));
                toInput.setText(contact.getUsername());
            }
        }
    }

    public void initServer(){
        // Logged in User setup for serverAPI
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString("username","");

        localKeyPairDB = new LocalKeyPairDBHandler(this);
        localKeyPair = localKeyPairDB.getKeyPairByUsername(username);
        getPreferences(Context.MODE_PRIVATE).edit().putString(Crypto.prefPrivateKey,localKeyPair.getPrivateKey()).apply();
        getPreferences(Context.MODE_PRIVATE).edit().putString(Crypto.prefPublicKey,localKeyPair.getPublicKey()).apply();

        myCrypto = new Crypto(getPreferences(Context.MODE_PRIVATE));
    }

    public void doSendMessageToUser(){
        String recipient = getUserNameFieldValue();
        ContactDBHandler db = new ContactDBHandler(this);
        Contact contact = db.getContactByUsername(recipient);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String sender = prefs.getString("username","");
        String hostName = prefs.getString("serverName", "");
        String portNumber = prefs.getString("serverPort", "");
        PublicKey serverKey = Crypto.getPublicKeyFromString(prefs.getString("serverKey", ""));
        final String server = "http://" + hostName + ":" + portNumber;

        long ttl = (ttlSelected != 0) ? ttlSelected * 1000 : 15000;

        if(contact != null) {

            SecretKey aesKey = Crypto.createAESKey();
            byte[] aesKeyBytes = aesKey.getEncoded();
            if(aesKeyBytes==null){
                Log.d("LOG","AES key failed (this should never happen)");
                return;
            }
            String base64encryptedAESKey =
                    Base64.encodeToString(Crypto.encryptRSA(
                            aesKeyBytes,
                            Crypto.getPublicKeyFromString(contact.getPublicKey()
                            )
                    ), Base64.NO_WRAP);

            // Attempt to send message
            Subscription sendMessageSub = Observable.just("ok")
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(Schedulers.io())
                    .flatMap(new SendMessageStage(server, recipient,
                            keyValuePairs(
                                "aes-key", base64encryptedAESKey,
                                "sender",  base64AESEncrypted(sender, aesKey),
                                "recipient",  base64AESEncrypted(recipient, aesKey),
                                "subject-line",  base64AESEncrypted(getSubjectFieldValue(), aesKey),
                                "body",  base64AESEncrypted(getBodyFieldValue(), aesKey),
                                "born-on-date",  base64AESEncrypted(Long.valueOf(System.currentTimeMillis()).toString(), aesKey),
                                "time-to-live",  base64AESEncrypted(Long.valueOf(ttl).toString(), aesKey)
                            )
                    ))
                    .subscribe(new Observer<String>() {
                        @Override
                        public void onCompleted() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {}
                            });
                        }

                        @Override
                        public void onError(Throwable e) {
                            final Throwable fe = e;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d("ZachLog","Error: Send Message",fe);
                                    Toast.makeText(ComposeActivity.this,String.format("Network Error: failed to send"),Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onNext(final String response) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(response.equals("ok")){
                                        Toast.makeText(ComposeActivity.this,String.format("sent a message"),Toast.LENGTH_SHORT).show();

                                        // Message was sent, go back to main screen
                                        Intent composeIntent = new Intent(ComposeActivity.this, MainActivity.class);
                                        composeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(composeIntent);
                                    } else{
                                        Toast.makeText(ComposeActivity.this,String.format("Failed to send message"),Toast.LENGTH_SHORT).show();
                                        Toast.makeText(ComposeActivity.this,String.format("User is offline"),Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    });

            cs.add(sendMessageSub);
        } else {
            Log.d("Main",recipient + " info not available");
        }
    }

    private String getUserNameFieldValue(){
        EditText usernameField = ((EditText)findViewById(R.id.msg_receiver));
        if(usernameField != null) {
            return usernameField.getText().toString();
        } else{
            return "";
        }
    }

    private String getSubjectFieldValue(){
        EditText subjectField = ((EditText)findViewById(R.id.msg_subject));
        if(subjectField != null) {
            return subjectField.getText().toString();
        } else{
            return "";
        }
    }

    private String getBodyFieldValue(){
        EditText bodyField = ((EditText)findViewById(R.id.msg_body));
        if(bodyField != null) {
            return bodyField.getText().toString();
        } else{
            return "";
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        cs = new CompositeSubscription();
    }

    @Override
    public void onPause(){
        super.onPause();
        cs.unsubscribe();
        cs = null;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(null != cs){
            cs.unsubscribe();
        }
        cs = null;
    }

    private JSONObject keyValuePairs(String... args){
        JSONObject json = new JSONObject();
        try {
            for(int i=0; i+1<args.length;i+=2){
                json.put(args[i],args[i+1]);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    private String base64AESEncrypted(String clearText, SecretKey aesKey){
        try {
            return Base64.encodeToString(Crypto.encryptAES(clearText.getBytes("UTF-8"),aesKey), Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

}
