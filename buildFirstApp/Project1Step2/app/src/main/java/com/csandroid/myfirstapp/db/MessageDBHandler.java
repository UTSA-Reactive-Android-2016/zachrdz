package com.csandroid.myfirstapp.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.csandroid.myfirstapp.models.Message;

import java.util.ArrayList;
import java.util.List;

public class MessageDBHandler extends SQLiteOpenHelper{
    // Database Version
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "reactiveAppMessages";
    // Contacts table name
    private static final String TABLE_MESSAGES = "messages";
    // Shops Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_SENDER_USERNAME = "sender_username";
    private static final String KEY_SUBJECT = "subject";
    private static final String KEY_MESSAGE_BODY = "message_body";
    private static final String KEY_TTL = "ttl";

    public MessageDBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_SENDER_USERNAME + " TEXT,"
                + KEY_SUBJECT + " TEXT," + KEY_MESSAGE_BODY + " TEXT," + KEY_TTL + " INTEGER" + ")";
        db.execSQL(CREATE_MESSAGES_TABLE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        // Creating tables again
        onCreate(db);
    }

    // Adding new message
    public void addMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SENDER_USERNAME, message.getSenderUsername());
        values.put(KEY_SUBJECT, message.getSubject());
        values.put(KEY_MESSAGE_BODY, message.getMessageBody());
        values.put(KEY_TTL, message.getTTL());

        // Inserting Row
        db.insert(TABLE_MESSAGES, null, values);
        db.close(); // Closing database connection
    }

    // Getting one message
    public Message getMessage(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MESSAGES, new String[] { KEY_ID,
                        KEY_SENDER_USERNAME, KEY_SUBJECT, KEY_MESSAGE_BODY, KEY_TTL },
                        KEY_ID + "=?", new String[] { String.valueOf(id) }, null, null, null, null);

        Message message = new Message();
        if (cursor != null) {
            cursor.moveToFirst();
            message = new Message(Integer.parseInt(cursor.getString(0)),
                    cursor.getString(1), cursor.getString(2), cursor.getString(3),
                    Integer.parseInt(cursor.getString(4)));

            cursor.close();
        }
        // return message
        return message;
    }

    // Getting All Messages
    public List<Message> getAllMessages() {
        List<Message> messageList = new ArrayList<>();
        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Message message = new Message();
                message.setId(Integer.parseInt(cursor.getString(0)));
                message.setSenderUsername(cursor.getString(1));
                message.setSubject(cursor.getString(2));
                message.setMessageBody(cursor.getString(3));
                message.setTTL(Integer.parseInt(cursor.getString(4)));
                // Adding message to list
                messageList.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        // return message list
        return messageList;
    }

    // Getting messages Count
    public int getMessagesCount() {
        String countQuery = "SELECT * FROM " + TABLE_MESSAGES;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();
        // return count
        return cursor.getCount();
    }

    // Updating a message
    public int updateMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SENDER_USERNAME, message.getSenderUsername());
        values.put(KEY_SUBJECT, message.getSubject());
        values.put(KEY_MESSAGE_BODY, message.getMessageBody());
        values.put(KEY_TTL, message.getTTL());

        // updating row
        return db.update(TABLE_MESSAGES, values, KEY_ID + " = ?",
                new String[]{String.valueOf(message.getId())});
    }

    // Deleting a message
    public void deleteMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGES, KEY_ID + " = ?",
                new String[] { String.valueOf(message.getId()) });
        db.close();
    }

}
