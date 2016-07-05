package com.csandroid.myfirstapp.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.csandroid.myfirstapp.models.LocalKeyPair;


public class LocalKeyPairDBHandler extends SQLiteOpenHelper{
    // Database Version
    private static final int DATABASE_VERSION = 3;
    // Database Name
    private static final String DATABASE_NAME = "reactiveAppKeyPair";
    // Table name
    private static final String TABLE_KEY_PAIR = "keyPair";
    // Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_PRIVATE_KEY = "private_key";
    private static final String KEY_PUBLIC_KEY = "public_key";

    public LocalKeyPairDBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_KEY_PAIR_TABLE = "CREATE TABLE " + TABLE_KEY_PAIR + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_PRIVATE_KEY + " TEXT," +
                KEY_PUBLIC_KEY + " TEXT" + ")";
        db.execSQL(CREATE_KEY_PAIR_TABLE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_KEY_PAIR);
        // Creating tables again
        onCreate(db);
    }

    // Adding new keypair
    public void addKeyPair(LocalKeyPair kp) {
        // Check to make sure a key pair doesn't already exist.
        // Only want one to exist in this table, ever.
        if(this.getKeyPairCount() < 1) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(KEY_PRIVATE_KEY, kp.getPrivateKey());
            values.put(KEY_PUBLIC_KEY, kp.getPublicKey());

            // Inserting Row
            db.insert(TABLE_KEY_PAIR, null, values);
            db.close(); // Closing database connection
        }
    }

    // Getting one keypair, should only be one in database, so no params needed
    public LocalKeyPair getKeyPair() {
        String selectQuery = "SELECT * FROM " + TABLE_KEY_PAIR +" LIMIT 1";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        LocalKeyPair kp = new LocalKeyPair();

        // looping through all rows and adding to list
        // should only be one row, but still need this
        if (cursor.moveToFirst()) {
            kp.setId(Integer.parseInt(cursor.getString(0)));
            kp.setPrivateKey(cursor.getString(1));
            kp.setPublicKey(cursor.getString(2));
        }

        cursor.close();
        db.close(); // Closing database connection
        return kp;
    }

    // Getting local key pair count, should only be one,
    // so use this function to check if local user already has one generated
    public int getKeyPairCount() {
        String countQuery = "SELECT * FROM " + TABLE_KEY_PAIR;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();

        db.close(); // Closing database connection

        // return count
        return count;
    }

    // Updating key pair
    public int updateKeyPair(LocalKeyPair kp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PRIVATE_KEY, kp.getPrivateKey());
        values.put(KEY_PUBLIC_KEY, kp.getPublicKey());

        // updating row
        int result = db.update(TABLE_KEY_PAIR, values, KEY_ID + " = ?",
                new String[]{String.valueOf(kp.getId())});

        db.close(); // Closing database connection

        // updating row
        return result;
    }

    // Deleting key pair
    public void deleteKeyPair(LocalKeyPair kp) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_KEY_PAIR, KEY_ID + " = ?",
                new String[] { String.valueOf(kp.getId()) });
        db.close();
    }

}
