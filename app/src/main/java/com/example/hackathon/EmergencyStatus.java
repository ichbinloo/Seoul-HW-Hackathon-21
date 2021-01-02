package com.example.hackathon;

import androidx.appcompat.app.AppCompatActivity;
import com.example.hackathon.BackEnd.*;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

public class EmergencyStatus extends AppCompatActivity {
    private TextView healthStatus;
    private AWSConnection managerAWSConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_status);
    }

    public void setHealthStatus(final String data) {
        healthStatus = (TextView) findViewById(R.id.health_stat);
        new Thread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                if (data.equals("falling")) {
                    healthStatus.setText("Likely high");
                }
                else healthStatus.setText("Healthy");
            }
        });

    }

}