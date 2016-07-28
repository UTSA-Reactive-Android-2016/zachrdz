package com.csandroid.myfirstapp.api.core;

import com.csandroid.myfirstapp.utils.Crypto;

import java.security.PublicKey;
import java.util.ArrayList;

public interface AppAPI {
    void setServerPort(final String serverPort);
    void setServerName(final String serverName);
    void checkAPIVersion();
    void register(final String username, String image, String publicKey);
    void getUserInfo(final String username);
    void login(final String username,final Crypto crypto);
    void logout(final String username,final Crypto crypto);
    void registerContacts(String username, final ArrayList<String> names);
    void startPushListener(final String username);
    void sendMessage(final Object messageReference,
                            PublicKey recipientKey,
                            String sender,
                            String recipient,
                            String subjectLine,
                            String body,
                            Long bornOnDate,
                            Long timeToLive);
}
