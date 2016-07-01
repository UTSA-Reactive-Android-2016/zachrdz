package com.csandroid.myfirstapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.csandroid.myfirstapp.db.ContactDBHandler;
import com.csandroid.myfirstapp.models.Contact;

public class AddContactActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(null != getSupportActionBar()){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        this.initOnClickListeners();
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
        final EditText username = (EditText)findViewById(R.id.username);
        final EditText publicKey = (EditText)findViewById(R.id.publicKey);

        Button saveBtn = (Button) findViewById(R.id.button2);
        ImageButton deleteBtn = (ImageButton) findViewById(R.id.imageButton2);

        if(null != saveBtn && null != username && null != publicKey) {
            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent contactsIntent = new Intent(AddContactActivity.this, ContactsActivity.class);
                    ContactDBHandler db = new ContactDBHandler(AddContactActivity.this);
                    Contact contact = new Contact();
                    contact.setUsername(username.getText().toString());
                    contact.setPublicKey(publicKey.getText().toString());
                    db.addContact(contact);
                    Toast.makeText(v.getContext(), "Added Contact: " + contact.getUsername(),
                            Toast.LENGTH_LONG).show();
                    contactsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(contactsIntent);
                }
            });
        }
        if(null != deleteBtn) {
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent contactsIntent = new Intent(AddContactActivity.this, ContactsActivity.class);
                    contactsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(contactsIntent);
                }
            });
        }
    }

}

