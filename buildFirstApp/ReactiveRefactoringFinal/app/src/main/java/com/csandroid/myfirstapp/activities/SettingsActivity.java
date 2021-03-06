package com.csandroid.myfirstapp.activities;

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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.csandroid.myfirstapp.R;
import com.csandroid.myfirstapp.db.LocalKeyPairDBHandler;
import com.csandroid.myfirstapp.models.LocalKeyPair;
import com.csandroid.myfirstapp.stages.GetChallengeStage;
import com.csandroid.myfirstapp.stages.GetServerKeyStage;
import com.csandroid.myfirstapp.stages.LogInStage;
import com.csandroid.myfirstapp.stages.LogOutStage;
import com.csandroid.myfirstapp.stages.RegistrationStage;
import com.csandroid.myfirstapp.utils.Crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class SettingsActivity extends AppCompatActivity {

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

    // Subscription holder
    private CompositeSubscription cs = new CompositeSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Settings");

        if (null != getSupportActionBar()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        this.populateFields();

        // Setup on click listeners
        this.initOnClickListeners();

        myCrypto = new Crypto(getPreferences(Context.MODE_PRIVATE));
        myCrypto.saveKeys(getPreferences(Context.MODE_PRIVATE));
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        this.settingsMenu = menu;

        Boolean loggedIn = getPreferences(Context.MODE_PRIVATE).getBoolean("loggedIn", false);

        // Show appropriate log in buttons
        settingsMenu.findItem(R.id.action_login).setVisible(!loggedIn);
        settingsMenu.findItem(R.id.action_logout).setVisible(loggedIn);
        toggleEdit(loggedIn);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                return true;
            case R.id.action_login:
                doLogin();
                return true;
            case R.id.action_logout:
                doLogout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**************************
     * Activity Start Up
     *************************************************/

    public void populateFields() {

        // Initialize LocalKeyPairDBHandler
        localKeyPairDB = new LocalKeyPairDBHandler(this);

        // Server Address
        String serverName = getPreferences(Context.MODE_PRIVATE).getString("serverName", "127.0.0.1");
        setServerNameFieldValue(serverName);

        // Username
        String username = getPreferences(Context.MODE_PRIVATE).getString("username", "");
        setUserNameFieldValue(username);

        // User Image
        String userImage = getPreferences(Context.MODE_PRIVATE).getString("userImage", "");
        if (!userImage.equals(""))
            setUserImageFieldValue(userImage);

        // Get local key pair for username and set viewable fields
        LocalKeyPair localKeyPair = localKeyPairDB.getKeyPairByUsername(username);
        if (null != localKeyPair) {
            setPublicKeyFieldValue(localKeyPair.getPublicKey());
            setPrivateKeyFieldValue(localKeyPair.getPrivateKey());
        }

    }

    public void initOnClickListeners() {
        Button registerBtn = (Button) findViewById(R.id.register);
        Button forceLogoutBtn = (Button) findViewById(R.id.forceLogout);
        ImageView userImage = (ImageView) findViewById(R.id.userImage);
        EditText usernameField = (EditText) findViewById(R.id.username);
        EditText serverNameField = (EditText) findViewById(R.id.servername);

        if (null != registerBtn) {
            registerBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doRegister(v);
                }
            });
        }
        if (null != forceLogoutBtn) {
            forceLogoutBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getPreferences(Context.MODE_PRIVATE).edit().putBoolean("loggedIn", false).apply();
                    settingsMenu.findItem(R.id.action_login).setVisible(true);
                    settingsMenu.findItem(R.id.action_logout).setVisible(false);

                    toggleEdit(false);

                    Toast.makeText(SettingsActivity.this, "Forced logout locally. (Not on Server)", Toast.LENGTH_LONG).show();
                }
            });
        }
        if (null != userImage) {
            userImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    verifyStoragePermissions(SettingsActivity.this);

                    // Create intent to Open Image applications like Gallery, Google Photos
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    // Start the Intent
                    startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
                }
            });
        }
        if (null != usernameField) {
            // When the user edits the username text field, update the public/private key fields
            // with associated values if that username exists locally, else empty those fields.
            usernameField.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // User is actively editing username field. Update Key Pair appropriately
                    LocalKeyPair localKeyPair = localKeyPairDB.getKeyPairByUsername(getUserNameFieldValue());
                    if (null != localKeyPair) {
                        // Update Public and Private Key Field values
                        setPrivateKeyFieldValue(localKeyPair.getPrivateKey());
                        setPublicKeyFieldValue(localKeyPair.getPublicKey());

                        // Update Key Pair from Activities Preferences
                        getPreferences(Context.MODE_PRIVATE).edit().putString(Crypto.prefPrivateKey, localKeyPair.getPrivateKey()).apply();
                        getPreferences(Context.MODE_PRIVATE).edit().putString(Crypto.prefPublicKey, localKeyPair.getPublicKey()).apply();

                        // Update myCrypto
                        myCrypto = new Crypto(getPreferences(Context.MODE_PRIVATE));
                    } else {
                        setPrivateKeyFieldValue("");
                        setPublicKeyFieldValue("");
                    }

                }
            });
        }
        if (null != serverNameField) {
            // When the user edits the servername text field, log them out if they are logged in
            serverNameField.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

    }

    /*
     *  Make sure to save off fields to preferences before closed or interrupted
     */
    @Override
    protected void onPause() {
        super.onPause();

        // Server Address
        String serverName = getServerNameFieldValue();
        getPreferences(Context.MODE_PRIVATE).edit().putString("serverName", serverName).apply();

        // User Image gets saved to preferences on user selection callback (Should be good)

        // User Name
        String username = getUserNameFieldValue();
        getPreferences(Context.MODE_PRIVATE).edit().putString("username", username).apply();


        // Set Shared Preferences for Application Wide Use
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("loggedIn", getPreferences(Context.MODE_PRIVATE).getBoolean("loggedIn", false));
        editor.putString("username", getPreferences(Context.MODE_PRIVATE).getString("username", ""));
        editor.putString("serverName", getServerNameFieldValue());
        editor.putString("serverPort", this.serverPort);
        editor.putString("serverKey", getPreferences(Context.MODE_PRIVATE).getString("serverKey", ""));
        editor.apply();

        // Remove subscriptions
        cs.unsubscribe();
    }

    @Override
    public void onResume() {
        super.onResume();
        cs.unsubscribe();
        cs = null;
        cs = new CompositeSubscription();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cs.unsubscribe();
        cs = null;
    }

    /**************************
     * Action Functions
     **************************************************/

    public void doRegister(View view) {
        // Set servername by getting it from EditText field
        String serverName = getServerNameFieldValue();
        serverName = "http://"+serverName+":25666";

        // Set username by getting it from EditText field
        String username = getUserNameFieldValue();

        // Set user image from activities preferences
        String userImage = getPreferences(Context.MODE_PRIVATE).getString("userImage", "");

        // Initial Private & Public key holders
        String privateKey;
        String publicKey;

        // Make sure an image is selected
        if (username.length() < 3) {
            // Username length must at least be 3
            Toast.makeText(this, "Your username must be at least 3 character long", Toast.LENGTH_SHORT).show();
        } else {
            // Validation passed, attempt to register user.
            LocalKeyPair localKeyPair = localKeyPairDB.getKeyPairByUsername(username);

            // Check if we've already generated a key pair for that name,
            // that may be stored in our local sqlite db.
            if (null != localKeyPair) {
                // Key Pair exists locally for this username, register the user.
                //serverAPI.register(username, userImage, localKeyPair.getPublicKey());

                privateKey = localKeyPair.getPrivateKey();
                publicKey = localKeyPair.getPublicKey();
            } else {
                // Key Pair does not exist for this username (User possibly edited username text field)

                // Clear out Key Pair from Activities Preferences
                getPreferences(Context.MODE_PRIVATE).edit().remove(Crypto.prefPrivateKey).apply();
                getPreferences(Context.MODE_PRIVATE).edit().remove(Crypto.prefPublicKey).apply();

                // Generate Key Pair and save it back to this Activities Preferences
                myCrypto = new Crypto(getPreferences(Context.MODE_PRIVATE));
                myCrypto.saveKeys(getPreferences(Context.MODE_PRIVATE));

                // Grab the private key and public key that were generated
                String newPrivateKey = getPreferences(Context.MODE_PRIVATE).getString(Crypto.prefPrivateKey, "");
                String newPublicKey = getPreferences(Context.MODE_PRIVATE).getString(Crypto.prefPublicKey, "");

                // Save them to our local Key Pair SQLite database
                localKeyPair = new LocalKeyPair(username, newPublicKey, newPrivateKey);
                localKeyPairDB.addKeyPair(localKeyPair);

                // Set Default User Image if none selected
                if (userImage == "") {
                    InputStream is;
                    byte[] buffer = new byte[0];
                    try {
                        is = getAssets().open("images/ic_android_black_24dp.png");
                        buffer = new byte[is.available()];
                        is.read(buffer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    userImage = Base64.encodeToString(buffer, Base64.DEFAULT).trim();
                }

                privateKey = newPrivateKey;
                publicKey = newPublicKey;
            }

            // Attempt to register the user
            Subscription registerSub = Observable.just(0)
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(Schedulers.io())
                    .flatMap(new GetServerKeyStage(
                            serverName))
                    .flatMap(new RegistrationStage(
                            serverName,
                            username,
                            userImage,
                            publicKey))
                    .subscribe(new Observer<PublicKey>() {
                        @Override
                        public void onCompleted() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d("ZachLog","Success: Registration");
                                    Toast.makeText(SettingsActivity.this, "Registered!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onError(Throwable e) {
                            final Throwable fe = e;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d("ZachLog","Error: Registration",fe);
                                    Toast.makeText(SettingsActivity.this, "Not registered! Server Unavailable.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onNext(final PublicKey key) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // handle initial state here
                                    Log.d("ZachLOG", "Next Register: " + Crypto.getPublicKeyString(key));

                                    // Save server key to shared pref.
                                    getPreferences(Context.MODE_PRIVATE).edit().putString("serverKey", Crypto.getPublicKeyString(key)).apply();
                                }
                            });
                        }
                    });

            cs.add(registerSub);

            // Update Public and Private Key Field values
            setPrivateKeyFieldValue(privateKey);
            setPublicKeyFieldValue(publicKey);
        }
    }

    public void doLogin() {
        // Set server name by getting it from EditText field
        String serverName = getServerNameFieldValue();
        serverName = "http://"+serverName+":25666";

        // Set username by getting it from EditText field
        final String username = getUserNameFieldValue();

        String serverKey = getPreferences(Context.MODE_PRIVATE).getString("serverKey", "");
        PublicKey sKey = Crypto.getPublicKeyFromString(serverKey);

        if(serverKey.length() == 0){
            Toast.makeText(SettingsActivity.this, "User is not registered!", Toast.LENGTH_SHORT).show();
        } else {

            // Attempt to login the user
            Subscription loginSub = Observable.just(sKey)
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(Schedulers.io())
                    .flatMap(new Func1<PublicKey, Observable<PublicKey>>() {
                        @Override
                        public Observable<PublicKey> call(PublicKey key) {
                            return Observable.just(key);
                        }
                    })
                    .flatMap(new GetChallengeStage(serverName, username, myCrypto))
                    .flatMap(new LogInStage(serverName, username))
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
                                    //Log.d("ZachLog", "Error: Login", fe);
                                    Toast.makeText(SettingsActivity.this, "Login Failed!", Toast.LENGTH_SHORT).show();
                                    Toast.makeText(SettingsActivity.this, "Have you registered?", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onNext(final String response) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // handle initial state here
                                    Log.d("ZachLOG", "Next Login: " + response);
                                    if(response.equalsIgnoreCase("OK")){
                                        Log.d("ZachLOG", "Success: Login");
                                        getPreferences(Context.MODE_PRIVATE).edit().putBoolean("loggedIn", true).apply();
                                        settingsMenu.findItem(R.id.action_login).setVisible(false);
                                        settingsMenu.findItem(R.id.action_logout).setVisible(true);

                                        Toast.makeText(SettingsActivity.this, "Logged in!", Toast.LENGTH_SHORT).show();

                                        toggleEdit(true);
                                    } else if (response.equalsIgnoreCase("fail")){
                                        Toast.makeText(SettingsActivity.this, "Challenge Failed!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    });

            cs.add(loginSub);
        }
    }

    public void doLogout() {
        // Set servername by getting it from EditText field
        String serverName = getServerNameFieldValue();
        serverName = "http://"+serverName+":25666";

        // Set username by getting it from EditText field
        String username = getUserNameFieldValue();

        String serverKey = getPreferences(Context.MODE_PRIVATE).getString("serverKey", "");
        PublicKey sKey = Crypto.getPublicKeyFromString(serverKey);

        if(serverKey.length() == 0){
            Toast.makeText(SettingsActivity.this, "User is not registered!", Toast.LENGTH_SHORT).show();
        } else {

            // Attempt to login the user
            Subscription logoutSub = Observable.just(sKey)
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(Schedulers.io())
                    .flatMap(new Func1<PublicKey, Observable<PublicKey>>() {
                        @Override
                        public Observable<PublicKey> call(PublicKey key) {
                            return Observable.just(key);
                        }
                    })
                    .flatMap(new GetChallengeStage(serverName, username, myCrypto))
                    .flatMap(new LogOutStage(serverName, username))
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
                                    Log.d("ZachLog", "Error: Logout", fe);
                                    Toast.makeText(SettingsActivity.this, "Not logged out! Server Unavailable.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onNext(final String response) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // handle initial state here
                                    Log.d("ZachLOG", "Next Logout: " + response);
                                    if(response.equalsIgnoreCase("OK")) {
                                        Log.d("ZachLog", "Success: Logout");
                                        getPreferences(Context.MODE_PRIVATE).edit().putBoolean("loggedIn", false).apply();
                                        settingsMenu.findItem(R.id.action_login).setVisible(true);
                                        settingsMenu.findItem(R.id.action_logout).setVisible(false);
                                        Toast.makeText(SettingsActivity.this, "Logged out!", Toast.LENGTH_SHORT).show();
                                        toggleEdit(false);
                                    } else if (response.equalsIgnoreCase("fail")){
                                        Toast.makeText(SettingsActivity.this, "Challenge Failed!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    });

            cs.add(logoutSub);
        }
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
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

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

                if (bitmap.getHeight() > 512 || bitmap.getWidth() > 512) {
                    Toast.makeText(this, "Image must be less than 512 x 512px!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    String userImageSelected = encodeToBase64(bitmap, Bitmap.CompressFormat.PNG, 100);

                    // Save encode image to this Activities preferences
                    getPreferences(Context.MODE_PRIVATE).edit().putString("userImage", userImageSelected).apply();

                    // Set the Image in ImageView after decoding the String
                    imgView.setImageBitmap(bitmap);
                }
            } else {
                Toast.makeText(this, "You haven't picked Image",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
        }

    }

    /**************************
     * Bitmap (Encoding & Decoding)
     **************************************/

    // Encode a bitmap to a String
    public static String encodeToBase64(Bitmap image, Bitmap.CompressFormat compressFormat, int quality) {
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        image.compress(compressFormat, quality, byteArrayOS);
        return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT);
    }

    // Decode a string into a bitmap
    public static Bitmap decodeBase64(String input) {
        byte[] decodedBytes = Base64.decode(input, 0);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    /**************************
     * Activity Fields (Getters & Setters)
     *******************************/

    private String getUserNameFieldValue() {
        EditText usernameField = ((EditText) findViewById(R.id.username));
        if (usernameField != null) {
            return usernameField.getText().toString();
        } else {
            return "";
        }
    }

    private void setUserNameFieldValue(String value) {
        EditText usernameField = ((EditText) findViewById(R.id.username));
        if (usernameField != null) {
            usernameField.setText(value);
        }
    }

    private String getServerNameFieldValue() {
        EditText serverNameField = ((EditText) findViewById(R.id.servername));
        if (serverNameField != null) {
            return serverNameField.getText().toString();
        } else {
            return "";
        }
    }

    private void setServerNameFieldValue(String value) {
        EditText serverNameField = ((EditText) findViewById(R.id.servername));
        if (serverNameField != null) {
            serverNameField.setText(value);
        }
    }

    private void setPrivateKeyFieldValue(String value) {
        TextView privateKeyField = ((TextView) findViewById(R.id.userPrivateKey));
        if (privateKeyField != null) {
            privateKeyField.setText(value);
        }
    }

    private void setPublicKeyFieldValue(String value) {
        TextView publicKeyField = ((TextView) findViewById(R.id.userPublicKey));
        if (publicKeyField != null) {
            publicKeyField.setText(value);
        }
    }

    private void setUserImageFieldValue(String value) {
        // value must be encoded bitmap string
        ImageView userImageField = ((ImageView) findViewById(R.id.userImage));
        if (userImageField != null) {
            userImageField.setBackgroundResource(0);
            userImageField.setImageBitmap(decodeBase64(value));
        }
    }

    private void toggleEdit(Boolean loggedIn){
        EditText usernameField = ((EditText) findViewById(R.id.username));
        EditText serverNameField = ((EditText) findViewById(R.id.servername));
        Button registerBtn = ((Button) findViewById(R.id.register));
        Button forceLogoutBtn = ((Button) findViewById(R.id.forceLogout));

        if(null != usernameField && null != serverNameField && null != registerBtn && null != forceLogoutBtn) {
            if (!loggedIn) {
                usernameField.setEnabled(true);
                serverNameField.setEnabled(true);
                registerBtn.setVisibility(View.VISIBLE);
                forceLogoutBtn.setVisibility(View.GONE);
            } else {
                usernameField.setEnabled(false);
                serverNameField.setEnabled(false);
                registerBtn.setVisibility(View.GONE);
                forceLogoutBtn.setVisibility(View.VISIBLE);
            }
        }
    }
}
