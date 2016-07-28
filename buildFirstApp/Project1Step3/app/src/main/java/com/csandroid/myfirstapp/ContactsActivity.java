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

import com.csandroid.myfirstapp.adapters.ContactAdapter;
import com.csandroid.myfirstapp.db.ContactDBHandler;
import com.csandroid.myfirstapp.db.LocalKeyPairDBHandler;
import com.csandroid.myfirstapp.models.Contact;
import com.csandroid.myfirstapp.models.LocalKeyPair;

import java.util.List;

public class ContactsActivity extends AppCompatActivity {

    private RecyclerView recList;
    LocalKeyPairDBHandler localKeyPairDB;
    LocalKeyPair localKeyPair;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        Toolbar toolbar = (Toolbar) findViewById(R.id.contacts_toolbar);
        setSupportActionBar(toolbar);
        if(null != getSupportActionBar()){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Logged in User
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString("username","");

        localKeyPairDB = new LocalKeyPairDBHandler(this);
        localKeyPair = localKeyPairDB.getKeyPairByUsername(username);

        this.setupRecyclerView();
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
    }

    private List<Contact> createList() {
        ContactDBHandler db = new ContactDBHandler(this);
        return db.getContacts(localKeyPair.getId());
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
