package com.csandroid.myfirstapp.models;

public class Contact {
    private int id;
    private int localKeyPairId;
    private String username;
    private String userImage;
    private String publicKey;

    public Contact(){

    }

    public Contact(int id, int localKeyPairId, String username, String userImage, String publicKey){
        this.id = id;
        this.localKeyPairId = localKeyPairId;
        this.username = username;
        this.userImage = userImage;
        this.publicKey = publicKey;
    }

    public Contact(int localKeyPairId, String username, String userImage, String publicKey){
        this.localKeyPairId = localKeyPairId;
        this.username = username;
        this.userImage = userImage;
        this.publicKey = publicKey;
    }

    public int getLocalKeyPairId() {
        return localKeyPairId;
    }

    public void setLocalKeyPairId(int localKeyPairId) {
        this.localKeyPairId = localKeyPairId;
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
