package com.csandroid.myfirstapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.csandroid.myfirstapp.db.MessageDBHandler;
import com.csandroid.myfirstapp.models.Message;

import org.w3c.dom.Text;

public class ReadMessageActivity extends AppCompatActivity {
    private int messageId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button replyBtn = (Button) findViewById(R.id.button);
        ImageButton deleteBtn = (ImageButton) findViewById(R.id.imageButton4);


        replyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent composeIntent = new Intent(ReadMessageActivity.this, ComposeActivity.class);
                //Create the bundle
                Bundle bundle = new Bundle();
                MessageDBHandler db = new MessageDBHandler(ReadMessageActivity.this);
                Message message = db.getMessage(ReadMessageActivity.this.messageId);
                //Add your data to bundle
                bundle.putString("reply_to_username", message.getSenderUsername());

                //Add the bundle to the intent
                composeIntent.putExtras(bundle);
                startActivity(composeIntent);
            }
        });

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainIntent = new Intent(ReadMessageActivity.this, MainActivity.class);
                MessageDBHandler db = new MessageDBHandler(v.getContext());
                Message msg = db.getMessage(ReadMessageActivity.this.messageId);
                db.deleteMessage(msg);
                Toast.makeText(v.getContext(), "Deleted message from: " + msg.getSenderUsername(),
                        Toast.LENGTH_LONG).show();
                startActivity(mainIntent);
            }
        });


        //Get the bundle
        Bundle bundle = getIntent().getExtras();

        //Extract the data…
        String messageId = bundle.getString("message_id");
        this.messageId = Integer.parseInt(messageId);

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
        TextView msgSender = (TextView)findViewById(R.id.msg_sender);
        TextView msgSubject = (TextView)findViewById(R.id.msg_subject);
        TextView msgTTL = (TextView)findViewById(R.id.msg_ttl);
        TextView msgBody = (TextView)findViewById(R.id.msg_body);

        MessageDBHandler db = new MessageDBHandler(this);

        Message message = db.getMessage(this.messageId);

        msgSender.setText(message.getSenderUsername());
        msgSubject.setText(message.getSubject());
        msgTTL.setText(Integer.toString(message.getTTL()));
        msgBody.setText(message.getMessageBody());
    }

}
