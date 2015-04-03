package ru.olegartys.simplechat;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import ru.olegartys.chat_message.Message;

public class ChatActivity extends ActionBarActivity {

    private String login;

    private ListView chatList;
    private ArrayList<ListItem> messagesList;
    private MessageListAdapter listAdapter;

    private EditText inputMsg;
    private Button btnSend;

    private Messenger serviceMessenger;
    private Messenger responseMessenger;
    private Boolean mBound = false;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            serviceMessenger = new Messenger(service);

            synchronized (mBound) {
                mBound.notifyAll();
            }
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceMessenger = null;
            mBound = false;
        }
    };

    /**
     * Handler that would receive messages from a service
     */
    class ResponseHandler extends Handler {
        @Override
        public void handleMessage(final android.os.Message msg) {

            switch (msg.what) {
                case MessageListenService.MessageType.SERVER_ERR:
                    onDisconnect();
                    break;

                case MessageListenService.MessageType.MSG_RECEIVED:
                    onMessageReceived((Message)msg.getData().getSerializable("msg"),
                            msg.getData().getInt("result"));
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //Adding adapter to list
        setListView();

        responseMessenger = new Messenger(new ResponseHandler());

        //Start listening for new messages
        sendMsgToService(MessageListenService.MessageType.START_LISTEN, null);

        inputMsg = (EditText)findViewById(R.id.inputMsg);
        btnSend = (Button)findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Read msg from editview
                final Editable s = inputMsg.getText();
                Message msg = new Message(login, s.toString());

                //Add them to the list
                messagesList.add(new ListItem(msg, true));
                listAdapter.notifyDataSetChanged();

                //Send message to a service; service will send it to server
                Bundle b = new Bundle();
                b.putString("msg", s.toString());
                sendMsgToService(MessageListenService.MessageType.MSG_SEND, b);

                //clear entry
                inputMsg.getText().clear();
            }
        });
    }

    /**
     * send msg to a service
     * @param msgType
     * @param b
     */
    private void sendMsgToService(final int msgType, final Bundle b) {

        new Thread (new Runnable() {
            @Override
            public void run() {
                android.os.Message sendMsg = android.os.Message.obtain(null,
                        msgType, 0, 0);

                if (b != null) sendMsg.setData(b);
                sendMsg.replyTo = responseMessenger;

                //Wait for activity to connect to service
                if (mBound == false)
                    synchronized(mBound) {
                        try {
                            mBound.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                //send message to service
                try {
                    serviceMessenger.send(sendMsg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    /**
     * When message received from server add it to list in case of success.
     * If sending message failed show err and disconnect from server.
     * @param receivedMsg
     */
    private void onMessageReceived(Message receivedMsg, final int errCode) {
        switch (errCode) {
            case MessageListenService.Errors.SUCCESS:
                messagesList.add(new ListItem(receivedMsg, false));
                listAdapter.notifyDataSetChanged();
                break;

            case MessageListenService.Errors.SENDING_MESSAGE_FAILURE:
                onDisconnect();
                break;
        }

    }

    @Override
    protected void onStart() {
        bindService(new Intent(this, MessageListenService.class), mConnection,
                Context.BIND_AUTO_CREATE);
        super.onStart();
    }

    //FIXME: how NOT to return to previos activity on back press??
    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onDestroy() {
        Log.d("!", "chat activity destroyed");
        mBound = false;
        unbindService(mConnection);
        super.onDestroy();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        login = getIntent().getStringExtra("login");
        getSupportActionBar().setTitle(login);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setListView() {
        chatList = (ListView)findViewById(R.id.list_view_messages);

        messagesList = new ArrayList<ListItem>();

        listAdapter = new MessageListAdapter(this, messagesList);
       // listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrList);
        chatList.setAdapter(listAdapter);
    }

    /**
     * If connection with server interrupted
     */
    private void onDisconnect() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error!")
                .setCancelable(false)
                .setMessage("Connection error!")
                .setNegativeButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
        ((TextView) alert.findViewById(android.R.id.message)).setGravity(Gravity.CENTER);
    }

}
