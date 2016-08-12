package com.csandroid.myfirstapp.adapters;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.csandroid.myfirstapp.R;
import com.csandroid.myfirstapp.activities.ReadMessageActivity;
import com.csandroid.myfirstapp.models.Message;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>{

    private List<Message> messageList;
    private boolean onBind;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }


    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder messageViewHolder, int i) {
        this.onBind = true;
        messageViewHolder.message = messageList.get(i);
        messageViewHolder.vSender.setText(messageViewHolder.message.getSenderUsername());
        messageViewHolder.vSubject.setText(messageViewHolder.message.getSubject());
        messageViewHolder.vTTL.setText("TTL: " + String.valueOf(messageViewHolder.message.getTTL() + " sec"));

        this.initOnClickListeners(messageViewHolder);
        long timeLeft = (messageViewHolder.message.getCreatedAt() + messageViewHolder.message.getTTL()) - (System.currentTimeMillis()/1000L);
        if(timeLeft < 0){
            timeLeft = 0;
        }
        messageViewHolder.startAnimation(timeLeft, messageViewHolder.message.getTTL());
        this.onBind = false;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.cards_main, viewGroup, false);

        return new MessageViewHolder(itemView);
    }



    public static class MessageViewHolder extends RecyclerView.ViewHolder {

        protected TextView vSender;
        protected TextView vSubject;
        protected TextView vTTL;
        protected View view;
        protected Message message;

        public MessageViewHolder(View v) {
            super(v);

            vSender = (TextView) v.findViewById(R.id.sender);
            vSubject = (TextView) v.findViewById(R.id.subject);
            vTTL = (TextView) v.findViewById(R.id.ttl);
            view = v;
        }

        public void startAnimation(long timeLeft, long originalTTL){
            // Out of 1000, figure out where we should start
            long scaledStart = 0;
            scaledStart = timeLeft * (1000/originalTTL);

            ProgressBar mProgressBar = (ProgressBar) view.findViewById(R.id.pb_loading);
            ObjectAnimator progressAnimator = ObjectAnimator.ofInt(mProgressBar, "progress", (int) scaledStart, 0);
            progressAnimator.setDuration(timeLeft * 1000);
            progressAnimator.setInterpolator(new LinearInterpolator());
            progressAnimator.start();
        }
    }

    private void initOnClickListeners(final MessageViewHolder mvh){
        mvh.itemView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Context context = v.getContext();
                Intent intent = new Intent(context, ReadMessageActivity.class);

                //Create the bundle
                Bundle bundle = new Bundle();

                //Add your data to bundle
                bundle.putString("message_id", Integer.toString(mvh.message.getId()));

                //Add the bundle to the intent
                intent.putExtras(bundle);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(intent);
            }
        });
    }
}
