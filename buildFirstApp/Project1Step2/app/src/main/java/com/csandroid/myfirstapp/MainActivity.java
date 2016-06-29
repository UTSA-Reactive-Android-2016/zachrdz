package com.csandroid.myfirstapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.csandroid.myfirstapp.db.MessageDBHandler;
import com.csandroid.myfirstapp.models.Message;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MessageDBHandler db;
    private RecyclerView recList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //Recycler view stuff
        recList = (RecyclerView) findViewById(R.id.main_cards_list);
        recList.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recList.setLayoutManager(llm);

        boolean dbExists = this.doesDatabaseExist(this.getApplicationContext(), "reactiveAppMessages");

        this.db = new MessageDBHandler(this);

        // Create 3 fake messages when the db is initially created
        if(!dbExists) {
            this.createFakeMessages();
        }

        MessageAdapter ma = new MessageAdapter(createList());
        recList.setAdapter(ma);
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
                startActivity(settingsIntent);
                return true;
            case R.id.action_contacts :
                Intent contactsIntent = new Intent(MainActivity.this, ContactsActivity.class);
                startActivity(contactsIntent);
                return true;
            case R.id.action_compose :
                Intent composeIntent = new Intent(MainActivity.this, ComposeActivity.class);
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
        List<Message> messages = this.db.getAllMessages();

        return messages;
    }

    private static boolean doesDatabaseExist(Context context, String dbName) {
        File dbFile = context.getDatabasePath(dbName);
        return dbFile.exists();
    }

    private void createFakeMessages(){
        this.db.addMessage(new Message("johndoe", "Really Important, read immediately!",
                "This is a super important, secret message!", 5));
        this.db.addMessage(new Message("mikejones", "Hows it going?",
                "Just wanted to see what you were up to...", 15));
        this.db.addMessage(new Message("stacyp", "Let me know if you get this.",
                "This is my message with stuff...", 300));
    }

}
