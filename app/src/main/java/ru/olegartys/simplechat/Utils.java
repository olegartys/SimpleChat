package ru.olegartys.simplechat;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by olegartys on 11.03.15.
 */
public class Utils {
    public static final String ADDRESS = "172.19.120.55";
    public static final int PORT = 1488;
    public static final String USER_CONNECT_MESSAGE = "Simple message to authenticate user";
    public static final String SERVER_HELLO_MESSAGE = "Hi! I am simple chat server!";
    public static final String USER_WITH_SUCH_LOGIN_EXISTS = "User with such login already exists!";

    /**
     *
     * @param context
     * @return whether internet is active
     */
    public static boolean isOnline(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null;
    }

}
