package com.csandroid.myfirstapp.models;

public class Message {
    private int id;
    private String senderUsername;
    private String subject;
    private String messageBody;
    private int TTL;

    public Message(){

    }

    public Message(int id, String username, String subject, String body, int TTL){
        this.id = id;
        this.senderUsername = username;
        this.subject = subject;
        this.messageBody = body;
        this.TTL = TTL;
    }

    public Message(String username, String subject, String body, int TTL){
        this.senderUsername = username;
        this.subject = subject;
        this.messageBody = body;
        this.TTL = TTL;
    }

    public int getTTL() {
        return TTL;
    }

    public void setTTL(int TTL) {
        this.TTL = TTL;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
