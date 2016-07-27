package com.csandroid.myfirstapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
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
import com.csandroid.myfirstapp.api.ServerAPI;
import com.csandroid.myfirstapp.db.LocalKeyPairDBHandler;
import com.csandroid.myfirstapp.models.LocalKeyPair;
import com.csandroid.myfirstapp.utils.Crypto;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {

    ServerAPI serverAPI;
    LocalKeyPair lkp;
    Crypto myCrypto;

    // Storage Permissions
    private static int RESULT_LOAD_IMG = 1;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private String userImageSelected;

    HashMap<String,ServerAPI.UserInfo> myUserMap = new HashMap<>();

    private String getUserName(){
        return ((EditText)findViewById(R.id.username)).getText().toString();
    }

    private String getServerName(){
        return ((EditText)findViewById(R.id.servername)).getText().toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(null != getSupportActionBar()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        this.populateFields();

        // Setup on click listeners
        this.initOnClickListeners();

        // Init API setup
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

    public void populateFields(){
        TextView userPublicKey = (TextView)findViewById(R.id.userPublicKey);
        TextView userPrivateKey = (TextView)findViewById(R.id.userPrivateKey);
        TextView username = (TextView)findViewById(R.id.username);
        ImageView userImage = (ImageView)findViewById(R.id.userImage);

        LocalKeyPairDBHandler db = new LocalKeyPairDBHandler(this);
        lkp = db.getKeyPair();

        if(null != userPublicKey && null != userPrivateKey && null != userImage && null != username) {
            String coolDog = "https://pbs.twimg.com/profile_images/565602752152076288/NxWEBoTo.jpeg";

            username.setText("zachrdz");
            userPublicKey.setText(lkp.getPublicKey());
            userPrivateKey.setText(lkp.getPrivateKey());
            Picasso.with(getApplicationContext()).load(coolDog).into(userImage);
        }
    }

    public void initOnClickListeners(){
        Button registerBtn = (Button) findViewById(R.id.register);
        ImageView userImage = (ImageView) findViewById(R.id.userImage);

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
    }

    public void initServerAPI(){
        String serverName = getPreferences(Context.MODE_PRIVATE).getString("ServerName","127.0.0.1");
        ((EditText)findViewById(R.id.servername)).setText(serverName);

        myCrypto = new Crypto(getPreferences(Context.MODE_PRIVATE));
        myCrypto.saveKeys(getPreferences(Context.MODE_PRIVATE));

        serverAPI = ServerAPI.getInstance(this.getApplicationContext(),
                myCrypto);

        serverAPI.setServerName(getServerName());
        serverAPI.setServerPort("25666");

        serverAPI.registerListener(new ServerAPI.Listener() {
            @Override
            public void onCommandFailed(String commandName, VolleyError volleyError) {
                Toast.makeText(SettingsActivity.this,String.format("command %s failed!",commandName),
                        Toast.LENGTH_SHORT).show();
                volleyError.printStackTrace();
            }

            @Override
            public void onGoodAPIVersion() {
                Toast.makeText(SettingsActivity.this,"API Version Matched!", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(SettingsActivity.this,"Logged in!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoginFailed(String reason) {
                Toast.makeText(SettingsActivity.this,"Not logged in : "+reason, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLogoutSucceeded() {
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

    public void doRegister(View view) {
        serverAPI.setServerName(getServerName());

        if(userImageSelected == null){
            Toast.makeText(this, "You must select an image, other than default.", Toast.LENGTH_LONG)
                    .show();
        }

        String username = ((EditText)findViewById(R.id.username)).getText().toString();
        serverAPI.register(username, userImageSelected, lkp.getPublicKey());
    }

    public void doLogin(View view) {
        serverAPI.setServerName(getServerName());

        serverAPI.login(getUserName(),myCrypto);
    }

    public void doLogout(View view) {
        serverAPI.setServerName(getServerName());

        serverAPI.logout(getUserName(),myCrypto);
    }

    /**
     * Checks if the app has permission to write to device storage
     *
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
                    userImageSelected = encodeToBase64(bitmap, Bitmap.CompressFormat.PNG, 100);
                    // Set the Image in ImageView after decoding the String
                    imgView.setImageBitmap(bitmap);
                }
            } else {
                Toast.makeText(this, "You haven't picked Image",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG)
                    .show();
        }

    }

    public static String encodeToBase64(Bitmap image, Bitmap.CompressFormat compressFormat, int quality)
    {
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        image.compress(compressFormat, quality, byteArrayOS);
        return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT);
    }

    public static Bitmap decodeBase64(String input)
    {
        byte[] decodedBytes = Base64.decode(input, 0);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }
}
