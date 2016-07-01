package com.csandroid.myfirstapp.utils;

import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.csandroid.myfirstapp.R;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.RSAKeyGenParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by zachary.rodriguez on 6/30/2016.
 */
public class EncryptHelper {

    private static String DEBUG = "EncryptHelper";
    private SecureRandom random = new SecureRandom();

    public SecretKey generateKey() {
        SecretKey AESKey = null;
        try {
            KeyGenerator aesGenerator = KeyGenerator.getInstance("AES", "SC");
            aesGenerator.init(256, random);
            AESKey = aesGenerator.generateKey();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
        }

        return AESKey;
    }

    public KeyPair generateKeyPair(){
        KeyPair myKeyPair = null;

        try {
            SecureRandom random = new SecureRandom();

            RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4);
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "SC");
            generator.initialize(spec, random);

            myKeyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        return myKeyPair;
    }

    public String encryptToBase64(String clearText, SecretKey AESKey){
        try {
            Log.d(DEBUG,"clear text is of length "+clearText.getBytes().length);
//            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding","SC");
//            rsaCipher.init(Cipher.ENCRYPT_MODE, myKeyPair.getPublic());
//            byte[] bytes = rsaCipher.doFinal(clearText.getBytes());

            Cipher aesCipher = Cipher.getInstance("AES","SC");
            aesCipher.init(Cipher.ENCRYPT_MODE, AESKey);
            byte[] bytes = aesCipher.doFinal(clearText.getBytes());
            Log.d(DEBUG,"cipher bytes is of length "+bytes.length);
            Log.d(DEBUG,"");

            return Base64.encodeToString(bytes,Base64.DEFAULT);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String decryptFromBase64(String cipherText, SecretKey AESKey){
        try {
            byte[] bytes = Base64.decode(cipherText,Base64.DEFAULT);
//            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding","SC");
//            rsaCipher.init(Cipher.DECRYPT_MODE, myKeyPair.getPrivate());

            Cipher aesCipher = Cipher.getInstance("AES","SC");
            aesCipher.init(Cipher.DECRYPT_MODE, AESKey);

//            bytes = rsaCipher.doFinal(bytes);
            bytes = aesCipher.doFinal(bytes);

            return new String(bytes,"UTF-8");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            Log.d(DEBUG,"Ouch",e);
        }
        return "";
    }
}
