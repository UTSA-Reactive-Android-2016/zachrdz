package com.csandroid.myfirstapp;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Administrator on 6/23/2016.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<MessageInfo> messageList;

    public MessageAdapter(List<MessageInfo> messageList) {
        this.messageList = messageList;
    }


    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public void onBindViewHolder(MessageViewHolder messageViewHolder, int i) {
        MessageInfo ci = messageList.get(i);

        messageViewHolder.vSender.setText(ci.sender);
        messageViewHolder.vSubject.setText(ci.subject);
        messageViewHolder.vTTL.setText(ci.ttl);

        messageViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, ReadMessageActivity.class);
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
