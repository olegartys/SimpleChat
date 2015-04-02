package ru.olegartys.chat_message;
/**
 * Created by olegartys on 28.01.15.
 */
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.String;import java.lang.System;import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class Message implements Serializable {

    private static final long serialVersionUID = -6504719361565270383L;

    private ServerUser usr;
    private String login;
    private String msg;
    private Date time;
    /*private ArrayList<ServerUser> onlineUsers;

    public Message (ServerUser usr, String msg, ArrayList<ServerUser> onlineUsers) {
        this.usr = usr;
        this.msg = msg;
        this.onlineUsers = onlineUsers;
    }*/

    public Message (ServerUser usr, String msg) {
        time = new Date();
        this.usr = usr;
        this.msg = msg;
    }

    public Message (String login, String msg) {
        time = new Date();
        usr = new ServerUser(login);
        this.login = login;
        this.msg = msg;
    }

    public String getLogin () {
        return usr.getLogin();
    }

    public String getMessage () {
        return msg;
    }

    public Date getTime() {
        return time;
    }

}