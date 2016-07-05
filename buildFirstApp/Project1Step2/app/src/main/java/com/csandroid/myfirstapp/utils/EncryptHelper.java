package com.csandroid.myfirstapp.utils;

import android.util.Log;
import android.util.Base64;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class EncryptHelper {

    private static String DEBUG = "EncryptHelper";
    private int decodeFlags = Base64.NO_WRAP | Base64.URL_SAFE;

    static {
        Security.addProvider(new BouncyCastleProvider());
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(),1);
    }

    public KeyPair generateKeyPair(){
        KeyPair myKeyPair = null;

        try {
            SecureRandom random = new SecureRandom();

            RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4);
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "SC");
            generator.initialize(spec, random);

            myKeyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException|NoSuchProviderException|InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        return myKeyPair;
    }

    public String getPublicKeyString(KeyPair keyPair){
        return Base64.encodeToString(keyPair.getPublic().getEncoded(),Base64.DEFAULT);
    }

    public String getPrivateKeyString(KeyPair keyPair){
        return Base64.encodeToString(keyPair.getPrivate().getEncoded(),Base64.DEFAULT);
    }

    public PublicKey getPublicKeyFromString(String publicKey){
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "SC");
            byte[] publicKeyBytes = Base64.decode(publicKey.getBytes("UTF-8"), Base64.DEFAULT);
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyBytes);
            return keyFactory.generatePublic(x509KeySpec);
        } catch(UnsupportedEncodingException|InvalidKeySpecException|
                NoSuchAlgorithmException|NoSuchProviderException e){
            Log.d(DEBUG, "Public Key StringToKey Conversion: " + e.getMessage());
        }

        return null;
    }

    public PrivateKey getPrivateKeyFromString(String privateKey){
        try {
            byte[] keyBytes = Base64.decode(privateKey.getBytes("utf-8"),decodeFlags);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            PrivateKey key = fact.generatePrivate(keySpec);

            return key;
        } catch(UnsupportedEncodingException|InvalidKeySpecException|NoSuchAlgorithmException e){
            Log.d(DEBUG, "Private Key StringToKey Conversion: " + e.getMessage());
        }

        return null;
    }

    public String encryptTextWithPublic(String clearText, PublicKey publicKey){
        try {
            Cipher aesCipher = Cipher.getInstance("RSA","SC");
            aesCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] bytes = aesCipher.doFinal(clearText.getBytes());
            return Base64.encodeToString(bytes,Base64.DEFAULT);
        } catch (NoSuchAlgorithmException|NoSuchProviderException|NoSuchPaddingException|
                InvalidKeyException|BadPaddingException|IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String encryptTextWithPrivate(String clearText, PrivateKey privateKey){
        try {
            Cipher aesCipher = Cipher.getInstance("RSA","SC");
            aesCipher.init(Cipher.ENCRYPT_MODE, privateKey);
            byte[] bytes = aesCipher.doFinal(clearText.getBytes());
            return Base64.encodeToString(bytes,Base64.DEFAULT);
        } catch (NoSuchAlgorithmException|NoSuchProviderException|NoSuchPaddingException|
                InvalidKeyException|BadPaddingException|IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String decryptTextWithPublic(String cipherText, PublicKey publicKey){
        try {
            byte[] bytes = Base64.decode(cipherText,decodeFlags);
            Cipher aesCipher = Cipher.getInstance("RSA","SC");
            aesCipher.init(Cipher.DECRYPT_MODE, publicKey);
            bytes = aesCipher.doFinal(bytes);

            return new String(bytes,"UTF-8");
        } catch (NoSuchAlgorithmException|NoSuchProviderException|NoSuchPaddingException|
                InvalidKeyException|BadPaddingException|IllegalBlockSizeException|
                UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            Log.d(DEBUG,"Unable to Decrypt text, Public: ",e);
        }
        return null;
    }

    public String decryptTextWithPrivate(String cipherText, PublicKey privateKey){
        try {
            byte[] bytes = Base64.decode(cipherText,decodeFlags);
            Cipher aesCipher = Cipher.getInstance("AES","SC");
            aesCipher.init(Cipher.DECRYPT_MODE, privateKey);
            bytes = aesCipher.doFinal(bytes);

            return new String(bytes,"UTF-8");
        } catch (NoSuchAlgorithmException|NoSuchProviderException|NoSuchPaddingException|
                InvalidKeyException|BadPaddingException|IllegalBlockSizeException|
                UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            Log.d(DEBUG,"Unable to Decrypt text, Private: ",e);
        }
        return null;
    }
}
