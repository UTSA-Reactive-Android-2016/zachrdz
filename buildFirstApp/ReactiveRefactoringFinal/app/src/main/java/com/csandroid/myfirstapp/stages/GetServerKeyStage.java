package com.csandroid.myfirstapp.stages;

import android.util.Log;

import com.csandroid.myfirstapp.utils.Crypto;
import com.csandroid.myfirstapp.utils.WebHelper;

import java.security.PublicKey;

import rx.Observable;
import rx.functions.Func1;

/**
 * Created by kbaldor on 7/28/16.
 */
public class GetServerKeyStage implements Func1<Integer, Observable<PublicKey>> {

    final String server;

    public GetServerKeyStage(String server) {
        this.server = server;
    }

    @Override
    public Observable<PublicKey> call(Integer unused)  {
        try {
            String response = WebHelper.StringGet(this.server+"/get-key");
            Log.d("GetServerKeyStage",response);
            return Observable.just(Crypto.getPublicKeyFromString(response));
        } catch (Exception e) {
            e.printStackTrace();
            return Observable.error(e);
        }
    }
}

