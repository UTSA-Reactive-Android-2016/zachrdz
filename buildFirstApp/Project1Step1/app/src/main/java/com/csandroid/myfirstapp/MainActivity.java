package com.csandroid.myfirstapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //Recycler view stuff
        RecyclerView recList = (RecyclerView) findViewById(R.id.main_cards_list);
        recList.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recList.setLayoutManager(llm);

        MessageAdapter ma = new MessageAdapter(createList(3));
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

    private List<MessageInfo> createList(int size) {

        List<MessageInfo> result = new ArrayList<MessageInfo>();
        for (int i=1; i <= size; i++) {
            MessageInfo ci = new MessageInfo();
            ci.sender = MessageInfo.SENDER_PREFIX + i;
            ci.subject = MessageInfo.SUBJECT_PREFIX + i;
            ci.ttl = MessageInfo.TTL_PREFIX + i;

            result.add(ci);
        }

        return result;
    }

}
