package com.csandroid.myfirstapp.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.csandroid.myfirstapp.R;
import com.csandroid.myfirstapp.db.MessageDBHandler;
import com.csandroid.myfirstapp.models.Message;

public class ReadMessageActivity extends AppCompatActivity {
    private int messageId;
    private CountDownTimer ttlTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Read");

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(null != this.ttlTimer){
            this.ttlTimer.cancel();
        }
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        // Verify message still exists
        MessageDBHandler db = new MessageDBHandler(this);
        if(null == db.getMessage(this.messageId) || null == db.getMessage(this.messageId).getSenderUsername()){
            //Message has been deleted, route back to list
            Intent mainIntent = new Intent(ReadMessageActivity.this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Toast.makeText(ReadMessageActivity.this.getApplicationContext(),
                    "Message Expired!",
                    Toast.LENGTH_LONG).show();
            startActivity(mainIntent);
        } else{
            if(this.ttlTimer != null){
                this.ttlTimer.cancel();
                this.ttlTimer = null;
            }
            this.populateFields();
        }
    }

    @Override
    public void onBackPressed()
    {
        if(null != this.ttlTimer){
            this.ttlTimer.cancel();
        }
        Intent mainIntent = new Intent(ReadMessageActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainIntent);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if(null != this.ttlTimer){
            this.ttlTimer.cancel();
        }
    }

    private void initOnClickListeners(){
        Button replyBtn = (Button) findViewById(R.id.button);
        ImageButton deleteBtn = (ImageButton) findViewById(R.id.imageButton4);

        if(null != replyBtn && null != deleteBtn){
            replyBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != ttlTimer){
                        ttlTimer.cancel();
                    }
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
                    if(null != ttlTimer){
                        ttlTimer.cancel();
                    }
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
            this.setupTTLTimer(msgTTL);
        }
        if(null != msgBody) {
            msgBody.setText(message.getMessageBody());
        }
    }

    private void setupTTLTimer(final TextView msgTTLField){
        final MessageDBHandler db = new MessageDBHandler(this);
        final Message msg = db.getMessage(this.messageId);

        // Get the current time
        final int currTime = (int) (System.currentTimeMillis() / 1000L);
        final int createdAt = msg.getCreatedAt();
        // Determine remaining ttl
        final int ttl = msg.getTTL();

        // Check if message should be deleted or shown
        boolean stillAlive = (createdAt + ttl) > currTime;

        if(stillAlive) {
            // Need this check, don't want to setup multiple timers!
            if(null == this.ttlTimer) {
                final int ttlInMillis = ((createdAt + ttl) - currTime) * 1000;

                this.ttlTimer = new CountDownTimer(ttlInMillis, 1000) {
                    public void onTick(long millisUntilFinished) {
                        String tickText = "TTL: " + millisUntilFinished / 1000 + " sec";
                        msgTTLField.setText(tickText);
                    }

                    public void onFinish() {
                        Log.d("Zach:", "Timer Finished!");
                        // Message has expired, delete it locally and send user back to list
                        Intent mainIntent = new Intent(ReadMessageActivity.this, MainActivity.class);
                        db.deleteMessage(msg);
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        Toast.makeText(ReadMessageActivity.this.getApplicationContext(),
                                "Message Expired!",
                                Toast.LENGTH_LONG).show();
                        startActivity(mainIntent);
                    }
                }.start();
            }
        } else{
            // Cancel timer
            if(null != this.ttlTimer) {
                this.ttlTimer.cancel();
            }
            // Message has expired, delete it locally and send user back to list
            Intent mainIntent = new Intent(ReadMessageActivity.this, MainActivity.class);
            db.deleteMessage(msg);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Toast.makeText(ReadMessageActivity.this.getApplicationContext(),
                    "Message Expired!",
                    Toast.LENGTH_LONG).show();
            startActivity(mainIntent);
        }

        // Start progress bar animation for timer
        long timeLeft = (msg.getCreatedAt() + msg.getTTL()) - (System.currentTimeMillis()/1000L);
        if(timeLeft < 0){
            timeLeft = 0;
        }
        startAnimation(timeLeft, msg.getTTL());
    }

    public void startAnimation(long timeLeft, long originalTTL){
        // Out of 1000, figure out where we should start
        long scaledStart = 0;
        scaledStart = timeLeft * (1000/originalTTL);

        ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.pb_loading);
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(mProgressBar, "progress", (int) scaledStart, 0);
        progressAnimator.setDuration(timeLeft * 1000);
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.start();
    }
}
