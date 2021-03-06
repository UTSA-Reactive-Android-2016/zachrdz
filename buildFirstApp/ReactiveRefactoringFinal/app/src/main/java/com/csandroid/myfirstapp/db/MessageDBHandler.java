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
    private static final int DATABASE_VERSION = 4;
    // Database Name
    private static final String DATABASE_NAME = "reactiveAppMessages";
    // Table name
    private static final String TABLE_MESSAGES = "messages";
    // Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_LOCAL_KEY_PAIR_ID = "local_key_pair_id";
    private static final String KEY_SENDER_USERNAME = "sender_username";
    private static final String KEY_SUBJECT = "subject";
    private static final String KEY_MESSAGE_BODY = "message_body";
    private static final String KEY_TTL = "ttl";
    private static final String KEY_CREATED_AT = "created_at";

    public MessageDBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_LOCAL_KEY_PAIR_ID + " INTEGER,"
                + KEY_SENDER_USERNAME + " TEXT,"
                + KEY_SUBJECT + " TEXT," + KEY_MESSAGE_BODY + " TEXT,"
                + KEY_CREATED_AT + " INTEGER," + KEY_TTL + " INTEGER" + ")";
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
    public int addMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LOCAL_KEY_PAIR_ID, message.getLocalKeyPairId());
        values.put(KEY_SENDER_USERNAME, message.getSenderUsername());
        values.put(KEY_SUBJECT, message.getSubject());
        values.put(KEY_MESSAGE_BODY, message.getMessageBody());
        if(message.getCreatedAt() > 0) {
            values.put(KEY_CREATED_AT, message.getCreatedAt());
        }else{
            values.put(KEY_CREATED_AT, (int) (System.currentTimeMillis() / 1000L));
        }
        values.put(KEY_TTL, message.getTTL());

        // Inserting Row
        long id = db.insert(TABLE_MESSAGES, null, values);
        db.close(); // Closing database connection

        return (int) id;
    }

    // Getting one message
    public Message getMessage(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MESSAGES, new String[] { KEY_ID, KEY_LOCAL_KEY_PAIR_ID,
                        KEY_SENDER_USERNAME, KEY_SUBJECT, KEY_MESSAGE_BODY, KEY_CREATED_AT, KEY_TTL },
                        KEY_ID + "=?", new String[] { String.valueOf(id) }, null, null, null, null);

        Message message = new Message();
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            message = new Message(Integer.parseInt(cursor.getString(0)),
                    Integer.parseInt(cursor.getString(1)),
                    cursor.getString(2), cursor.getString(3), cursor.getString(4),
                    Integer.parseInt(cursor.getString(5)),
                    Integer.parseInt(cursor.getString(6)));

            cursor.close();
        }

        db.close(); // Closing database connection

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
                message.setLocalKeyPairId(Integer.parseInt(cursor.getString(1)));
                message.setSenderUsername(cursor.getString(2));
                message.setSubject(cursor.getString(3));
                message.setMessageBody(cursor.getString(4));
                message.setCreatedAt(Integer.parseInt(cursor.getString(5)));
                message.setTTL(Integer.parseInt(cursor.getString(6)));
                // Adding message to list
                messageList.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close(); // Closing database connection

        // return message list
        return messageList;
    }

    // Getting Messages associated with localKeyPairId
    public List<Message> getMessages(int localKeyPairId) {
        List<Message> messageList = new ArrayList<>();
        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + KEY_LOCAL_KEY_PAIR_ID + "=" +localKeyPairId+"";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Message message = new Message();
                message.setId(Integer.parseInt(cursor.getString(0)));
                message.setLocalKeyPairId(Integer.parseInt(cursor.getString(1)));
                message.setSenderUsername(cursor.getString(2));
                message.setSubject(cursor.getString(3));
                message.setMessageBody(cursor.getString(4));
                message.setCreatedAt(Integer.parseInt(cursor.getString(5)));
                message.setTTL(Integer.parseInt(cursor.getString(6)));
                // Adding message to list
                messageList.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close(); // Closing database connection

        // return message list
        return messageList;
    }

    // Getting messages Count
    public int getMessagesCount() {
        String countQuery = "SELECT * FROM " + TABLE_MESSAGES;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();

        db.close(); // Closing database connection

        // return count
        return count;
    }

    // Updating a message
    public int updateMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LOCAL_KEY_PAIR_ID, message.getLocalKeyPairId());
        values.put(KEY_SENDER_USERNAME, message.getSenderUsername());
        values.put(KEY_SUBJECT, message.getSubject());
        values.put(KEY_MESSAGE_BODY, message.getMessageBody());
        values.put(KEY_TTL, message.getTTL());

        // updating row
        int result = db.update(TABLE_MESSAGES, values, KEY_ID + " = ?",
                new String[]{String.valueOf(message.getId())});

        db.close(); // Closing database connection

        return result;
    }

    // Deleting a message
    public void deleteMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGES, KEY_ID + " = ?",
                new String[] { String.valueOf(message.getId()) });
        db.close();
    }

}
