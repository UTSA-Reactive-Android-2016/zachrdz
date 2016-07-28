package com.csandroid.myfirstapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.csandroid.myfirstapp.api.core.ServerAPI;
import com.csandroid.myfirstapp.db.LocalKeyPairDBHandler;
import com.csandroid.myfirstapp.models.LocalKeyPair;
import com.csandroid.myfirstapp.utils.Crypto;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {

    ServerAPI serverAPI;
    LocalKeyPairDBHandler localKeyPairDB;
    Crypto myCrypto;

    private Menu settingsMenu;

    // Storage Permissions
    private static int RESULT_LOAD_IMG = 1;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final String serverPort = "25666";
    HashMap<String,ServerAPI.UserInfo> myUserMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Settings");

        if(null != getSupportActionBar()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        this.populateFields();

        // Setup on click listeners
        this.initOnClickListeners();

        // Init API setup
        this.initServerAPI();
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        this.settingsMenu = menu;

        Boolean loggedIn = getPreferences(Context.MODE_PRIVATE).getBoolean("loggedIn",false);

        // Show appropriate log in buttons
        settingsMenu.findItem(R.id.action_login).setVisible(!loggedIn);
        settingsMenu.findItem(R.id.action_logout).setVisible(loggedIn);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                return true;
            case R.id.action_login :
                doLogin();
                return true;
            case R.id.action_logout :
                doLogout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /************************** Activity Start Up *************************************************/

    public void populateFields(){

        // Initialize LocalKeyPairDBHandler
        localKeyPairDB = new LocalKeyPairDBHandler(this);

        // Server Address
        String serverName = getPreferences(Context.MODE_PRIVATE).getString("serverName","127.0.0.1");
        setServerNameFieldValue(serverName);

        // Username
        String username = getPreferences(Context.MODE_PRIVATE).getString("username","");
        setUserNameFieldValue(username);

        // User Image
        String userImage = getPreferences(Context.MODE_PRIVATE).getString("userImage","");
        if(!userImage.equals(""))
            setUserImageFieldValue(userImage);

        // Get local key pair for username and set viewable fields
        LocalKeyPair localKeyPair = localKeyPairDB.getKeyPairByUsername(username);
        if(null != localKeyPair){
            setPublicKeyFieldValue(localKeyPair.getPublicKey());
            setPrivateKeyFieldValue(localKeyPair.getPrivateKey());
        }

    }

    public void initOnClickListeners(){
        Button registerBtn = (Button) findViewById(R.id.register);
        ImageView userImage = (ImageView) findViewById(R.id.userImage);
        EditText usernameField = (EditText) findViewById(R.id.username);
        EditText serverNameField = (EditText) findViewById(R.id.servername);

        if(null != registerBtn) {
            registerBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doRegister(v);
                }
            });
        }
        if(null != userImage) {
            userImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    verifyStoragePermissions(SettingsActivity.this);

                    // Create intent to Open Image applications like Gallery, Google Photos
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    // Start the Intent
                    startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
                }
            });
        }
        if(null != usernameField){
            // When the user edits the username text field, update the public/private key fields
            // with associated values if that username exists locally, else empty those fields.
            usernameField.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Upon change of field, automatically log out user if they are logged in
                    Boolean loggedIn = getPreferences(Context.MODE_PRIVATE).getBoolean("loggedIn",false);

                    if(loggedIn){
                        doLogout();
                        Toast.makeText(getApplicationContext(),
                                "You were logged out due to username change.",
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    // User is actively editing username field. Update Key Pair appropriately
                    LocalKeyPair localKeyPair = localKeyPairDB.getKeyPairByUsername(getUserNameFieldValue());
                    if(null != localKeyPair){
                        // Update Public and Private Key Field values
                        setPrivateKeyFieldValue(localKeyPair.getPrivateKey());
                        setPublicKeyFieldValue(localKeyPair.getPublicKey());

                        // Update Key Pair from Activities Preferences
                        getPreferences(Context.MODE_PRIVATE).edit().putString(Crypto.prefPrivateKey,localKeyPair.getPrivateKey()).apply();
                        getPreferences(Context.MODE_PRIVATE).edit().putString(Crypto.prefPublicKey,localKeyPair.getPublicKey()).apply();

                        // Update myCrypto and in turn update ServerAPI with it
                        myCrypto = new Crypto(getPreferences(Context.MODE_PRIVATE));
                        serverAPI.updateMyCrypto(myCrypto);

                    } else{
                        setPrivateKeyFieldValue("");
                        setPublicKeyFieldValue("");
                    }
                }
            });
        }
        if(null != serverNameField){
            // When the user edits the servername text field, log them out if they are logged in
            serverNameField.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Upon change of field, automatically log out user if they are logged in
                    Boolean loggedIn = getPreferences(Context.MODE_PRIVATE).getBoolean("loggedIn",false);

                    if(loggedIn){
                        doLogout();
                        Toast.makeText(getApplicationContext(),
                                "You were logged out due to server address change.",
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

    }

    public void initServerAPI(){
        myCrypto = new Crypto(getPreferences(Context.MODE_PRIVATE));
        myCrypto.saveKeys(getPreferences(Context.MODE_PRIVATE));

        serverAPI = ServerAPI.getInstance(this.getApplicationContext(), myCrypto);

        serverAPI.setServerName(getServerNameFieldValue());
        serverAPI.setServerPort(this.serverPort);

        serverAPI.registerListener(new ServerAPI.Listener() {
            @Override
            public void onCommandFailed(String commandName, VolleyError volleyError) {
                Toast.makeText(SettingsActivity.this,String.format("command %s failed!",commandName),
                        Toast.LENGTH_SHORT).show();
                volleyError.printStackTrace();
            }

            @Override
            public void onGoodAPIVersion() {
                //Toast.makeText(SettingsActivity.this,"API Version Matched!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBadAPIVersion() {
                Toast.makeText(SettingsActivity.this,"API Version Mismatch!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRegistrationSucceeded() {
                Toast.makeText(SettingsActivity.this,"Registered!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRegistrationFailed(String reason) {
                Toast.makeText(SettingsActivity.this,"Not registered!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoginSucceeded() {
                getPreferences(Context.MODE_PRIVATE).edit().putBoolean("loggedIn",true).apply();
                settingsMenu.findItem(R.id.action_login).setVisible(false);
                settingsMenu.findItem(R.id.action_logout).setVisible(true);
                Toast.makeText(SettingsActivity.this,"Logged in!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoginFailed(String reason) {
                Toast.makeText(SettingsActivity.this,"Not logged in : "+reason, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLogoutSucceeded() {
                getPreferences(Context.MODE_PRIVATE).edit().putBoolean("loggedIn",false).apply();
                settingsMenu.findItem(R.id.action_login).setVisible(true);
                settingsMenu.findItem(R.id.action_logout).setVisible(false);
                Toast.makeText(SettingsActivity.this,"Logged out!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLogoutFailed(String reason) {
                Toast.makeText(SettingsActivity.this,"Not logged out!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUserInfo(ServerAPI.UserInfo info) {
                myUserMap.put(info.username,info);
            }

            @Override
            public void onUserNotFound(String username) {
                Toast.makeText(SettingsActivity.this,String.format("user %s not found!",username),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onContactLogin(String username) {
                Toast.makeText(SettingsActivity.this,String.format("user %s logged in",username),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onContactLogout(String username) {
                Toast.makeText(SettingsActivity.this,String.format("user %s logged out",username),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSendMessageSucceeded(Object key) {
                Toast.makeText(SettingsActivity.this,String.format("sent a message"),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSendMessageFailed(Object key, String reason) {
                Toast.makeText(SettingsActivity.this,String.format("failed to send a message"),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMessageDelivered(String sender, String recipient, String subject, String body, long born_on_date, long time_to_live) {
                Toast.makeText(SettingsActivity.this,String.format("got message from %s",sender),Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*
     *  Make sure to save off fields to preferences before closed or interupted
     */
    @Override
    protected void onPause() {
        super.onPause();

        // Server Address
        String serverName = getServerNameFieldValue();
        getPreferences(Context.MODE_PRIVATE).edit().putString("serverName",serverName).apply();

        // User Image gets saved to preferences on user selection callback (Should be good)

        // User Name
        String username = getUserNameFieldValue();
        getPreferences(Context.MODE_PRIVATE).edit().putString("username",username).apply();


        // Set Shared Preferences for Application Wide Use
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("loggedIn", getPreferences(Context.MODE_PRIVATE).getBoolean("loggedIn",false));
        editor.putString("username", getPreferences(Context.MODE_PRIVATE).getString("username",""));
        editor.putString("serverName", getServerNameFieldValue());
        editor.putString("serverPort", this.serverPort);
        editor.apply();
    }

    /************************** Action Functions **************************************************/

    public void doRegister(View view) {
        // Set Server Address by getting it from EditText field
        serverAPI.setServerName(getServerNameFieldValue());
        serverAPI.checkAPIVersion();

        // Set username by getting it from EditText field
        String username = getUserNameFieldValue();

        // Set user image from activities preferences
        String userImage = getPreferences(Context.MODE_PRIVATE).getString("userImage","");

        // Initial Private & Public key holders
        String privateKey;
        String publicKey;

        // Make sure an image is selected
        if(userImage.equals("")){
            Toast.makeText(this, "You must select a profile image.", Toast.LENGTH_LONG).show();
        } else if(null == username || username.length() < 3){
            // Username length must at least be 3
            Toast.makeText(this, "Your username must be at least 3 character long", Toast.LENGTH_LONG).show();
        } else {
            // Validation passed, attempt to register user.
            LocalKeyPair localKeyPair = localKeyPairDB.getKeyPairByUsername(username);

            // Check if we've already generated a key pair for that name,
            // that may be stored in our local sqlite db.
            if(null != localKeyPair){
                // Key Pair exists locally for this username, register the user.
                serverAPI.register(username, userImage, localKeyPair.getPublicKey());

                privateKey = localKeyPair.getPrivateKey();
                publicKey = localKeyPair.getPublicKey();
            } else{
                // Key Pair does not exist for this username (User possibly edited username text field)

                // Clear out Key Pair from Activities Preferences
                getPreferences(Context.MODE_PRIVATE).edit().putString(Crypto.prefPrivateKey,"").apply();
                getPreferences(Context.MODE_PRIVATE).edit().putString(Crypto.prefPublicKey,"").apply();

                // Generate Key Pair and save it back to this Activities Preferences
                myCrypto = new Crypto(getPreferences(Context.MODE_PRIVATE));
                myCrypto.saveKeys(getPreferences(Context.MODE_PRIVATE));

                // Grab the private key and public key that were generated
                String newPrivateKey = getPreferences(Context.MODE_PRIVATE).getString(Crypto.prefPrivateKey,"");
                String newPublicKey = getPreferences(Context.MODE_PRIVATE).getString(Crypto.prefPublicKey,"");

                // Save them to our local Key Pair SQLite database
                localKeyPair = new LocalKeyPair(username, newPublicKey, newPrivateKey);
                localKeyPairDB.addKeyPair(localKeyPair);

                // Register the new user
                serverAPI.register(username, userImage, localKeyPair.getPublicKey());

                privateKey = newPrivateKey;
                publicKey = newPublicKey;
            }

            // Update Public and Private Key Field values
            setPrivateKeyFieldValue(privateKey);
            setPublicKeyFieldValue(publicKey);
        }
    }

    public void doLogin() {
        serverAPI.setServerName(getServerNameFieldValue());
        serverAPI.checkAPIVersion();
        serverAPI.login(getUserNameFieldValue(),myCrypto);
    }

    public void doLogout() {
        serverAPI.setServerName(getServerNameFieldValue());
        serverAPI.checkAPIVersion();
        serverAPI.logout(getUserNameFieldValue(),myCrypto);
    }

    /************************** Image Selection Functions *****************************************/

    /**
     * Checks if the app has permission to write to device storage
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK
                    && null != data) {
                // Get the Image from data

                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };

                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                // Move to first row
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String imgDecodableString = cursor.getString(columnIndex);
                cursor.close();
                ImageView imgView = (ImageView) findViewById(R.id.userImage);
                // Remove background
                imgView.setBackgroundResource(0);

                Bitmap bitmap = BitmapFactory.decodeFile(imgDecodableString);

                if(bitmap.getHeight() > 512 || bitmap.getWidth() > 512){
                    Toast.makeText(this, "Image must be less than 512 x 512px!",
                            Toast.LENGTH_LONG).show();
                } else{
                    String userImageSelected = encodeToBase64(bitmap, Bitmap.CompressFormat.PNG, 100);

                    // Save encode image to this Activities preferences
                    getPreferences(Context.MODE_PRIVATE).edit().putString("userImage",userImageSelected).apply();

                    // Set the Image in ImageView after decoding the String
                    imgView.setImageBitmap(bitmap);
                }
            } else {
                Toast.makeText(this, "You haven't picked Image",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
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

    private String getUserNameFieldValue(){
        EditText usernameField = ((EditText)findViewById(R.id.username));
        if(usernameField != null) {
            return usernameField.getText().toString();
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

    private String getServerNameFieldValue(){
        EditText serverNameField = ((EditText)findViewById(R.id.servername));
        if(serverNameField != null) {
            return serverNameField.getText().toString();
        } else{
            return "";
        }
    }

    private void setServerNameFieldValue(String value){
        EditText serverNameField = ((EditText)findViewById(R.id.servername));
        if(serverNameField != null) {
            serverNameField.setText(value);
        }
    }

    private void setPrivateKeyFieldValue(String value){
        TextView privateKeyField = ((TextView)findViewById(R.id.userPrivateKey));
        if(privateKeyField != null) {
            privateKeyField.setText(value);
        }
    }

    private void setPublicKeyFieldValue(String value){
        TextView publicKeyField = ((TextView)findViewById(R.id.userPublicKey));
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
