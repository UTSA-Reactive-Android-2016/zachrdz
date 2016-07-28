package com.csandroid.myfirstapp.models;

public class LocalKeyPair {
    private int id;
    private String username;
    private String publicKey;
    private String privateKey;

    public LocalKeyPair(){}

    public LocalKeyPair(int id, String username, String pubKey, String privKey){
        this.id = id;
        this.username = username;
        this.publicKey = pubKey;
        this.privateKey = privKey;
    }

    public LocalKeyPair(String username, String pubKey, String privKey){
        this.username = username;
        this.publicKey = pubKey;
        this.privateKey = privKey;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
