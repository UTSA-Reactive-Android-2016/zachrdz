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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.csandroid.myfirstapp.R;
import com.csandroid.myfirstapp.api.core.ServerAPI;
import com.csandroid.myfirstapp.db.ContactDBHandler;
import com.csandroid.myfirstapp.db.LocalKeyPairDBHandler;
import com.csandroid.myfirstapp.models.Contact;
import com.csandroid.myfirstapp.models.LocalKeyPair;
import com.csandroid.myfirstapp.utils.Crypto;

public class ComposeActivity extends AppCompatActivity {

    ServerAPI serverAPI;
    LocalKeyPairDBHandler localKeyPairDB;
    LocalKeyPair localKeyPair;
    Crypto myCrypto;
    ServerAPI.Listener serverAPIListener;
    private long ttlSelected;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(null != getSupportActionBar()){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        this.initOnClickListeners();
        this.populateFields();
        this.initServerAPI();
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
                        Intent composeIntent = new Intent(ComposeActivity.this, MainActivity.class);

                        doSendMessageToUser();

                        composeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(composeIntent);
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
                    final CharSequence colors[] = new CharSequence[]{"5", "15", "60"};

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
        serverAPI.registerListener(serverAPIListener = new ServerAPI.Listener() {
            @Override
            public void onCommandFailed(String commandName, VolleyError volleyError) {
                Toast.makeText(ComposeActivity.this,String.format("command %s failed!",commandName),
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
            public void onLogoutSucceeded() {}

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
            public void onSendMessageSucceeded(Object key) {
                Toast.makeText(ComposeActivity.this,String.format("sent a message"),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSendMessageFailed(Object key, String reason) {
                Toast.makeText(ComposeActivity.this,String.format("failed to send a message"),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMessageDelivered(String sender, String recipient, String subject, String body, long born_on_date, long time_to_live) {}
        });
    }

    private void unregisterServerAPIListener(){
        serverAPI.unregisterListener(serverAPIListener);
    }

    @Override
    public void onResume(){
        super.onResume();
        registerServerAPIListener();
    }

    @Override
    public void onPause(){
        super.onPause();
        unregisterServerAPIListener();
    }

    public void doSendMessageToUser(){
        String recipient = getUserNameFieldValue();
        ContactDBHandler db = new ContactDBHandler(this);
        Contact contact = db.getContactByUsername(recipient);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String sender = prefs.getString("username","");
        long ttl = (ttlSelected != 0) ? ttlSelected * 1000 : 15000;

        if(contact != null) {
            serverAPI.sendMessage(new Object(), // I don't have an object to keep track of, but I need one!
                    Crypto.getPublicKeyFromString(contact.getPublicKey()),
                    sender,
                    recipient,
                    getSubjectFieldValue(),
                    getBodyFieldValue(),
                    System.currentTimeMillis(),
                    ttl);
            Log.d("TTLZACH", Long.toString(ttl));
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
}
