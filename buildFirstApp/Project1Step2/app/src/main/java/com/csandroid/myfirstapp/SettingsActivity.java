package com.csandroid.myfirstapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.csandroid.myfirstapp.db.LocalKeyPairDBHandler;
import com.csandroid.myfirstapp.models.LocalKeyPair;

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

        LocalKeyPairDBHandler db = new LocalKeyPairDBHandler(this);
        LocalKeyPair kp = db.getKeyPair();

        if(null != userPublicKey && null != userPrivateKey) {
            userPublicKey.setText(kp.getPublicKey());
            userPrivateKey.setText(kp.getPrivateKey());
        }
    }
}
