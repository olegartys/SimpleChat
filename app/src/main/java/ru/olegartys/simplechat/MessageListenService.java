package ru.olegartys.simplechat;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import ru.olegartys.chat_message.Message;
import ru.olegartys.chat_message.ServerUser;

public class MessageListenService extends Service {

    public static ServerUser usr = null;
    private String login;
    public static Socket sock;
    private boolean firstBind = true;

    private Messenger responseMessenger;

    //Timeout for connection with server
    private static final int SOCK_TIMEOUT = 20000;

    /**
     * Type of messages that could be sent to a client.
     */
    public static class MessageType {
        public static final int MSG_CONNECT_TO_SERVER = 0;
        public static final int MSG_SEND = 1;
        public static final int MSG_RECEIVED = 2;
        public static final int SERVER_ERR = 3;
        public static final int START_LISTEN = 4;
    }

    /**
     * Type of errors that could be sent to a client.
     */
    public static class Errors {
        public static final int SUCCESS = 0;
        public static final int CONNECTION_FAILURE = 1;
        public static final int RECEIVING_MSG_FAILURE = 2;
        public static final int INCORRECT_LOGIN = 3;
        public static final int SENDING_MESSAGE_FAILURE = 4;
    }

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(final android.os.Message msg) {
            //to whom we will answer
            responseMessenger = msg.replyTo;
            switch (msg.what) {
                //if client want's to connect to service
                case MessageType.MSG_CONNECT_TO_SERVER:
                    onConnectToServer(msg);
                    break;

                //Listen server for a new messages
                case MessageType.START_LISTEN:
                    onStartListen();
                    break;

                //send message to a server
                case MessageType.MSG_SEND:
                    onMessageSend(msg);
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * It runs when received CONNECT_TO_SERVER msg.
     * @param msg
     */
    private void onConnectToServer(android.os.Message msg) {
        login = msg.getData().getString("login");

        //connecting to a server
        new Thread(new Runnable() {
            @Override
            public void run() {
                sock = new Socket();
                try {
                    //establishing connection
                    sock.connect(new InetSocketAddress(Utils.ADDRESS, Utils.PORT), SOCK_TIMEOUT);
                    usr = new ServerUser(sock, login);

                    //sending test message to server
                    Message helloMsg = new Message(usr, Utils.USER_CONNECT_MESSAGE);
                    usr.getUserOutputStream().writeObject(helloMsg);

                    //receiving test message from server
                    try {
                        Message serverResponse = (Message) usr.getUserInputStream().readObject();

                        if (serverResponse.getMessage().equals(Utils.SERVER_HELLO_MESSAGE)) {
                            //if connection was successful send "good" message to a client
                            MessageListenService.this.sendMessageToActivity(null, MessageType.MSG_CONNECT_TO_SERVER,
                                    Errors.SUCCESS, responseMessenger);

                        } else if (serverResponse.getMessage().equals(Utils.USER_WITH_SUCH_LOGIN_EXISTS)) {
                            Log.d("resp", serverResponse.getMessage());
                            MessageListenService.this.sendMessageToActivity(null, MessageType.MSG_CONNECT_TO_SERVER,
                                    Errors.INCORRECT_LOGIN, responseMessenger);
                        } //else throw new IOException();

                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        throw new IOException();
                    }

                } catch (IOException e) {
                    MessageListenService.this.sendMessageToActivity(null, MessageType.MSG_CONNECT_TO_SERVER,
                            Errors.CONNECTION_FAILURE, responseMessenger);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * It runs when received START_LISTEN msg.
     */
    private void onStartListen() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Message receivedMsg = (Message) usr.getUserInputStream().readObject();
                        showPushNotification(receivedMsg);
                        MessageListenService.this.sendMessageToActivity(receivedMsg, MessageType.MSG_RECEIVED,
                                Errors.SUCCESS, responseMessenger);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        //throw new IOException();
                    } catch (IOException e) {
                        MessageListenService.this.sendMessageToActivity(null, MessageType.SERVER_ERR,
                                Errors.CONNECTION_FAILURE, responseMessenger);
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }).start();
    }

    //FIXME: on push on notification loads EMPTY chat activity
    private void showPushNotification(Message msg) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.stat_notify_chat)
                        .setContentTitle(msg.getLogin())
                        .setContentText(msg.getMessage());
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, ChatActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ChatActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(0, mBuilder.build());
    }

    /**
     * It runs when received SEND msg.
     * @param msg
     */
    private void onMessageSend(android.os.Message msg) {
        Message sendMsg = new Message(usr, msg.getData().getString("msg"));
        try {
            usr.getUserOutputStream().writeObject(sendMsg);
            //MessageListenService.this.sendMessageToActivity(null, MessageType.SERVER_ERR,
            //Errors.SENDING_MESSAGE_FAILURE, responseMessenger);
        } catch (IOException e) {
            MessageListenService.this.sendMessageToActivity(null, MessageType.MSG_SEND,
                    Errors.SENDING_MESSAGE_FAILURE, responseMessenger);
        }
    }

    /**
     * Sends message to a client (ex. Activity).
     * @param msg
     * @param msgType
     * @param errCode
     * @param responseMessenger
     */
    private void sendMessageToActivity(Message msg, int msgType, int errCode, Messenger responseMessenger) {
        android.os.Message responseMsg = android.os.Message.obtain(null, msgType);
        Bundle b = new Bundle();
        if (msg != null) b.putSerializable("msg", msg);
        b.putInt("result", errCode);
        responseMsg.setData(b);
        try {
            responseMessenger.send(responseMsg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        if (firstBind == true) {
            firstBind = false;
           // login = intent.getStringExtra("login");
        }
//        Log.d("service", login);
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("service", "Created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("service", "destroyed");
    }

}
