package com.csandroid.myfirstapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.csandroid.myfirstapp.api.core.ServerAPI;
import com.csandroid.myfirstapp.db.ContactDBHandler;
import com.csandroid.myfirstapp.db.LocalKeyPairDBHandler;
import com.csandroid.myfirstapp.models.Contact;
import com.csandroid.myfirstapp.models.LocalKeyPair;
import com.csandroid.myfirstapp.utils.Crypto;
import com.csandroid.myfirstapp.utils.EncryptHelper;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.util.HashMap;

public class AddContactActivity extends AppCompatActivity {

    ServerAPI serverAPI;
    LocalKeyPairDBHandler localKeyPairDB;
    LocalKeyPair localKeyPair;
    Crypto myCrypto;
    HashMap<String,ServerAPI.UserInfo> myUserMap = new HashMap<>();


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
        this.initServerAPI();
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
        ImageButton searchBtn = (ImageButton) findViewById(R.id.search_button);

        if(null != saveBtn && null != usernameField && null != publicKeyField) {
            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ServerAPI.UserInfo userSelected = myUserMap.get(getUserNameFieldValue());

                    if (userSelected != null){
                        ContactDBHandler db = new ContactDBHandler(AddContactActivity.this);
                        Contact existingUser = db.getContactByUsername(userSelected.username);
                        if(existingUser != null && existingUser.getLocalKeyPairId() == localKeyPair.getId()){
                            String errMsg = "You already have this user in your contacts!";
                            Toast.makeText(v.getContext(), "Error: " + errMsg,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Intent contactsIntent = new Intent(AddContactActivity.this, ContactsActivity.class);

                            Contact contact = new Contact();
                            contact.setLocalKeyPairId(localKeyPair.getId());
                            contact.setUsername(userSelected.username);
                            contact.setUserImage(userSelected.image);
                            contact.setPublicKey(getPublicKeyFieldValue());
                            db.addContact(contact);
                            Toast.makeText(v.getContext(), "Added Contact: " + contact.getUsername(),
                                    Toast.LENGTH_LONG).show();
                            contactsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(contactsIntent);
                        }

                    } else if(getUserNameFieldValue().length() < 1){
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
        if(null != searchBtn){
            searchBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != usernameField && usernameField.getText().toString().length() > 0) {
                        serverAPI.getUserInfo(getUserNameFieldValue());
                    } else if(null != usernameField && usernameField.getText().toString().length() == 0){
                        String errMsg = "You didn't type anything to search!";
                        Toast.makeText(v.getContext(), "Error: " + errMsg,
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    public void initServerAPI(){
        // Logged in User setup for serverAPI
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString("username","");
        String hostName = prefs.getString("serverName", "");
        String portNumber = prefs.getString("serverPort", "");

        localKeyPairDB = new LocalKeyPairDBHandler(this);
        localKeyPair = localKeyPairDB.getKeyPairByUsername(username);
        getPreferences(Context.MODE_PRIVATE).edit().putString(Crypto.prefPrivateKey,localKeyPair.getPrivateKey()).apply();
        getPreferences(Context.MODE_PRIVATE).edit().putString(Crypto.prefPublicKey,localKeyPair.getPublicKey()).apply();

        myCrypto = new Crypto(getPreferences(Context.MODE_PRIVATE));
        serverAPI = ServerAPI.getInstance(this.getApplicationContext(), myCrypto);
        serverAPI.setServerName(hostName);
        serverAPI.setServerPort(portNumber);

        serverAPI.registerListener(new ServerAPI.Listener() {
            @Override
            public void onCommandFailed(String commandName, VolleyError volleyError) {
                Toast.makeText(AddContactActivity.this,String.format("command %s failed!",commandName),
                        Toast.LENGTH_SHORT).show();
                volleyError.printStackTrace();
            }

            @Override
            public void onGoodAPIVersion() {
                //Toast.makeText(AddContactActivity.this,"API Version Matched!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBadAPIVersion() {
                Toast.makeText(AddContactActivity.this,"API Version Mismatch!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRegistrationSucceeded() {
                Toast.makeText(AddContactActivity.this,"Registered!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRegistrationFailed(String reason) {
                Toast.makeText(AddContactActivity.this,"Not registered!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoginSucceeded() {
                Toast.makeText(AddContactActivity.this,"Logged in!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoginFailed(String reason) {
                Toast.makeText(AddContactActivity.this,"Not logged in : "+reason, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLogoutSucceeded() {
                Toast.makeText(AddContactActivity.this,"Logged out!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLogoutFailed(String reason) {
                Toast.makeText(AddContactActivity.this,"Not logged out!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUserInfo(ServerAPI.UserInfo info) {
                myUserMap.put(info.username,info);

                setPublicKeyFieldValue(Crypto.getPublicKeyString(info.publicKey));
                setUserImageFieldValue(info.image);
                Toast.makeText(AddContactActivity.this,String.format("User Found: %s",info.username),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUserNotFound(String username) {
                Toast.makeText(AddContactActivity.this,String.format("user %s not found!",username),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onContactLogin(String username) {
                Toast.makeText(AddContactActivity.this,String.format("user %s logged in",username),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onContactLogout(String username) {
                Toast.makeText(AddContactActivity.this,String.format("user %s logged out",username),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSendMessageSucceeded(Object key) {
                Toast.makeText(AddContactActivity.this,String.format("sent a message"),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSendMessageFailed(Object key, String reason) {
                Toast.makeText(AddContactActivity.this,String.format("failed to send a message"),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMessageDelivered(String sender, String recipient, String subject, String body, long born_on_date, long time_to_live) {
                Toast.makeText(AddContactActivity.this,String.format("got message from %s",sender),Toast.LENGTH_SHORT).show();
            }
        });
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

    private String getUserNameFieldValue(){
        EditText usernameField = ((EditText)findViewById(R.id.username));
        if(usernameField != null) {
            return usernameField.getText().toString();
        } else{
            return "";
        }
    }

    private String getPublicKeyFieldValue(){
        TextView publicKeyField = ((TextView)findViewById(R.id.publicKey));
        if(publicKeyField != null) {
            return publicKeyField.getText().toString();
        } else{
            return "";
        }
    }

    private void setUserNameFieldValue(String value){
        EditText usernameField = ((EditText)findViewById(R.id.username));
        if(usernameField != null) {
            usernameField.setText(value);
        }
    }

    private void setPublicKeyFieldValue(String value){
        TextView publicKeyField = ((TextView)findViewById(R.id.publicKey));
        if(publicKeyField != null) {
            publicKeyField.setText(value);
        }
    }

    private void setUserImageFieldValue(String value){
        // value must be encoded bitmap string
        ImageView userImageField = ((ImageView)findViewById(R.id.userImage));
        if(userImageField != null) {
            userImageField.setBackgroundResource(0);
            userImageField.setImageBitmap(decodeBase64(value));
        }
    }

}

