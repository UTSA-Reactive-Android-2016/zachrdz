package com.csandroid.myfirstapp.stages;


import com.csandroid.myfirstapp.utils.WebHelper;

import org.json.JSONObject;

import rx.Observable;
import rx.functions.Func1;

public class LogOutStage implements Func1<String, Observable<String>> {

    final String server;
    final String username;


    public LogOutStage(String server, String username){
        this.server = server;
        this.username = username;
    }

    @Override
    public Observable<String> call(String challenge_response)  {
        try {
            JSONObject userDetails = new JSONObject();
            userDetails.put("username",username);
            userDetails.put("response",challenge_response);
            JSONObject response = WebHelper.JSONPut(server+"/logout",userDetails);
            return Observable.just(response.getString("status"));
        } catch (Exception e) {
            e.printStackTrace();
            return Observable.error(e);
        }
    }
}

