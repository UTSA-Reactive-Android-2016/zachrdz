package com.csandroid.myfirstapp.models;

public class LocalKeyPair {
    private int id;
    private String publicKey;
    private String privateKey;

    public LocalKeyPair(){}

    public LocalKeyPair(int id, String pubKey, String privKey){
        this.id = id;
        this.publicKey = pubKey;
        this.privateKey = privKey;
    }

    public LocalKeyPair(String pubKey, String privKey){
        this.publicKey = pubKey;
        this.privateKey = privKey;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
