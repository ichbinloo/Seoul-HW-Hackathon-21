package com.example.hackathon;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.hackathon.BackEnd.*;

public class MainActivity extends AppCompatActivity {
    // Checking Internet connection
    protected InternetConnection internetConnection;
    protected AWSConnection awsConnection;
    private Button status; private Button heart_rate;

    // Android onCreate method to call actions when the Activity is launched.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeComponents();
        checkInternetConnection();

        status = findViewById(R.id.emergencyRate);
        status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEmergencyStatus();
            }
        });
        heart_rate = findViewById(R.id.heartRate);
        heart_rate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHeartRate();
            }
        });
    }

    // Android onDestroy method called right before the application is closed.
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(internetConnection);
    }

    public void initializeComponents() {
        startAWSConnection();
    }

    private void checkInternetConnection(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        internetConnection = new InternetConnection();
        registerReceiver(internetConnection, intentFilter);
    }

    private void startAWSConnection() {
        AWSConnection currentAWSConnection = AWSConnection.getInstance(this);
        if (InternetUtils.hasInternetConnection(this)) {
            currentAWSConnection.getConnection();
        } else {
            finish();
        }
    }

    public void openEmergencyStatus() {
        Intent intent = new Intent (this, EmergencyStatus.class);
        startActivity(intent);
    }

    public void openHeartRate() {
        Intent intent = new Intent (this, HeartRate.class);
        startActivity(intent);
    }
}