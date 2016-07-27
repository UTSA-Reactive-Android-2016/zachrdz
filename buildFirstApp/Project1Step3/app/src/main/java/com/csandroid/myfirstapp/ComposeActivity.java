package com.csandroid.myfirstapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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

import com.csandroid.myfirstapp.db.ContactDBHandler;
import com.csandroid.myfirstapp.models.Contact;
import com.csandroid.myfirstapp.utils.EncryptHelper;

import java.security.PublicKey;

public class ComposeActivity extends AppCompatActivity {

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
                        String publicKeyString = contact.getPublicKey();
                        Log.d("Public Key: ", publicKeyString);

                        EncryptHelper encryptHelper = new EncryptHelper();
                        PublicKey publicKey = encryptHelper.getPublicKeyFromString(publicKeyString);
                        String encryptedMessage = encryptHelper.encryptTextWithPublic(message, publicKey);

                        Toast.makeText(v.getContext(), "Encrypted Msg: " + encryptedMessage,
                                Toast.LENGTH_LONG).show();

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
                    CharSequence colors[] = new CharSequence[]{"5s", "15s", "60s"};

                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    builder.setTitle("Pick a TTL for this message:");
                    builder.setItems(colors, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // the user clicked on colors[which]
                            dialog.cancel();
                        }
                    });
                    builder.show();
                }
            });
        }
    }

    private void populateFields(){
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

}
