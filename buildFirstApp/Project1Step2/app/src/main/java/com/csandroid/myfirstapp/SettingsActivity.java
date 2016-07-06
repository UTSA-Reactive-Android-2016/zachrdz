package com.csandroid.myfirstapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.csandroid.myfirstapp.db.LocalKeyPairDBHandler;
import com.csandroid.myfirstapp.models.LocalKeyPair;
import com.squareup.picasso.Picasso;

public class SettingsActivity extends AppCompatActivity {

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
        LocalKeyPair kp = db.getKeyPair();

        if(null != userPublicKey && null != userPrivateKey && null != userImage && null != username) {
            String coolDog = "https://pbs.twimg.com/profile_images/565602752152076288/NxWEBoTo.jpeg";

            username.setText("zachrdz");
            userPublicKey.setText(kp.getPublicKey());
            userPrivateKey.setText(kp.getPrivateKey());
            Picasso.with(getApplicationContext()).load(coolDog).into(userImage);
        }
    }
}
