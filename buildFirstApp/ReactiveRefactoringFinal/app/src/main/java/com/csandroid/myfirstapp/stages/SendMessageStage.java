package com.csandroid.myfirstapp.stages;

/**
 * Created by zachrdz on 8/11/16.
 */

import com.csandroid.myfirstapp.utils.WebHelper;

import org.json.JSONObject;

import rx.Observable;
import rx.functions.Func1;

/**
 * Created by kbaldor on 7/28/16.
 */
public class SendMessageStage implements Func1<String, Observable<String>> {

    final String server;
    final String recipient;
    final JSONObject message;


    public SendMessageStage(String server, String recipient, JSONObject message){
        this.message = message;
        this.server = server;
        this.recipient = recipient;
    }

    @Override
    public Observable<String> call(String challenge_response)  {
        try {
            JSONObject response = WebHelper.JSONPut(server+"/send-message/"+recipient, message);
            System.out.println("ZACHLOG: send message attempt: " + response.toString());

            return Observable.just(response.getString("status"));
        } catch (Exception e) {
            e.printStackTrace();
            return Observable.error(e);
        }
    }
}
