package com.csandroid.myfirstapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.csandroid.myfirstapp.R;
import com.csandroid.myfirstapp.ReadMessageActivity;
import com.csandroid.myfirstapp.db.MessageDBHandler;
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
        messageViewHolder.vTTL.setText(String.valueOf(messageViewHolder.message.getTTL()));

        this.initOnClickListeners(messageViewHolder);
        this.setupCountdownTimer(messageViewHolder);
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
        protected CountDownTimer countDownTimer;

        public MessageViewHolder(View v) {
            super(v);

            vSender = (TextView) v.findViewById(R.id.sender);
            vSubject = (TextView) v.findViewById(R.id.subject);
            vTTL = (TextView) v.findViewById(R.id.ttl);
            view = v;
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

    private void setupCountdownTimer(final MessageViewHolder mvh){

        // Get the current time
        final int currTime = (int) (System.currentTimeMillis() / 1000L);
        final int createdAt = mvh.message.getCreatedAt();
        // Determine remaining ttl
        final int ttl = mvh.message.getTTL();

        // Check if message should be deleted or shown
        boolean stillAlive = (createdAt + ttl) > currTime;

        if(stillAlive) {
            if(null == mvh.countDownTimer) {
                final int ttlInMillis = ((createdAt + ttl) - currTime) * 1000;

                mvh.countDownTimer = new CountDownTimer(ttlInMillis, 1000) {
                    public void onTick(long millisUntilFinished) {
                        String tickText = "TTL: " + millisUntilFinished / 1000 + " sec";
                        mvh.vTTL.setText(tickText);
                    }

                    public void onFinish() {
                        this.cancel();
                        deleteMessageAndNotify(mvh);
                    }
                }.start();
            }
        } else{
            if(null != mvh.countDownTimer){
                mvh.countDownTimer.cancel();
            }
            deleteMessageAndNotify(mvh);
        }
    }

    private void deleteMessageAndNotify(MessageViewHolder mvh){
        if(!onBind && mvh.getAdapterPosition() > -1) {
            // Delete message and notify adapter, will reload list.
            MessageDBHandler db = new MessageDBHandler(mvh.view.getContext());
            db.deleteMessage(mvh.message);
            //int index = messageList.indexOf(mvh.message);

            messageList.remove(mvh.getAdapterPosition());
            this.notifyItemRemoved(mvh.getAdapterPosition());
        }
    }
}
