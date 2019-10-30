package com.hdpolover.ybbproject.notifications;

public class Token {
    //An FCM token, or much commonly known as a registration Token
    //an ID issued by GCM connection servers to the client app that allows it to receive message

    String token;

    public Token(String token){
        this.token = token;
    }

    public Token(){

    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
