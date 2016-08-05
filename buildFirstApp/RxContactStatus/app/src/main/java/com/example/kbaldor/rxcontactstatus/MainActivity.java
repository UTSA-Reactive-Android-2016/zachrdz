package com.example.kbaldor.rxcontactstatus;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

import com.example.kbaldor.rxcontactstatus.stages.GetChallengeStage;
import com.example.kbaldor.rxcontactstatus.stages.GetServerKeyStage;
import com.example.kbaldor.rxcontactstatus.stages.LogInStage;
import com.example.kbaldor.rxcontactstatus.stages.NotificationStage;
import com.example.kbaldor.rxcontactstatus.stages.RegisterContactsStage;
import com.example.kbaldor.rxcontactstatus.stages.RegistrationStage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    Crypto myCrypto;

    String username = "zzz123";
    String server_name = "http://129.115.27.54:25666";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ArrayList<String> contacts = new ArrayList<>();
        contacts.add("alice");
        contacts.add("bob");
        contacts.add("cathy");


        myCrypto = new Crypto(getPreferences(Context.MODE_PRIVATE));

        final TextView contactsListView = (TextView)findViewById(R.id.contacts_list);
        final TextView loggedInContactsView = (TextView)findViewById(R.id.logged_in_list);
        final TextView loggedOutContactsView = (TextView)findViewById(R.id.logged_out_list);

        for(int i = 0; i < contacts.size(); i++){
            contactsListView.setText(contactsListView.getText().toString() + contacts.get(i));

            // If it's not the last one, then add a comma
            if(!((i+1) == contacts.size())){
                contactsListView.setText(contactsListView.getText().toString() + ", ");
            }
        }

        Observable.just(0) // the value doesn't matter, it just kicks things off
                .observeOn(Schedulers.newThread())
                .subscribeOn(Schedulers.newThread())
                .flatMap(new GetServerKeyStage(server_name))
                .flatMap(new RegistrationStage(server_name, username,
                                               getBase64Image(), myCrypto.getPublicKeyString()))
                .flatMap(new GetChallengeStage(server_name,username,myCrypto))
                .flatMap(new LogInStage(server_name, username))
                .flatMap(new RegisterContactsStage(server_name, username, contacts))
                .subscribe(new Observer<Notification>() {
            @Override
            public void onCompleted() {

                // now that we have the initial state, start polling for updates

                Observable.interval(0,1, TimeUnit.SECONDS, Schedulers.newThread())
                        .subscribeOn(AndroidSchedulers.mainThread())
                     //   .take(5) // would only poll five times
                     //   .takeWhile( <predicate> ) // could stop based on a flag variable
                        .subscribe(new Observer<Long>() {
                            @Override
                            public void onCompleted() {}

                            @Override
                            public void onError(Throwable e) {}

                            @Override
                            public void onNext(Long numTicks) {
                                Observable.just(0)
                                        .flatMap(new NotificationStage(server_name, username))
                                        .subscribe(new Observer<Notification>() {
                                            @Override
                                            public void onCompleted() {

                                            }

                                            @Override
                                            public void onError(Throwable e) {

                                            }

                                            @Override
                                            public void onNext(Notification notification) {
                                                String username = "";
                                                // handle updating state here
                                                Log.d("LOG","Next "+ notification);
                                                if(notification instanceof Notification.LogIn) {
                                                    username = ((Notification.LogIn)notification).username;
                                                    Log.d("LOG","User "+ username + " is logged in");

                                                    removeUsernameFromView(loggedOutContactsView, username);
                                                    addUsernameToView(loggedInContactsView, username);
                                                }
                                                if(notification instanceof Notification.LogOut) {
                                                    username = ((Notification.LogOut)notification).username;
                                                    Log.d("LOG","User "+username+" is logged out");

                                                    removeUsernameFromView(loggedInContactsView, username);
                                                    addUsernameToView(loggedOutContactsView, username);
                                                }
                                            }
                                        });
                                Log.d("POLL","Polling "+numTicks
                                );
                            }
                        });
            }

            @Override
            public void onError(Throwable e) {
                Log.d("LOG","Error: ",e);
            }

            @Override
            public void onNext(Notification notification) {
                String username = "";
                // handle initial state here
                Log.d("LOG","Next "+ notification);
                if(notification instanceof Notification.LogIn) {
                    username = ((Notification.LogIn)notification).username;
                    Log.d("LOG","User "+ username + " is logged in");

                    addUsernameToView(loggedInContactsView, username);
                }
                if(notification instanceof Notification.LogOut) {
                    username = ((Notification.LogOut)notification).username;
                    Log.d("LOG","User "+username+" is logged out");

                    addUsernameToView(loggedOutContactsView, username);
                }
            }
        });

    }

    String getBase64Image(){
        InputStream is;
        byte[] buffer = new byte[0];
        try {
            is = getAssets().open("images/ic_android_black_24dp.png");
            buffer = new byte[is.available()];
            is.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Base64.encodeToString(buffer,Base64.DEFAULT).trim();
    }

    boolean register(String username, String base64Image, String keyString){
        return true;
    }

    private void addUsernameToView(final TextView text,final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String oldValue = text.getText().toString();

                // Remove username from string if it exists
                String newValue = oldValue.replaceAll(value + System.getProperty("line.separator"), "");
                text.setText(newValue);

                // Add back username to string
                text.append(value + System.getProperty("line.separator"));

                // add in any needed commas
            }
        });
    }

    private void removeUsernameFromView(final TextView text,final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String oldValue = text.getText().toString();

                // Remove username from string if it exists
                String newValue = oldValue.replaceAll(value + System.getProperty("line.separator"), "");
                text.setText(newValue);
            }
        });
    }

}
