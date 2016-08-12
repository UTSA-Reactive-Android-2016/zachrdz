package com.csandroid.myfirstapp.stages;

import com.csandroid.myfirstapp.models.UserInfo;
import com.csandroid.myfirstapp.utils.WebHelper;

import org.json.JSONObject;

import rx.Observable;
import rx.functions.Func1;

/**
 * Created by zachrdz 8/4/16.
 */
public class GetUserInfoStage implements Func1<Integer, Observable<UserInfo>> {

    final String server;
    final String username;

    public GetUserInfoStage(String server, String username){
        this.server = server;
        this.username = username;
    }

    @Override
    public Observable<UserInfo> call(Integer number)  {
        UserInfo ui = null;

        try {
            String response = WebHelper.StringGet(server+"/get-contact-info/"+username);
            JSONObject responseJson = new JSONObject(response);

            String status = responseJson.getString("status");
            if(status.equals("ok")) {
                String username = responseJson.getString("username");
                String image = responseJson.getString("image");
                String key = responseJson.getString("key");

                ui = new UserInfo(username, image, key);
            }

            return Observable.just(ui);
        } catch (Exception e) {
            e.printStackTrace();
            return Observable.error(e);
        }
    }
}
