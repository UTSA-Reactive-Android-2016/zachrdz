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
import android.view.View;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.csandroid.myfirstapp.adapters.ContactAdapter;
import com.csandroid.myfirstapp.api.core.ServerAPI;
import com.csandroid.myfirstapp.db.ContactDBHandler;
import com.csandroid.myfirstapp.db.LocalKeyPairDBHandler;
import com.csandroid.myfirstapp.models.Contact;
import com.csandroid.myfirstapp.models.LocalKeyPair;
import com.csandroid.myfirstapp.utils.Crypto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ContactsActivity extends AppCompatActivity {

    private RecyclerView recList;
    LocalKeyPairDBHandler localKeyPairDB;
    LocalKeyPair localKeyPair;
    List<Contact> contactsList;
    ServerAPI.Listener serverAPIListener;

    ServerAPI serverAPI;
    Crypto myCrypto;
    HashMap<String,ServerAPI.UserInfo> myUserMap = new HashMap<>();
    private final static int mInterval = 1000 * 3; // 3 seconds
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        Toolbar toolbar = (Toolbar) findViewById(R.id.contacts_toolbar);
        setSupportActionBar(toolbar);
        if(null != getSupportActionBar()){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        this.initServerAPI();
        this.setupRecyclerView();

        this.initContactStatusPoll();
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.contacts_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_add_contact:
                Intent addContactIntent = new Intent(ContactsActivity.this, AddContactActivity.class);
                addContactIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(addContactIntent);
                return true;
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        // When the view is brought back into focus, reload contacts
        // list to make sure user doesn't see stale data.
        ContactAdapter ca = new ContactAdapter(createList());
        recList.setAdapter(ca);
        recList.invalidate();

        doRegisterContacts();
        registerServerAPIListener();
        startRepeatingTask();
    }

    @Override
    public void onPause(){
        super.onPause();
        unregisterServerAPIListener();
        stopRepeatingTask();
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
                Toast.makeText(ContactsActivity.this,String.format("command %s failed!",commandName),
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
            public void onContactLogin(String username) {
                int i = 0;
                for (Contact contact:contactsList) {
                    if(contact.getUsername().equals(username)){
                        View v = recList.getLayoutManager().findViewByPosition(i);

                        // Toggle online status here
                        if(null != v) {
                            v.findViewById(R.id.online).setVisibility(View.VISIBLE);
                            v.findViewById(R.id.offline).setVisibility(View.GONE);
                        }
                        break;
                    }
                    i++;
                }
            }

            @Override
            public void onContactLogout(String username) {
                int i = 0;
                for (Contact contact:contactsList) {
                    if(contact.getUsername().equals(username)){
                        View v = recList.getLayoutManager().findViewByPosition(i);

                        if(null != v) {
                            // Toggle online status here
                            v.findViewById(R.id.online).setVisibility(View.GONE);
                            v.findViewById(R.id.offline).setVisibility(View.VISIBLE);
                        }
                        break;
                    }
                    i++;
                }
            }

            @Override
            public void onSendMessageSucceeded(Object key) {}

            @Override
            public void onSendMessageFailed(Object key, String reason) {}

            @Override
            public void onMessageDelivered(String sender, String recipient, String subject, String body, long born_on_date, long time_to_live) {}
        });
    }

    private void unregisterServerAPIListener(){
        serverAPI.unregisterListener(serverAPIListener);
    }

    public void initContactStatusPoll(){
        mHandler = new Handler();
        startRepeatingTask();
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                doRegisterContacts();
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

    public void doRegisterContacts(){
        ArrayList<String> contacts = new ArrayList<>();
        for(Contact contact:contactsList){
            contacts.add(contact.getUsername());
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString("username","");

        serverAPI.registerContacts(username,contacts);
    }

    private List<Contact> createList() {
        ContactDBHandler db = new ContactDBHandler(this);
        contactsList = db.getContacts(localKeyPair.getId());
        return contactsList;
    }

    private void setupRecyclerView(){
        recList = (RecyclerView) findViewById(R.id.contacts_cards_list);
        if(null != recList){
            recList.setHasFixedSize(true);
            LinearLayoutManager llm = new LinearLayoutManager(this);
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            recList.setLayoutManager(llm);

            ContactAdapter ca = new ContactAdapter(createList());
            recList.setAdapter(ca);
        }
    }
}
