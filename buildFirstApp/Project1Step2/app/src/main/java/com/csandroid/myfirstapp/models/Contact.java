package com.csandroid.myfirstapp.models;

/**
 * Created by Zach on 6/29/2016.
 */
public class Contact {
    private int id;
    private String username;
    private String userImage;
    private String publicKey;

    public Contact(){

    }

    public Contact(int id, String username, String userImage, String publicKey){
        this.id = id;
        this.username = username;
        this.userImage = userImage;
        this.publicKey = publicKey;
    }

    public Contact(String username, String userImage, String publicKey){
        this.username = username;
        this.userImage = userImage;
        this.publicKey = publicKey;
    }


    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserImage() {
        return userImage;
    }

    public void setUserImage(String userImage) {
        this.userImage = userImage;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
