package ru.olegartys.chat_message;

/**
 * Created by olegartys on 28.01.15.
 */

import java.io.*;
import java.io.IOException;import java.io.ObjectInputStream;import java.io.ObjectOutputStream;import java.io.Serializable;import java.lang.String;import java.lang.System;import java.net.Socket;
import java.net.SocketException;

public class ServerUser implements Serializable {

    private Socket sock;
    private ObjectInputStream is;
    private ObjectOutputStream os;
    protected String login;
    private Integer num;

    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("login", String.class),
            new ObjectStreamField("num", Integer.class)};

    public ServerUser () {

    }

    public ServerUser (String login) {
        this.login = login;
    }

    /**
     * Server constructor
     * @param clientSock
     * @param num
     *
     */
    public ServerUser (Socket clientSock, int num) {
        sock = clientSock;
        this.num = num;
    }

    /**
     * Client constructor
     * @param clientSock
     * @param login
     *
     */
    public ServerUser (Socket clientSock, String login) {
        sock = clientSock;
        try {
            is = new ObjectInputStream(sock.getInputStream());
            os = new ObjectOutputStream(sock.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.login = login;
    }

    public ServerUser (Socket clientSock, ObjectInputStream is, ObjectOutputStream os,
                       String login, int num) {
        this.sock = clientSock;
        this.is = is;
        this.os = os;
        this.login = login;
        this.num = num;
    }


    public Socket getUserSocket () {
        return this.sock;
    }

    public ObjectInputStream getUserInputStream () {
        return this.is;
    }

    public ObjectOutputStream getUserOutputStream () {
        return this.os;
    }

    public String getLogin () {
        return this.login;
    }

    public void setInputStream (ObjectInputStream is) {
        this.is = is;
    }

    public void setOutputStream (ObjectOutputStream os) {
        this.os = os;
    }

    public void setLogin (String login) {
        this.login = login;
    }

    public int getNum () {
        return this.num;
    }

}
