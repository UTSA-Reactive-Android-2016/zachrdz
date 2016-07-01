package com.csandroid.myfirstapp;

import android.content.Intent;
import android.os.Bundle;
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

public class ReadMessageActivity extends AppCompatActivity {
    private int messageId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(null != getSupportActionBar()){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        this.initOnClickListeners();
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

    private void initOnClickListeners(){
        Button replyBtn = (Button) findViewById(R.id.button);
        ImageButton deleteBtn = (ImageButton) findViewById(R.id.imageButton4);

        if(null != replyBtn && null != deleteBtn){
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
                    composeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(mainIntent);
                }
            });
        }
    }

    public void populateFields(){
        //Get the bundle
        Bundle bundle = getIntent().getExtras();

        if(null != bundle){
            //Extract the data…
            String messageId = bundle.getString("message_id");
            this.messageId = Integer.parseInt(messageId);
        }

        TextView msgSender = (TextView)findViewById(R.id.msg_sender);
        TextView msgSubject = (TextView)findViewById(R.id.msg_subject);
        TextView msgTTL = (TextView)findViewById(R.id.msg_ttl);
        TextView msgBody = (TextView)findViewById(R.id.msg_body);

        MessageDBHandler db = new MessageDBHandler(this);
        Message message = db.getMessage(this.messageId);

        if(null != msgSender){
            msgSender.setText(message.getSenderUsername());
        }
        if(null != msgSubject) {
            msgSubject.setText(message.getSubject());
        }
        if(null != msgTTL) {
            msgTTL.setText(String.valueOf(message.getTTL()));
        }
        if(null != msgBody) {
            msgBody.setText(message.getMessageBody());
        }
    }

}
