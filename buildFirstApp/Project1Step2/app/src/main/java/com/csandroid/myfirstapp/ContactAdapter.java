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

import com.csandroid.myfirstapp.db.MessageDBHandler;
import com.csandroid.myfirstapp.models.Contact;
import com.csandroid.myfirstapp.models.Message;

import java.util.List;

/**
 * Created by Administrator on 6/23/2016.
 */
public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder>{

    private List<Contact> contactList;

    public ContactAdapter(List<Contact> contactList) {
        this.contactList = contactList;
    }


    @Override
    public int getItemCount() {
        return contactList.size();
    }

    @Override
    public void onBindViewHolder(ContactViewHolder contactViewHolder, int i) {
        final Contact ci = contactList.get(i);
        contactViewHolder.vUsername.setText(ci.getUsername());
        contactViewHolder.vUserImage.setText(ci.getUserImage());
        contactViewHolder.vPublicKey.setText(ci.getPublicKey());

        contactViewHolder.itemView.findViewById(R.id.editContactBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, EditContactActivity.class);

                //Create the bundle
                Bundle bundle = new Bundle();

                //Add your data to bundle
                bundle.putString("contact_id", Integer.toString(ci.getId()));

                //Add the bundle to the intent
                intent.putExtras(bundle);

                context.startActivity(intent);
            }
        });

        contactViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, ComposeActivity.class);
                //Create the bundle
                Bundle bundle = new Bundle();

                //Add your data to bundle
                bundle.putString("contact_id", Integer.toString(ci.getId()));

                //Add the bundle to the intent
                intent.putExtras(bundle);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public ContactViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.cards_contact, viewGroup, false);

        return new ContactViewHolder(itemView);
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {

        protected TextView vUsername;
        protected TextView vUserImage;
        protected TextView vPublicKey;

        public ContactViewHolder(View v) {
            super(v);

            vUsername = (TextView) v.findViewById(R.id.username);
            vUserImage = (TextView) v.findViewById(R.id.userImage);
            vPublicKey = (TextView) v.findViewById(R.id.publicKey);
        }
    }

}
