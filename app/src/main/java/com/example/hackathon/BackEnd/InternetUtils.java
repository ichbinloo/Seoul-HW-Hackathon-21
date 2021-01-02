package com.example.hackathon.BackEnd;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.example.hackathon.R;

public class InternetUtils {

    public static final String LOG_TAG = InternetUtils.class.getCanonicalName();

    public static boolean hasInternetConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.
                        getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            ToastMessage.setToastMessage(context,
                    context.getString(R.string.no_internet) + " " +
                            context.getString(R.string.please_connect),
                    Toast.LENGTH_LONG);
            Log.i(LOG_TAG,"Internet is not available");
            AWSConnection.getInstance(context).disconnectAWS();
        } else {
            Log.d(LOG_TAG, "Internet OK");
        }
        return isConnected;
    }

}
