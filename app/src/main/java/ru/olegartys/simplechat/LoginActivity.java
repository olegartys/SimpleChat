package ru.olegartys.simplechat;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class LoginActivity extends ActionBarActivity {

    private boolean mBound = false;
    private Messenger mMessenger;

    private EditText loginEntry;
    private Button loginBut;
    private ImageButton imgButton;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mMessenger = new Messenger(service);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mMessenger = null;
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginEntry = (EditText)findViewById(R.id.login_entry);
        loginBut = (Button)findViewById(R.id.login_button);
        imgButton = (ImageButton)findViewById(R.id.clear_button);

        //On click clear login entry
        imgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginEntry.getText().clear();
            }
        });

        //On click connect to server
        loginBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String usrLogin = loginEntry.getText().toString();
                if (usrLogin.isEmpty()) {
                    Toast.makeText(v.getContext(), "Enter your login!",
                            Toast.LENGTH_SHORT).show();
                } else
                    //if there is internet connection
                    if (Utils.isOnline(v.getContext())) {
                        //sending login from entry
                        ConnectToServerTask mt = new ConnectToServerTask(v.getContext(),
                            usrLogin);

                        mt.execute();
                    } else {
                        Toast.makeText(v.getContext(),
                                "Check internet connection!",Toast.LENGTH_SHORT).show();
                    }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        //binding to a service
        bindService(new Intent(this, MessageListenService.class),
                mConnection, Context.BIND_AUTO_CREATE);
    }

    //FIXME: посмотреть чactivity lifecycle. Сервис не должен завершиться при переходе на другую активити
    @Override
    public void onDestroy() {
        Log.d("!", "login sctivity destroyed");
        mBound = false;
        unbindService(mConnection);
        super.onDestroy();
    }

    @Override
    public void setContentView(int layoutResID) {
        setFonts();
        super.setContentView(layoutResID);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
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

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    /**
     * Set fonts to an activity elements.
     */
    private void setFonts() {
        final String GLOBAL_FONT = "fonts/SinkinSans-200XLight.otf";

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath(GLOBAL_FONT)
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );
    }

    /**
     * AsyncTask where connection with server is establishing. It opens user socket, creates
     * user object (ServerUser) and sends message to server that connection is good. After it new
     * activity is loading.
     */
    class ConnectToServerTask extends AsyncTask<Void, Void, Integer> {

        private Context context;
        private String usrLogin;

        private Boolean received = false;
        private int connectionResultCode;

        private android.os.Message connectionMsg;

        //dialog shown when connection is establishing
        private ProgressDialog connectionDialog;

        public ConnectToServerTask(Context context, String usrLogin) {
            this.context = context;
            this.usrLogin = usrLogin;
        }

        @Override
        protected void  onPreExecute() {
            //creating connect message that would be sent to a service
            if (!mBound) cancel(true);
            connectionMsg = android.os.Message.obtain(null,
                    MessageListenService.MessageType.MSG_CONNECT_TO_SERVER, 0, 0);

            //we will send user login
            Bundle b = new Bundle();
            b.putString("login", usrLogin);
            connectionMsg.setData(b);

            //Messenger, to whom service will send messages
            connectionMsg.replyTo = new Messenger(new ResponseHandler());

            //show connection dialog
            connectionDialog = new ProgressDialog(context);
            connectionDialog.setMessage("Connecting to server, please wait!");
            connectionDialog.setCanceledOnTouchOutside(false);

            connectionDialog.show();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            //sending message to a service
             try {
                mMessenger.send(connectionMsg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            //waiting for message from service
            synchronized(received) {
                try {
                    received.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //if service was not able to connect, then cancel task
            switch (connectionResultCode) {
                case MessageListenService.Errors.CONNECTION_FAILURE:
                    this.cancel(true);
                    break;
                case MessageListenService.Errors.INCORRECT_LOGIN:
                    return MessageListenService.Errors.INCORRECT_LOGIN;
                default:
                    return MessageListenService.Errors.SUCCESS;
            }

            //else executing onPostExecute
            return MessageListenService.Errors.SUCCESS;
        }

        //It starts if connection was successful
        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                case MessageListenService.Errors.INCORRECT_LOGIN:
                    Toast.makeText(context,
                            "User with such login already exists!", Toast.LENGTH_SHORT).show();
                    connectionDialog.cancel();
                    break;

                case MessageListenService.Errors.SUCCESS:
                    //close progress dialog
                    connectionDialog.cancel();
                    Toast.makeText(context, "Connection successful!", Toast.LENGTH_SHORT).show();

                    //starting new activity
                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.putExtra("login", usrLogin);

                    context.startActivity(intent);
                    break;
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            connectionDialog.cancel();
            showErrDialog();
        }

        //shows dialog about error when connect
        private void showErrDialog () {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Error!")
                    .setCancelable(false)
                    .setMessage("Can't connect to server!")
                    .setNegativeButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();
            ((TextView) alert.findViewById(android.R.id.message)).setGravity(Gravity.CENTER);
        }

        /**
         * Handler that would receive messages from a service
         */
        class ResponseHandler extends Handler {
            @Override
            public void handleMessage(final android.os.Message msg) {
                //see the type of message
                switch (msg.what) {
                    case MessageListenService.MessageType.MSG_CONNECT_TO_SERVER:
                        connectionResultCode = msg.getData().getInt("result");
                        synchronized (received) {
                            received.notifyAll();
                            received = true;
                        }
                        break;

                    default:
                        super.handleMessage(msg);
                }
            }
        }
    }

}
