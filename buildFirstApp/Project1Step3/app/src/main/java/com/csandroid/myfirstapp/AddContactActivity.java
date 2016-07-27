package com.csandroid.myfirstapp;

import android.content.Intent;
import android.os.Bundle;
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
import com.csandroid.myfirstapp.models.Contact;
import com.csandroid.myfirstapp.utils.EncryptHelper;
import com.squareup.picasso.Picasso;

import java.security.KeyPair;

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
        final TextView usernameField = (TextView) findViewById(R.id.username);
        final TextView publicKeyField = (TextView) findViewById(R.id.publicKey);
        final ImageView userImageField = (ImageView) findViewById(R.id.userImage);

        Button saveBtn = (Button) findViewById(R.id.button2);
        ImageButton deleteBtn = (ImageButton) findViewById(R.id.imageButton2);
        ImageButton searchBtn = (ImageButton) findViewById(R.id.search_button);

        if(null != saveBtn && null != usernameField && null != publicKeyField) {
            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Check username length if valid
                    if (usernameField.getText().toString().length() > 0 &&
                            publicKeyField.getText().toString().length() > 0){
                        Intent contactsIntent = new Intent(AddContactActivity.this, ContactsActivity.class);

                        ContactDBHandler db = new ContactDBHandler(AddContactActivity.this);
                        Contact contact = new Contact();
                        contact.setUsername(usernameField.getText().toString());
                        contact.setUserImage("http://i.imgur.com/a09vxsJ.png");
                        contact.setPublicKey(publicKeyField.getText().toString());
                        db.addContact(contact);
                        Toast.makeText(v.getContext(), "Added Contact: " + contact.getUsername(),
                                Toast.LENGTH_LONG).show();
                        contactsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(contactsIntent);
                    } else if(usernameField.getText().toString().length() < 1){
                        String errMsg = "Username length must be greater than 0 characters!";
                        Toast.makeText(v.getContext(), "Error: " + errMsg,
                                Toast.LENGTH_LONG).show();
                    } else{
                        String errMsg = "User was not found. (Click search button up top to fake find them)";
                        Toast.makeText(v.getContext(), "Error: " + errMsg,
                                Toast.LENGTH_LONG).show();
                    }
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
        if(null != searchBtn){
            searchBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != usernameField && usernameField.getText().toString().length() > 0) {
                        // Generate KeyPair to set for fake contact
                        EncryptHelper encryptHelper = new EncryptHelper();
                        KeyPair kp = encryptHelper.generateKeyPair();
                        String publicKeyString = encryptHelper.getPublicKeyString(kp);

                        if (null != publicKeyField) {
                            publicKeyField.setText(publicKeyString, TextView.BufferType.SPANNABLE);
                        }

                        if(null != userImageField){
                            Picasso.with(getApplicationContext()).load("http://i.imgur.com/a09vxsJ.png").into(userImageField);
                        }

                        String msg = "User found!";
                        Toast.makeText(v.getContext(), "" + msg,
                                Toast.LENGTH_LONG).show();
                    } else if(null != usernameField && usernameField.getText().toString().length() == 0){
                        String errMsg = "You didn't type anything to search!";
                        Toast.makeText(v.getContext(), "Error: " + errMsg,
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

}

