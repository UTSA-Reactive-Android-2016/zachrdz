package com.csandroid.myfirstapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.csandroid.myfirstapp.models.Message;

import java.util.List;

/**
 * Created by Administrator on 6/23/2016.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messageList;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }


    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public void onBindViewHolder(MessageViewHolder messageViewHolder, int i) {
        final Message ci = messageList.get(i);

        messageViewHolder.vSender.setText(ci.getSenderUsername());
        messageViewHolder.vSubject.setText(ci.getSubject());
        messageViewHolder.vTTL.setText(Integer.toString(ci.getTTL()));

        messageViewHolder.itemView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Context context = v.getContext();
                Intent intent = new Intent(context, ReadMessageActivity.class);

                //Create the bundle
                Bundle bundle = new Bundle();

                //Add your data to bundle
                bundle.putString("message_id", Integer.toString(ci.getId()));

                //Add the bundle to the intent
                intent.putExtras(bundle);

                context.startActivity(intent);
            }
        });
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

        public MessageViewHolder(View v) {
            super(v);

            vSender = (TextView) v.findViewById(R.id.sender);
            vSubject = (TextView) v.findViewById(R.id.subject);
            vTTL = (TextView) v.findViewById(R.id.ttl);
        }

    }

}
