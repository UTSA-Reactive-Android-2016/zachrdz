package com.csandroid.myfirstapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.csandroid.myfirstapp.R;
import com.csandroid.myfirstapp.db.ContactDBHandler;
import com.csandroid.myfirstapp.models.Contact;
import com.csandroid.myfirstapp.stages.RemoveContactStage;
import com.csandroid.myfirstapp.utils.Crypto;

import java.io.ByteArrayOutputStream;
import java.security.PublicKey;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class EditContactActivity extends AppCompatActivity {

    private int contactId;
    private CompositeSubscription cs = new CompositeSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_contact);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Edit Contact");
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String username = prefs.getString("username","");
        String hostName = prefs.getString("serverName", "");
        String portNumber = prefs.getString("serverPort", "");
        PublicKey serverKey = Crypto.getPublicKeyFromString(prefs.getString("serverKey", ""));
        final String server = "http://" + hostName + ":" + portNumber;

        if(null != deleteBtn) {
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ContactDBHandler db = new ContactDBHandler(v.getContext());
                    Contact contact = db.getContact(EditContactActivity.this.contactId);
                    doRemoveContact(username, server, contact.getUsername());
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

    @Override
    public void onResume(){
        super.onResume();
        cs = new CompositeSubscription();
    }

    @Override
    public void onPause(){
        super.onPause();
        cs.unsubscribe();
        cs = null;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(null != cs){
            cs.unsubscribe();
        }
        cs = null;
    }

    public void doRemoveContact(final String username, final String server, final String contact){
        // Attempt to search for user
        Subscription removeContactSub = Observable.just("ok")
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .flatMap(new RemoveContactStage(server, username, contact))
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {}
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        final Throwable fe = e;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("ZachLog","Error: Contact Removal",fe);
                            }
                        });
                    }

                    @Override
                    public void onNext(final String response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(response.equals("ok")){
                                    Toast.makeText(EditContactActivity.this,String.format("Unlinked contact",username),Toast.LENGTH_SHORT).show();

                                    // Remove contact locally now and go back to contacts Activity
                                    Intent contactsIntent = new Intent(EditContactActivity.this, ContactsActivity.class);
                                    ContactDBHandler db = new ContactDBHandler(getApplicationContext());
                                    Contact contact = db.getContact(EditContactActivity.this.contactId);
                                    db.deleteContact(contact);
                                    Toast.makeText(EditContactActivity.this, "Deleted contact: " + contact.getUsername(), Toast.LENGTH_LONG).show();
                                    contactsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(contactsIntent);
                                } else{
                                    Toast.makeText(EditContactActivity.this,String.format("Error: Unable to unlink contact",username),Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });

        cs.add(removeContactSub);
    }
}
