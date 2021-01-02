package com.example.hackathon;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.datastore.AWSDataStorePlugin;
import com.amplifyframework.datastore.generated.model.HeyDoc;
import com.example.hackathon.BackEnd.AWSConnection;
import com.example.hackathon.BackEnd.InternetUtils;

public class MainActivity extends AppCompatActivity {
    private boolean internetConnection;

    // Android onCreate method to call actions when the Activity is launched.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeComponents();
        try {
            Amplify.addPlugin(new AWSApiPlugin());
            Amplify.addPlugin(new AWSDataStorePlugin());
            Amplify.configure(getApplicationContext());

            Log.i("Tutorial", "Initialized Amplify");
        } catch (AmplifyException failure) {
            Log.e("Tutorial", "Could not initialize Amplify", failure);
        }

        Amplify.DataStore.observe(HeyDoc.class,
                started -> Log.i("Tutorial", "Observation began."),
                change -> Log.i("Tutorial", change.item().toString()),
                failure -> Log.e("Tutorial", "Observation failed.", failure),
                () -> Log.i("Tutorial", "Observation complete.")
        );
    }

    // Android onDestroy method called right before the application is closed.
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void initializeComponents() {
        startAWSConnection();

    }

    private void startAWSConnection() {
        AWSConnection currentAWSConnection = AWSConnection.getInstance(this);
        if (InternetUtils.hasInternetConnection(this)) {
            currentAWSConnection.getConnection();
        }
    }
}