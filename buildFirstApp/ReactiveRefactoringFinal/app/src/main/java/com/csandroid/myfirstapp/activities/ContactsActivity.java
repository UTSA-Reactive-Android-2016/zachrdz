package com.csandroid.myfirstapp.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.csandroid.myfirstapp.R;
import com.csandroid.myfirstapp.adapters.ContactAdapter;
import com.csandroid.myfirstapp.db.ContactDBHandler;
import com.csandroid.myfirstapp.db.LocalKeyPairDBHandler;
import com.csandroid.myfirstapp.models.Contact;
import com.csandroid.myfirstapp.models.LocalKeyPair;
import com.csandroid.myfirstapp.stages.NotificationStage;
import com.csandroid.myfirstapp.stages.RegisterContactsStage;
import com.csandroid.myfirstapp.utils.Crypto;
import com.csandroid.myfirstapp.utils.Notification;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class ContactsActivity extends AppCompatActivity {

    private RecyclerView recList;
    LocalKeyPairDBHandler localKeyPairDB;
    LocalKeyPair localKeyPair;
    List<Contact> contactsList;
    ContactAdapter ca;

    Crypto myCrypto;

    // Subscription holder
    private CompositeSubscription cs = new CompositeSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        Toolbar toolbar = (Toolbar) findViewById(R.id.contacts_toolbar);
        setSupportActionBar(toolbar);
        setTitle("Contacts");
        if(null != getSupportActionBar()){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        this.initKey();
        this.setupRecyclerView();
        this.registerContacts();
        this.initSearchListener();
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.contacts_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_add_contact:
                Intent addContactIntent = new Intent(ContactsActivity.this, AddContactActivity.class);
                addContactIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(addContactIntent);
                return true;
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        cs = new CompositeSubscription();

        // When the view is brought back into focus, reload contacts
        // list to make sure user doesn't see stale data.
        ContactAdapter ca = new ContactAdapter(createList());
        recList.setAdapter(ca);
        recList.invalidate();

        this.initKey();
        this.registerContacts();

        // Clear out search field text if any typed
        EditText searchField = (EditText)findViewById(R.id.search_bar);
        if(searchField != null) {
            searchField.setText("");
            searchField.clearFocus();
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        cs.unsubscribe();
        cs = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(null != cs){
            cs.unsubscribe();
        }
        cs = null;
    }

    public void initKey() {
        // Logged in User
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String username = prefs.getString("username", "");

        localKeyPairDB = new LocalKeyPairDBHandler(this);
        localKeyPair = localKeyPairDB.getKeyPairByUsername(username);
        getPreferences(Context.MODE_PRIVATE).edit().putString(Crypto.prefPrivateKey, localKeyPair.getPrivateKey()).apply();
        getPreferences(Context.MODE_PRIVATE).edit().putString(Crypto.prefPublicKey, localKeyPair.getPublicKey()).apply();

        myCrypto = new Crypto(getPreferences(Context.MODE_PRIVATE));
    }

    public void registerContacts(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String username = prefs.getString("username", "");
        String hostName = prefs.getString("serverName", "");
        String portNumber = prefs.getString("serverPort", "");
        PublicKey serverKey = Crypto.getPublicKeyFromString(prefs.getString("serverKey", ""));
        final String server = "http://" + hostName + ":" + portNumber;

        ArrayList<String> contacts = new ArrayList<>();
        for(Contact contact:contactsList){
            contacts.add(contact.getUsername());
        }

        Subscription regContacts = Observable.just("ok")
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .flatMap(new Func1<String, Observable<String>>() {
                    @Override
                    public Observable<String> call(String status) {
                        return Observable.just(status);
                    }
                })
                .flatMap(new RegisterContactsStage(server, username, contacts))
                .subscribe(new Observer<Notification>() {
                    @Override
                    public void onCompleted() {
                        Subscription contactStatus = Observable.interval(0, 1, TimeUnit.SECONDS, Schedulers.newThread())
                                .subscribeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Observer<Long>() {
                                    @Override
                                    public void onCompleted() {
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                    }

                                    @Override
                                    public void onNext(Long numTicks) {
                                        Observable.just(0)
                                                .subscribeOn(AndroidSchedulers.mainThread())
                                                .observeOn(Schedulers.io())
                                                .flatMap(new NotificationStage(server, username))
                                                .subscribe(new Observer<Notification>() {
                                                    @Override
                                                    public void onCompleted() {

                                                    }

                                                    @Override
                                                    public void onError(Throwable e) {

                                                    }

                                                    @Override
                                                    public void onNext(final Notification notification) {
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                handleContactStatus(notification);
                                                            }
                                                        });
                                                    }
                                                });
                                        Log.d("POLL", "Polling Contact Status...");
                                    }
                                });
                        cs.add(contactStatus);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("LOG","Error: ",e);
                    }

                    @Override
                    public void onNext(final Notification notification) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                handleContactStatus(notification);
                            }
                        });
                    }
                });

        cs.add(regContacts);
    }

    public void initSearchListener(){
        EditText searchField = (EditText)findViewById(R.id.search_bar);

        if (null != searchField) {
            // When the user edits the username text field, update the contacts shown
            searchField.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if(createList().size() > 0) {
                        cs.unsubscribe();
                        cs = null;
                        cs = new CompositeSubscription();

                        Log.d("Search Text", getSearchFieldValue());
                        createList();
                        // User is actively editing username field. Update list appropriately
                        ArrayList<Contact> filteredList = new ArrayList<>();
                        for (Contact c : contactsList) {
                            if (c.getUsername().contains(getSearchFieldValue())) {
                                filteredList.add(c);
                                Log.d("Search Text Match:", c.getUsername());
                            }
                        }
                        contactsList = filteredList;
                        ca = new ContactAdapter(contactsList);
                        recList.setAdapter(ca);
                        recList.invalidate();

                        registerContacts();
                    }
                }
            });
        }
    }

    private void handleContactStatus(Notification notification){
        String username = "";
        // handle updating state here
        Log.d("LOG", "Next " + notification);
        if (notification instanceof Notification.LogIn) {
            username = ((Notification.LogIn) notification).username;
            Log.d("LOG", "User " + username + " is logged in");

            int i = 0;
            for (Contact contact : contactsList) {
                if (contact.getUsername().equals(username)) {
                    View v = recList.getLayoutManager().findViewByPosition(i);

                    // Toggle online status here
                    if (null != v) {
                        v.findViewById(R.id.online).setVisibility(View.VISIBLE);
                        v.findViewById(R.id.offline).setVisibility(View.GONE);
                    }
                    break;
                }
                i++;
            }
        }

        if (notification instanceof Notification.LogOut) {
            username = ((Notification.LogOut) notification).username;
            Log.d("LOG", "User " + username + " is logged out");

            int i = 0;
            for (Contact contact : contactsList) {
                if (contact.getUsername().equals(username)) {
                    View v = recList.getLayoutManager().findViewByPosition(i);

                    if (null != v) {
                        // Toggle online status here
                        v.findViewById(R.id.online).setVisibility(View.GONE);
                        v.findViewById(R.id.offline).setVisibility(View.VISIBLE);
                    }
                    break;
                }
                i++;
            }
        }
    }

    private List<Contact> createList() {
        ContactDBHandler db = new ContactDBHandler(this);
        contactsList = db.getContacts(localKeyPair.getId());
        return contactsList;
    }

    private void setupRecyclerView(){
        recList = (RecyclerView) findViewById(R.id.contacts_cards_list);
        if(null != recList){
            recList.setHasFixedSize(true);
            LinearLayoutManager llm = new LinearLayoutManager(this);
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            recList.setLayoutManager(llm);

            createList();
            ca = new ContactAdapter(contactsList);
            recList.setAdapter(ca);
        }
    }

    private String getSearchFieldValue() {
        EditText searchField = ((EditText) findViewById(R.id.search_bar));
        if (searchField != null) {
            return searchField.getText().toString();
        } else {
            return "";
        }
    }
}
