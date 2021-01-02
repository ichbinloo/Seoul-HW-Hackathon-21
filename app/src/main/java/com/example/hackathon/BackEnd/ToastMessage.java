package com.example.hackathon.BackEnd;

import android.content.Context;
import android.widget.Toast;

public abstract class ToastMessage {

    private static Toast sToastMessage;
    // Avoid instantiate more than one time
    public static void setToastMessage(final Context context,
                                       final String msg,
                                       final int duration) {
        //create toast message if it was not created previously
        if (sToastMessage == null) {
            sToastMessage = Toast.makeText(context, msg, duration);
        } else {
            sToastMessage.setText(msg);
            sToastMessage.setDuration(duration);
        }
        //show the toast message
        sToastMessage.show();
    }

    public static void cancelToastMessage() {
        sToastMessage.cancel();
    }
}
