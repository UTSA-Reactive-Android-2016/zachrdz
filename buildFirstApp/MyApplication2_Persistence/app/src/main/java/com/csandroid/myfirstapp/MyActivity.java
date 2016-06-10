package com.csandroid.myfirstapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MyActivity extends AppCompatActivity {

    public final static String EXTRA_MESSAGE = "com.mycompany.myfirstapp.MESSAGE";
    private boolean sentMessage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String savedMessage = preferences.getString("usersMessage", "");
        EditText editText = (EditText) findViewById(R.id.edit_message);

        // If there is a saved message, repopulate the field. Could be empty too.
        editText.setText(savedMessage);
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first

        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();

        //Save message for later (cleared out once sent)
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();

        if(this.sentMessage){
            editor.putString("usersMessage", "");
        }else {
            editor.putString("usersMessage", message);
        }
        editor.commit();
    }

    /** Called when the user clicks the Send button */
    public void sendMessage(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);

        //Clear message from shared preferences, since it was sent successfully
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("usersMessage","");
        editor.commit();

        this.sentMessage = true;

        startActivity(intent);
    }
}
