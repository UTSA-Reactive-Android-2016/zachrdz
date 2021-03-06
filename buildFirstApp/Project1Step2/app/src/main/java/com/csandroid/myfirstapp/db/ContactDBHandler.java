package com.csandroid.myfirstapp.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.csandroid.myfirstapp.models.Contact;

import java.util.ArrayList;
import java.util.List;

public class ContactDBHandler extends SQLiteOpenHelper{
    // Database Version
    private static final int DATABASE_VERSION = 3;
    // Database Name
    private static final String DATABASE_NAME = "reactiveAppContacts";
    // Table name
    private static final String TABLE_CONTACTS = "contacts";
    // Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_IMAGE = "user_image";
    private static final String KEY_PUBLIC_KEY = "public_key";

    public ContactDBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_CONTACTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_USERNAME + " TEXT,"
                + KEY_USER_IMAGE + " TEXT," + KEY_PUBLIC_KEY + " TEXT" + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
        // Creating tables again
        onCreate(db);
    }

    // Adding new contact
    public int addContact(Contact contact) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME, contact.getUsername());
        values.put(KEY_USER_IMAGE, contact.getUserImage());
        values.put(KEY_PUBLIC_KEY, contact.getPublicKey());

        // Inserting Row
        long id = db.insert(TABLE_CONTACTS, null, values);
        db.close(); // Closing database connection
        return (int) id;
    }

    // Getting one contact
    public Contact getContact(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CONTACTS, new String[] { KEY_ID,
                        KEY_USERNAME, KEY_USER_IMAGE, KEY_PUBLIC_KEY},
                KEY_ID + "=?", new String[] { String.valueOf(id) }, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        Contact contact = new Contact();

        if(null != cursor && cursor.getCount() > 0) {
            contact = new Contact(Integer.parseInt(cursor.getString(0)),
                    cursor.getString(1), cursor.getString(2), cursor.getString(3));

            cursor.close();
        }
        db.close(); // Closing database connection

        // return contact
        return contact;
    }

    // Getting one contact by username
    public Contact getContactByUsername(String username) {
        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + KEY_USERNAME + "='" +username.trim()+"'";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        Contact contact = new Contact();

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            contact.setId(Integer.parseInt(cursor.getString(0)));
            contact.setUsername(cursor.getString(1));
            contact.setUserImage(cursor.getString(2));
            contact.setPublicKey(cursor.getString(3));
        }

        cursor.close();
        db.close(); // Closing database connection
        return contact;
    }

    // Getting All Contacts
    public List<Contact> getAllContacts() {
        List<Contact> contactList = new ArrayList<>();
        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Contact contact = new Contact();
                contact.setId(Integer.parseInt(cursor.getString(0)));
                contact.setUsername(cursor.getString(1));
                contact.setUserImage(cursor.getString(2));
                contact.setPublicKey(cursor.getString(3));

                // Adding contact to list
                contactList.add(contact);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close(); // Closing database connection

        // return contact list
        return contactList;
    }

    // Getting contacts Count
    public int getContactsCount() {
        String countQuery = "SELECT * FROM " + TABLE_CONTACTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();

        db.close(); // Closing database connection

        // return count
        return count;
    }

    // Updating a contact
    public int updateContact(Contact contact) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME, contact.getUsername());
        values.put(KEY_USER_IMAGE, contact.getUserImage());
        values.put(KEY_PUBLIC_KEY, contact.getPublicKey());

        // updating row
        int result = db.update(TABLE_CONTACTS, values, KEY_ID + " = ?",
                new String[]{String.valueOf(contact.getId())});

        db.close(); // Closing database connection

        // updating row
        return result;
    }

    // Deleting a contact
    public void deleteContact(Contact contact) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CONTACTS, KEY_ID + " = ?",
                new String[] { String.valueOf(contact.getId()) });
        db.close();
    }

}
