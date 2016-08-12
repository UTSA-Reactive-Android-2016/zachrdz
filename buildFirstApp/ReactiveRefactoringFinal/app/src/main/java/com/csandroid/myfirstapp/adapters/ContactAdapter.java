package com.csandroid.myfirstapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.csandroid.myfirstapp.R;
import com.csandroid.myfirstapp.activities.ComposeActivity;
import com.csandroid.myfirstapp.activities.EditContactActivity;
import com.csandroid.myfirstapp.ext.CircleTransform;
import com.csandroid.myfirstapp.models.Contact;

import java.io.ByteArrayOutputStream;
import java.util.List;

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
        final Contact contact = contactList.get(i);
        CircleTransform ct = new CircleTransform();
        contactViewHolder.vUsername.setText(contact.getUsername());
        contactViewHolder.vUserImage.setImageBitmap(ct.transform(decodeBase64(contact.getUserImage())));
        this.initOnClickListeners(contactViewHolder, contact);
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
        protected ImageView vUserImage;
        protected View view;

        public ContactViewHolder(View v) {
            super(v);

            vUsername = (TextView) v.findViewById(R.id.username);
            vUserImage = (ImageView) v.findViewById(R.id.userImage);
            view = v;
        }
    }

    private void initOnClickListeners(ContactViewHolder contactViewHolder, final Contact contact){
        // Open edit contact activity when list item's gear icon is clicked
        contactViewHolder.itemView.findViewById(R.id.editContactBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, EditContactActivity.class);

                //Create the bundle
                Bundle bundle = new Bundle();

                //Add your data to bundle
                bundle.putString("contact_id", Integer.toString(contact.getId()));

                //Add the bundle to the intent
                intent.putExtras(bundle);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(intent);
            }
        });

        // Open compose activity when list item is clicked
        contactViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, ComposeActivity.class);
                //Create the bundle
                Bundle bundle = new Bundle();

                //Add your data to bundle
                bundle.putString("contact_id", Integer.toString(contact.getId()));

                //Add the bundle to the intent
                intent.putExtras(bundle);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(intent);
            }
        });
    }

    /************************** Bitmap (Encoding & Decoding) **************************************/

    // Encode a bitmap to a String
    public static String encodeToBase64(Bitmap image, Bitmap.CompressFormat compressFormat, int quality)
    {
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        image.compress(compressFormat, quality, byteArrayOS);
        return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT);
    }

    // Decode a string into a bitmap
    public static Bitmap decodeBase64(String input)
    {
        byte[] decodedBytes = Base64.decode(input, 0);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

}
