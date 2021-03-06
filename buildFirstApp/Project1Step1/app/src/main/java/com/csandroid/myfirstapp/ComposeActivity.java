package com.csandroid.myfirstapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class ComposeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        EditText et = (EditText) findViewById(R.id.editText);
        Button sendBtn = (Button) findViewById(R.id.button);
        ImageButton deleteBtn = (ImageButton) findViewById(R.id.imageButton4);
        ImageButton ttlBtn = (ImageButton) findViewById(R.id.imageButton5);


        et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent composeIntent = new Intent(ComposeActivity.this, ContactsActivity.class);
                startActivity(composeIntent);
            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent composeIntent = new Intent(ComposeActivity.this, MainActivity.class);
                startActivity(composeIntent);
            }
        });

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent composeIntent = new Intent(ComposeActivity.this, MainActivity.class);
                startActivity(composeIntent);
            }
        });

        ttlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence colors[] = new CharSequence[] {"5s", "15s", "60s"};

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Pick a TTL for this message:");
                builder.setItems(colors, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // the user clicked on colors[which]
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        });
    }

}
