package com.csandroid.myfirstapp.stages;

import com.csandroid.myfirstapp.utils.WebHelper;

import org.json.JSONObject;

import rx.Observable;
import rx.functions.Func1;

/**
 * Created by kbaldor on 7/28/16.
 */
public class RemoveContactStage implements Func1<String, Observable<String>> {

    final String server;
    final String username;
    final String contact;


    public RemoveContactStage(String server, String username, String contact){
        this.server = server;
        this.username = username;
        this.contact = contact;
    }

    @Override
    public Observable<String> call(String challenge_response)  {
        try {
            JSONObject json = new JSONObject();
            json.put("username",username);
            json.put("friend",contact);
            JSONObject response = WebHelper.JSONPut(server+"/remove-friend",json);
            System.out.println("ZACHLOG: remove contact attempt: " + response.toString());

            return Observable.just(response.getString("status"));
        } catch (Exception e) {
            e.printStackTrace();
            return Observable.error(e);
        }
    }
}
