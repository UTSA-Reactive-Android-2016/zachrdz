package com.csandroid.myfirstapp;

import android.content.Intent;
import android.os.Bundle;
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

import com.csandroid.myfirstapp.api.ServerAPI;
import com.csandroid.myfirstapp.db.LocalKeyPairDBHandler;
import com.csandroid.myfirstapp.models.LocalKeyPair;
import com.csandroid.myfirstapp.utils.Crypto;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {

    ServerAPI serverAPI;
    LocalKeyPair lkp;
    Crypto myCrypto;

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

        ServerAPI.getInstance()
        this.populateFields();

        // Setup on click listeners
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

        if(null != registerBtn) {
            registerBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doRegister(v);
                }
            });
        }
    }

    public void doRegister(View view) {
        serverAPI.setServerName(getServerName());

        InputStream is;
        byte[] buffer = new byte[0];
        try {
            is = getAssets().open("https://pbs.twimg.com/profile_images/565602752152076288/NxWEBoTo.jpeg");
            buffer = new byte[is.available()];
            is.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String username = ((EditText)findViewById(R.id.username)).getText().toString();
        serverAPI.register(username, Base64.encodeToString(buffer,Base64.DEFAULT).trim(), lkp.getPublicKey());
    }

    public void doLogin(View view) {
        serverAPI.setServerName(getServerName());

        serverAPI.login(getUserName(),myCrypto);
    }

    public void doLogout(View view) {
        serverAPI.setServerName(getServerName());

        serverAPI.logout(getUserName(),myCrypto);
    }
}
