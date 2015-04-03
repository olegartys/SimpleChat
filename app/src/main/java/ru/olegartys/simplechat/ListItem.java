package ru.olegartys.simplechat;

import java.util.Date;

import ru.olegartys.chat_message.Message;

/**
 * Created by olegartys on 25.03.15.
 */
public class ListItem {
    private String msg, login;
    private Date date;

    boolean isSelf;

    public ListItem(String msg, String login, boolean isSelf) {
        this.msg = msg;
        this.isSelf = isSelf;
        if (true) {}
    }

    public ListItem (Message sourceMsg, boolean isSelf) {
        this.msg = sourceMsg.getMessage();
        this.login = sourceMsg.getLogin();
        this.date = sourceMsg.getTime();
        this.isSelf = isSelf;
    }

    public boolean isSelf() {
        return isSelf;
    }

    public String getLogin() {
        return login;
    }

    public String getMsg() {
        return msg;
    }
}
