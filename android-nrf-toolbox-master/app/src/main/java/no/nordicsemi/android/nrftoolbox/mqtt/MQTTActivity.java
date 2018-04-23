package no.nordicsemi.android.nrftoolbox.mqtt;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import no.nordicsemi.android.nrftoolbox.R;

public class MQTTActivity extends AppCompatActivity implements View.OnClickListener {
    private String mqttDeviceType   = "HTC_FONE";
    private String mqttURL          = ".messaging.internetofthings.ibmcloud.com";
    private String mqttOrganization = "a4nvkh";
    private String mqttEventTopic   = "iot-2/evt/status/fmt/json";
    private int    mqttKeepAlive    = 10;                               /* in sec */

    String mqttHostName             = "tcp://" + mqttOrganization + mqttURL;
    String mqttClientID;

    String mqttDeviceName;
    String mqttAuthMethod;
    String mqttAuthToken;
    int    mqttPort;

    private static MqttAndroidClient mqttClient;
    private static MqttConnectOptions mqttOptions;

    // Textview
    TextView txtInputDeviceName;
    TextView txtInputAuthMethod;
    TextView txtInputAuthToken;
    TextView txtInputPort;

    // Button
    Button connectServerButton;
    Button uploadDataButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mqtt);

        txtInputDeviceName  = findViewById(R.id.input_device_name);
        txtInputAuthMethod  = findViewById(R.id.input_auth_method);
        txtInputAuthToken   = findViewById(R.id.input_auth_token);
        txtInputPort        = findViewById(R.id.input_mqtt_port);

        connectServerButton = findViewById(R.id.action_server_connect);
        uploadDataButton    = findViewById(R.id.action_upload);
        connectServerButton.setOnClickListener(this);
        uploadDataButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        // TODO
        if (v.getId() == R.id.action_server_connect) {
            mqttDeviceName = txtInputDeviceName.getText().toString();
            mqttAuthMethod = txtInputAuthMethod.getText().toString();
            mqttAuthToken  = txtInputAuthToken.getText().toString();
            mqttPort       = Integer.parseInt(txtInputPort.getText().toString());
            mqttClientID   = "d:" + mqttOrganization + ":" + mqttDeviceType + ":" + mqttDeviceName;
            serverConnectClicked(v);
        }
        else if (v.getId() == R.id.action_upload) {
            uploadClicked(v);
        }
    }

    private void createMQTTClient() {
        mqttClient = new MqttAndroidClient(this.getApplicationContext(), mqttHostName, mqttClientID);
        mqttOptions = new MqttConnectOptions();
        mqttOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
        mqttOptions.setUserName(mqttAuthMethod);
        mqttOptions.setPassword(mqttAuthToken.toCharArray());
        mqttOptions.setKeepAliveInterval(mqttKeepAlive);
    }

    private void setSubcribeCallback() {
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Toast.makeText(MQTTActivity.this, "Connection Lost!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    private void mqttConnect() {
        try {
            IMqttToken MQTTToken = mqttClient.connect(mqttOptions);
            MQTTToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(MQTTActivity.this, "Can't Connect", Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void mqttPublish(String message) {
        try {
            mqttClient.publish(mqttEventTopic, message.getBytes(), 0, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    void serverConnectClicked(final View view) {
        // Connect to server
        createMQTTClient();
        mqttConnect();
        String say_hello = "Hello from Duy";
//        mqttPublish(say_hello);
        int a = 7;
    }

    void uploadClicked(final View view) {
        // Start uploading
    }
}
