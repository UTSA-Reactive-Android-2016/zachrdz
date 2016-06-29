package com.csandroid.myfirstapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.csandroid.myfirstapp.db.ContactDBHandler;
import com.csandroid.myfirstapp.db.MessageDBHandler;
import com.csandroid.myfirstapp.models.Contact;
import com.csandroid.myfirstapp.models.Message;

public class EditContactActivity extends AppCompatActivity {

    private int contactId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_contact);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ImageButton deleteBtn = (ImageButton) findViewById(R.id.imageButton2);


        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent contactsIntent = new Intent(EditContactActivity.this, ContactsActivity.class);
                ContactDBHandler db = new ContactDBHandler(v.getContext());
                Contact contact = db.getContact(EditContactActivity.this.contactId);
                db.deleteContact(contact);
                Toast.makeText(v.getContext(), "Deleted contact: " + contact.getUsername(),
                        Toast.LENGTH_LONG).show();
                startActivity(contactsIntent);
            }
        });

        //Get the bundle
        Bundle bundle = getIntent().getExtras();

        //Extract the data…
        String contactId = bundle.getString("contact_id");
        this.contactId = Integer.parseInt(contactId);

        this.populateFields();

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

    public void populateFields(){
        TextView username = (TextView)findViewById(R.id.username);
        ImageView userImage = (ImageView)findViewById(R.id.userImage);
        TextView publicKey = (TextView)findViewById(R.id.publicKey);

        ContactDBHandler db = new ContactDBHandler(this);

        Contact contact = db.getContact(this.contactId);

        username.setText(contact.getUsername());
        publicKey.setText(contact.getPublicKey());
    }
}
