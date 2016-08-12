package com.csandroid.myfirstapp.stages;

import com.csandroid.myfirstapp.utils.Notification;
import com.csandroid.myfirstapp.utils.WebHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import rx.Observable;
import rx.functions.Func1;

/**
 * Created by zachrdz 8/4/16.
 */
public class NotificationStage implements Func1<Integer, Observable<Notification>> {

    final String server;
    final String username;

    public NotificationStage(String server, String username){
        this.server = server;
        this.username = username;
    }

    @Override
    public Observable<Notification> call(Integer challenge_response)  {
        try {

            String response = WebHelper.StringGet(server+"/wait-for-push/"+username);
            JSONObject responseJson = new JSONObject(response);

            ArrayList<Notification> notifications = new ArrayList<>();
            JSONArray notifiers = responseJson.getJSONArray("notifications");

            for (int i = 0 ; i < notifiers.length(); i++) {
                JSONObject notification = notifiers.getJSONObject(i);
                String type = notification.getString("type");
                String username = "";

                if(type.equals("login")){
                    username = notification.getString("username");
                    notifications.add(new Notification.LogIn(username));
                } else if(type.equals("logout")){
                    username = notification.getString("username");
                    notifications.add(new Notification.LogOut(username));
                } else if(type.equals("message")){
                    JSONObject message = notification.getJSONObject("content");
                    System.out.println("ZACH: message: " + message.toString());
                    notifications.add(new Notification.Message(message));
                }
            }

            return Observable.from(notifications);
        } catch (Exception e) {
            e.printStackTrace();
            return Observable.error(e);
        }
    }
}
