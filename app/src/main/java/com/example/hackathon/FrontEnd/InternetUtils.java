package com.example.hackathon.FrontEnd;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class InternetUtils {
    public static final String LOG_TAG = InternetUtils.class.getCanonicalName();

    public static boolean hasInternetConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.
                        getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            Log.i(LOG_TAG,"Internet is not available");
            //AWSConnection.getInstance(context).disconnectAWS();
        } else {
            Log.d(LOG_TAG, "Internet OK");
        }

        return isConnected;
    }
}
