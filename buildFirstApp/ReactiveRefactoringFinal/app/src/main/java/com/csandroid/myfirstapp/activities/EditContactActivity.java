package com.csandroid.myfirstapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.csandroid.myfirstapp.R;
import com.csandroid.myfirstapp.db.ContactDBHandler;
import com.csandroid.myfirstapp.models.Contact;

import java.io.ByteArrayOutputStream;

public class EditContactActivity extends AppCompatActivity {

    private int contactId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_contact);
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

    public void populateFields(){
        //Get the bundle
        Bundle bundle = getIntent().getExtras();

        if(null != bundle){
            //Extract the data…
            String contactId = bundle.getString("contact_id");
            this.contactId = Integer.parseInt(contactId);
        }

        TextView username = (TextView)findViewById(R.id.username);
        ImageView userImage = (ImageView)findViewById(R.id.userImage);
        TextView publicKey = (TextView)findViewById(R.id.publicKey);

        ContactDBHandler db = new ContactDBHandler(this);

        Contact contact = db.getContact(this.contactId);

        if(null != username && null != publicKey && null != userImage) {
            username.setText(contact.getUsername());
            publicKey.setText(contact.getPublicKey());
            setUserImageFieldValue(contact.getUserImage());
        }
    }

    public void initOnClickListeners(){
        ImageButton deleteBtn = (ImageButton) findViewById(R.id.imageButton2);

        if(null != deleteBtn) {
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent contactsIntent = new Intent(EditContactActivity.this, ContactsActivity.class);
                    ContactDBHandler db = new ContactDBHandler(v.getContext());
                    Contact contact = db.getContact(EditContactActivity.this.contactId);
                    db.deleteContact(contact);
                    Toast.makeText(v.getContext(), "Deleted contact: " + contact.getUsername(),
                            Toast.LENGTH_LONG).show();
                    contactsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(contactsIntent);
                }
            });
        }
    }

    /************************** Bitmap (Encoding & Decoding) **************************************/

    // Encode a bitmap to a String
    public static String encodeToBase64(Bitmap image, Bitmap.CompressFormat compressFormat, int quality)
    {
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        image.compress(compressFormat, quality, byteArrayOS);
        return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT);
    }

    // Decode a string into a bitmap
    public static Bitmap decodeBase64(String input)
    {
        byte[] decodedBytes = Base64.decode(input, 0);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    /************************** Activity Fields (Getters & Setters) *******************************/

    private void setUserImageFieldValue(String value){
        // value must be encoded bitmap string
        ImageView userImageField = ((ImageView)findViewById(R.id.userImage));
        if(userImageField != null) {
            userImageField.setBackgroundResource(0);
            userImageField.setImageBitmap(decodeBase64(value));
        }
    }
}
