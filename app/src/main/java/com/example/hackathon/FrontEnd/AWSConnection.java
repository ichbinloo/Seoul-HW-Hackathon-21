package com.example.hackathon.FrontEnd;

import android.content.Context;
import com.amazonaws.regions.Regions;
import com.example.hackathon.MainActivity;

public class AWSConnection {
    static final String LOG_TAG = AWSConnection.class.getCanonicalName();
    private static final Regions MY_REGION = Regions.DEFAULT_REGION;
    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";

    private Context mContext;
    private MainActivity mActivity;
    private static AWSConnection managerInstance;
    private boolean mConnected;

    private AWSConnection(Context context) {
        mActivity = (MainActivity) context;
        mContext = context;
    }

    public static AWSConnection getInstance(Context context) {
        if (managerInstance == null) {
            managerInstance = new AWSConnection(context);
        }
        return managerInstance;
    }

    public boolean isConnected() {
        return mConnected;
    }

}
