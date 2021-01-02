package com.example.hackathon.BackEnd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class InternetConnection extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        InternetUtils.hasInternetConnection(context);
    }
}
