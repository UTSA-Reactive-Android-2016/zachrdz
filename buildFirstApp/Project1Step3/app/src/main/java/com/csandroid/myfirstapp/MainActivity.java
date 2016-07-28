package com.csandroid.myfirstapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.csandroid.myfirstapp.adapters.MessageAdapter;
import com.csandroid.myfirstapp.db.ContactDBHandler;
import com.csandroid.myfirstapp.db.LocalKeyPairDBHandler;
import com.csandroid.myfirstapp.db.MessageDBHandler;
import com.csandroid.myfirstapp.models.Contact;
import com.csandroid.myfirstapp.models.LocalKeyPair;
import com.csandroid.myfirstapp.models.Message;
import com.csandroid.myfirstapp.utils.EncryptHelper;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

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

        // Run initial tasks on app creation (first time)
        //this.initAppCreationTasks();

        // Setup recycler view/list
        this.setupRecyclerView();

        // Setup on click listeners
        //this.initOnClickListeners();
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
        this.recList = getMessageListFromDB();

        // When the view is brought back into focus, reload messages
        // list to make sure user doesn't see stale data.
        this.mAdapter = new MessageAdapter(recList);
        recView.setAdapter(this.mAdapter);
        recView.invalidate();
    }

    private List<Message> getMessageListFromDB() {
        MessageDBHandler db = new MessageDBHandler(this);
        return db.getAllMessages();
    }

    private void setupRecyclerView() {
        this.recList = getMessageListFromDB();

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

    private void initAppCreationTasks(){
        // Tasks to be run when the app is first installed
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("firstTime", false)) {
            // Create Fake contacts
            this.createFakeContacts();

            // Create Fake messages
            this.createFakeMessages();

            // Create KeyPair
            this.setupInitialKeyPair();

            // Mark first time has ran.
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("firstTime", true);
            editor.commit();
        }
    }

    private void setupInitialKeyPair(){
        // When this app is first installed, generate a key pair for the user
        // and store it in the local database.

        // Generate New Key Pair and get values
        EncryptHelper encryptHelper = new EncryptHelper();
        KeyPair keyPair = encryptHelper.generateKeyPair();
        String privateKeyString = encryptHelper.getPrivateKeyString(keyPair);
        String publicKeyString = encryptHelper.getPublicKeyString(keyPair);

        // Create instance of LKP to set values
        LocalKeyPair lkp = new LocalKeyPair();
        lkp.setPrivateKey(privateKeyString);
        lkp.setPublicKey(publicKeyString);

        // Save key pair to local database
        LocalKeyPairDBHandler lkpdb = new LocalKeyPairDBHandler(this);
        lkpdb.addKeyPair(lkp);
    }

    private List<Message> createFakeMessages(){
        MessageDBHandler dbMessage = new MessageDBHandler(this);
        ContactDBHandler dbContact = new ContactDBHandler(this);
        List<Message> messageList = new ArrayList<>();

        Message m1 = new Message("johndoe", "Really Important, read immediately!",
                "This is a super important, secret message!", 5);
        Message m2 = new Message("mikejones", "Hows it going?",
                "Just wanted to see what you were up to...", 15);
        Message m3 = new Message("stacyp", "Let me know if you get this.",
                "This is my message with stuff...", 300);

        // Generate 3 fake messages, make sure contact exists before generation.
        if(dbContact.getContactByUsername("johndoe").getUsername() != null) {
            // Add to db
            int m1Id = dbMessage.addMessage(m1);
            // Add to returned list
            messageList.add(dbMessage.getMessage(m1Id));
        }
        if(dbContact.getContactByUsername("mikejones").getUsername() != null) {
            // Add to db
            int m2Id = dbMessage.addMessage(m2);
            // Add to returned list
            messageList.add(dbMessage.getMessage(m2Id));
        }
        if(dbContact.getContactByUsername("stacyp").getUsername() != null) {
            // Add to db
            int m3Id = dbMessage.addMessage(m3);
            // Add to returned list
            messageList.add(dbMessage.getMessage(m3Id));
        }

        // List of messages added to db
        return messageList;
    }

    private List<Contact> createFakeContacts(){
        ContactDBHandler db = new ContactDBHandler(this);
        EncryptHelper encryptHelper = new EncryptHelper();

        // Generate 3 keypairs, only utilizing public key since these contacts are fake
        KeyPair kp1 = encryptHelper.generateKeyPair();
        KeyPair kp2 = encryptHelper.generateKeyPair();
        KeyPair kp3 = encryptHelper.generateKeyPair();

        Contact c1 = new Contact("johndoe", "http://i.imgur.com/0kKrYvV.jpg", encryptHelper.getPublicKeyString(kp1));
        Contact c2 = new Contact("mikejones", "http://i.imgur.com/lO1cnUP.jpg?1", encryptHelper.getPublicKeyString(kp2));
        Contact c3 = new Contact("stacyp", "https://i.imgur.com/Cs9IoHk.jpg", encryptHelper.getPublicKeyString(kp3));
        List<Contact> contactList = new ArrayList<>();

        // Add contacts to db
        int c1Id = db.addContact(c1);
        int c2Id = db.addContact(c2);
        int c3Id = db.addContact(c3);

        // Build contact list to return to caller
        contactList.add(db.getContact(c1Id));
        contactList.add(db.getContact(c2Id));
        contactList.add(db.getContact(c3Id));

        return contactList;
    }

    private void initOnClickListeners(){
        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.generateFakeMessages);
        if(null != fab) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(getMessageListFromDB().size() < 6) {
                        // Generate fake messages, add them to list
                        List<Message> newMessages = createFakeMessages();
                        recList.addAll(newMessages);
                        for (int i = 0; i < newMessages.size(); i++) {
                            mAdapter.notifyItemInserted(recList.size() - i++);
                        }
                        Toast.makeText(v.getContext(), "Fake messages generated!",
                                Toast.LENGTH_LONG).show();
                    } else{
                        Toast.makeText(v.getContext(), "Fake message limit reached!",
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }
}
