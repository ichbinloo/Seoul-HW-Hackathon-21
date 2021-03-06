package com.example.hackathon.BackEnd;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.example.hackathon.MainActivity;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.UUID;
import com.example.hackathon.R;

public class AWSConnection {

    static final String LOG_TAG = AWSConnection.class.getCanonicalName();
    // Amazon Cognito Set-up
    private static final String COGNITO_POOL_ID = "ap-northeast-2:b1e57af4-a57e-4501-af00-6f7e04b638e9";
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "ancun86kxagz2-ats.iot.ap-northeast-2.amazonaws.com";
    private static final String AWS_IOT_POLICY_NAME = "";
    private CognitoCachingCredentialsProvider cachingCredentialsProvider;
    private AWSIotMqttManager mqttManager;
    private static final Regions MY_REGION = Regions.AP_NORTHEAST_2;
    private String clientID, keystorePath, keystoreName, keystorePassword;
    private AWSIotClient awsIotClient;
    public String message;

    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";

    private Context context;
    private MainActivity mainActivity;
    @SuppressLint("StaticFieldLeak")
    private static AWSConnection managerInstance;
    private KeyStore clientKeyStore = null;
    private String certificateId;
    private boolean connected;


    private AWSConnection(Context context) {
        mainActivity = (MainActivity) context;
        this.context = context;
    }

    public static AWSConnection getInstance(Context context) {
        if (managerInstance == null) {
            managerInstance = new AWSConnection(context);
        }
        return managerInstance;
    }

    public void cognitoAuth(){
        // Generate the client's id & credentials provider
        clientID = UUID.randomUUID().toString();
        cachingCredentialsProvider = new CognitoCachingCredentialsProvider(
                context,COGNITO_POOL_ID,MY_REGION);
        Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientID,CUSTOMER_SPECIFIC_ENDPOINT);
        mqttManager.setKeepAlive(10); // send pings every 10 seconds

        // Set Last Will and Testament for MQTT. On an unclean disconnect, AWS IoT will publish
        // this message to alert other clients
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        // IoT Client (for creation of certificate if needed)
        awsIotClient = new AWSIotClient(cachingCredentialsProvider);
        awsIotClient.setRegion(region);

        // Set-up Keystore
        keystorePath = context.getFilesDir().getPath();
        keystoreName = KEYSTORE_NAME;
        keystorePassword = KEYSTORE_PASSWORD;
        certificateId = CERTIFICATE_ID;

        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                        keystoreName, keystorePassword)) {
                    Log.i(LOG_TAG, "Certificate " + certificateId
                            + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
                } else {
                    Log.i(LOG_TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(LOG_TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
        }

        if (clientKeyStore == null) {
            Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create a new private key and certificate. This call creates both on the server
                        // and returns them to the device.
                        CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                                new CreateKeysAndCertificateRequest();
                        createKeysAndCertificateRequest.setSetAsActive(true);
                        final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                        createKeysAndCertificateResult =
                                awsIotClient.createKeysAndCertificate(
                                        createKeysAndCertificateRequest);
                        Log.i(LOG_TAG,
                                "Cert ID: " +
                                        createKeysAndCertificateResult.getCertificateId() +
                                        " created.");

                        // store in keystore for use in MQTT client and saved as alias "default"
                        // so a new certificate isn't generated each run of this application
                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                createKeysAndCertificateResult.getCertificatePem(),
                                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                keystorePath, keystoreName, keystorePassword);

                        // load keystore from file into memory to pass on connection
                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                keystorePath, keystoreName, keystorePassword);

                        // Attach a policy to the newly created certificate.
                        // Assuming that the policy was already created in AWS IoT and we are now
                        // just attaching it to the certificate.
                        AttachPrincipalPolicyRequest policyAttachRequest =
                                new AttachPrincipalPolicyRequest();
                        policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
                        policyAttachRequest.setPrincipal(createKeysAndCertificateResult.getCertificateArn());
                        awsIotClient.attachPrincipalPolicy(policyAttachRequest);
                        getConnection();

                    } catch (Exception e) {
                        Log.e(LOG_TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                    }
                }
            }).start();
        }
    }

    public void getConnection(){
        cognitoAuth();
        connected = false;

        try {
            if (clientKeyStore != null) {
                mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(final AWSIotMqttClientStatus status,
                                                final Throwable throwable) {
                        Log.d(LOG_TAG, "Status = " + String.valueOf(status));
                        if (status == AWSIotMqttClientStatus.Connected) {
                            connected = true;
                            subscribeToTopic();
                        }
                    }
                });
            } else {
                ToastMessage.setToastMessage(context, mainActivity.getResources().getString(R.string.try_connect),
                        Toast.LENGTH_LONG);
            }
        } catch (Exception error) {
            Log.e(LOG_TAG,"Connection fails.",error);
        }
    }

    public void subscribeToTopic() {
        try {
            mqttManager.subscribeToTopic("test/dt/stm32l475e/sensor-data/topic",
                    AWSIotMqttQos.QOS0 /* Quality of Service */, new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(final String topic, final byte[] data) {
                            message = new String(data, StandardCharsets.UTF_8);
                            Log.d(LOG_TAG, "Message received: " + message);

                            if (message.equals("falling")) {

                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Subscription error: ", e);
        }
    }

    public void disconnectAWS() {
        try {
            mqttManager.disconnect();
            managerInstance = null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Disconnect error.", e);
        }
    }

    public boolean isConnect(){
        return connected;
    }

}
