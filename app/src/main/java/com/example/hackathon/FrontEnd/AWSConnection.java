package com.example.hackathon.FrontEnd;

import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIot;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.example.hackathon.MainActivity;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;

import java.security.KeyStore;
import java.util.UUID;

public class AWSConnection {

    static final String LOG_TAG = AWSConnection.class.getCanonicalName();
    // Amazon Cognito Set-up
    private static final String COGNITO_POOL_ID = "ap-northeast-2:b1e57af4-a57e-4501-af00-6f7e04b638e9";
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "";
    private static final String AWS_IOT_POLICY_NAME = "";
    private CognitoCachingCredentialsProvider cachingCredentialsProvider;
    private AWSIotMqttManager mqttManager;
    private static final Regions MY_REGION = Regions.AP_NORTHEAST_2;
    private String clientID, keystorePath, keystoreName, keystorePassword;
    private AWSIotClient awsIotClient;

    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";

    private Context context;
    private MainActivity mainActivity;
    private static AWSConnection managerInstance;
    private KeyStore clientKeyStore = null;
    private String certificateId;


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
                        // Create a new private key and certificate. This call
                        // creates both on the server and returns them to the
                        // device.
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

                        // store in keystore for use in MQTT client
                        // saved as alias "default" so a new certificate isn't
                        // generated each run of this application
                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                createKeysAndCertificateResult.getCertificatePem(),
                                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                keystorePath, keystoreName, keystorePassword);

                        // load keystore from file into memory to pass on
                        // connection
                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                keystorePath, keystoreName, keystorePassword);

                        // Attach a policy to the newly created certificate.
                        // This flow assumes the policy was already created in
                        // AWS IoT and we are now just attaching it to the
                        // certificate.
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
        try {

        } catch (Exception error) {
            Log.e(LOG_TAG,"Connection fails.",error);
        }
    }

}
