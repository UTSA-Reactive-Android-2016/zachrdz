package com.csandroid.myfirstapp.models;

import com.csandroid.myfirstapp.utils.Crypto;

import java.security.PublicKey;

/**
 * Created by Zach on 8/11/2016.
 */
public class UserInfo{
    public final String username;
    public final String image;
    public final PublicKey publicKey;
    public UserInfo(String username, String image, String keyString){
        this.username = username;
        this.image = image;
        this.publicKey = Crypto.getPublicKeyFromString(keyString);
    }
}