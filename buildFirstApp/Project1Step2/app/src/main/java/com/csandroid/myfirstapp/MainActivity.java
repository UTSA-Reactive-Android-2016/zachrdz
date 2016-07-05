package com.csandroid.myfirstapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.csandroid.myfirstapp.db.ContactDBHandler;
import com.csandroid.myfirstapp.db.LocalKeyPairDBHandler;
import com.csandroid.myfirstapp.db.MessageDBHandler;
import com.csandroid.myfirstapp.models.Contact;
import com.csandroid.myfirstapp.models.LocalKeyPair;
import com.csandroid.myfirstapp.models.Message;
import com.csandroid.myfirstapp.utils.EncryptHelper;

import java.security.KeyPair;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recList;

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
        this.initAppCreationTasks();

        // Setup recycler view/list
        this.setupRecyclerView();
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

        switch(id){
            case R.id.action_settings :
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(settingsIntent);
                return true;
            case R.id.action_contacts :
                Intent contactsIntent = new Intent(MainActivity.this, ContactsActivity.class);
                contactsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(contactsIntent);
                return true;
            case R.id.action_compose :
                Intent composeIntent = new Intent(MainActivity.this, ComposeActivity.class);
                composeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(composeIntent);
                return true;
            default :
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        // When the view is brought back into focus, reload messages
        // list to make sure user doesn't see stale data.
        MessageAdapter ma = new MessageAdapter(createList());
        recList.setAdapter(ma);
        recList.invalidate();
    }

    private List<Message> createList() {
        MessageDBHandler db = new MessageDBHandler(this);
        return db.getAllMessages();
    }

    private void setupRecyclerView() {
        //Recycler view stuff
        recList = (RecyclerView) findViewById(R.id.main_cards_list);
        if(null != recList) {
            recList.setHasFixedSize(true);
            LinearLayoutManager llm = new LinearLayoutManager(this);
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            recList.setLayoutManager(llm);

            MessageAdapter ma = new MessageAdapter(createList());
            recList.setAdapter(ma);
        }
    }

    private void initAppCreationTasks(){
        // Tasks to be run when the app is first installed
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("firstTime", false)) {
            // Create Fake messages
            this.createFakeMessages();

            // Create Fake contacts
            this.createFakeContacts();

            // Create KeyPair
            this.setupInitialKeyPair();

            // mark first time has ran.
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

    private void createFakeMessages(){
        MessageDBHandler db = new MessageDBHandler(this);
        db.addMessage(new Message("johndoe", "Really Important, read immediately!",
                "This is a super important, secret message!", 5));
        db.addMessage(new Message("mikejones", "Hows it going?",
                "Just wanted to see what you were up to...", 15));
        db.addMessage(new Message("stacyp", "Let me know if you get this.",
                "This is my message with stuff...", 300));
    }

    private void createFakeContacts(){
        ContactDBHandler db = new ContactDBHandler(this);
        EncryptHelper encryptHelper = new EncryptHelper();

        // Generate 3 keypairs, only utilizing public key since these contacts are fake
        KeyPair kp1 = encryptHelper.generateKeyPair();
        KeyPair kp2 = encryptHelper.generateKeyPair();
        KeyPair kp3 = encryptHelper.generateKeyPair();

        db.addContact(new Contact("johndoe", null, encryptHelper.getPublicKeyString(kp1)));
        db.addContact(new Contact("mikejones", null, encryptHelper.getPublicKeyString(kp2)));
        db.addContact(new Contact("stacyp", null, encryptHelper.getPublicKeyString(kp3)));
    }

}
