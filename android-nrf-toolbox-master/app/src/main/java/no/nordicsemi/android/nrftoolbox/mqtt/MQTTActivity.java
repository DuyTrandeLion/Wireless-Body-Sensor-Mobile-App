package no.nordicsemi.android.nrftoolbox.mqtt;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.CountDownTimer;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import no.nordicsemi.android.nrftoolbox.R;
import no.nordicsemi.android.nrftoolbox.template.TemplateActivity;

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
    String userFullName;
    int    userAge;
    String userID;
    String userFone;
    int    mqttPort;

    public boolean isUploading = false;

    private static MqttAndroidClient  mqttClient  = null;
    private static MqttConnectOptions mqttOptions = null;
    private static CountDownTimer     publishTimer;

    // Textview
    EditText txtInputDeviceName;
    EditText txtInputAuthMethod;
    EditText txtInputAuthToken;
    EditText txtInputUserFullName;
    EditText txtInputUserAge;
    EditText txtInputUserID;
    EditText txtInputUserFone;
    TextView txtInputPort;
    TextView displayTemperature;

    // Button
//    Button connectServerButton;
//    Button uploadDataButton;
    Button saveUserDataButton;

    // Save state
    String PreferenceKey = "SavedKey";

    // Test data
    long publishCounterValue = 0;

    // Upload data
//    Intent mIntent;
//    int UploadedHeartRate;
//    float UploadedTemperature;

    public SharedPreferences sharedMeasuredValues;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SaveUserInputData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SaveUserInputData();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mqtt);

        txtInputDeviceName  = findViewById(R.id.input_device_name);
        txtInputAuthMethod  = findViewById(R.id.input_auth_method);
        txtInputAuthToken   = findViewById(R.id.input_auth_token);
//        txtInputPort        = findViewById(R.id.input_mqtt_port);
        displayTemperature  = findViewById(R.id.temperature_display);

        txtInputUserFullName  = findViewById(R.id.input_user_full_name_token);
        txtInputUserAge       = findViewById(R.id.input_user_age_token);
        txtInputUserID        = findViewById(R.id.input_user_id_token);
        txtInputUserFone      = findViewById(R.id.input_user_fone_token);


//        connectServerButton = findViewById(R.id.action_server_connect);
//        uploadDataButton    = findViewById(R.id.action_upload);
        saveUserDataButton  = findViewById(R.id.action_save_user_config_data);

        if (savedInstanceState != null) {
            txtInputDeviceName.setText("InputDeviceNameState");
            txtInputAuthMethod.setText("InputAuthMethodState");
            txtInputAuthToken.setText("InputAuthTokenState");
        }

        SharedPreferences prefs  = getSharedPreferences(PreferenceKey, MODE_PRIVATE);
        txtInputDeviceName.setText(prefs.getString("SAVE_INPUT_DEVICE_NAME", null));
        txtInputAuthMethod.setText(prefs.getString("SAVE_INPUT_AUTH_METHOD", null));
        txtInputAuthToken.setText(prefs.getString("SAVE_INPUT_AUTH_TOKEN", null));

        txtInputUserFullName.setText(prefs.getString("SAVE_USER_FULL_NAME", null));
        userAge = prefs.getInt("SAVE_USER_AGE", 0);
        txtInputUserAge.setText(String.valueOf(userAge));
        txtInputUserID.setText(prefs.getString("SAVE_USER_ID", null));
        txtInputUserFone.setText(prefs.getString("SAVE_USER_FONE", null));

        //isUploading = prefs.getBoolean("SAVE_UPLOADING_STATE", false);

//        if (checkMQTTConnectStatus()) {
//            connectServerButton.setText(R.string.action_disconnect);
//            isUploading = prefs.getBoolean("SAVE_UPLOADING_STATE", false);
//            if (isUploading) {
//                uploadDataButton.setText(R.string.action_uploading);
//            }
//            else {
//                uploadDataButton.setText(R.string.action_uploading);
//            }
//        }
//        else {
//            connectServerButton.setText(R.string.action_connect);
//            uploadDataButton.setText(R.string.action_upload);
//        }
//
//        connectServerButton.setOnClickListener(this);
//        uploadDataButton.setOnClickListener(this);
        saveUserDataButton.setOnClickListener(this);
    }

    private void SaveUserInputData() {
        if (txtInputDeviceName.getText().toString() != null &
            txtInputAuthMethod.getText().toString() != null &
            txtInputAuthToken.getText().toString() != null) {
            SharedPreferences.Editor editor = getSharedPreferences(PreferenceKey, MODE_PRIVATE).edit();
            editor.putString("SAVE_INPUT_DEVICE_NAME", txtInputDeviceName.getText().toString());
            editor.putString("SAVE_INPUT_AUTH_METHOD", txtInputAuthMethod.getText().toString());
            editor.putString("SAVE_INPUT_AUTH_TOKEN", txtInputAuthToken.getText().toString());
            editor.putString("SAVE_MQTT_HOST", mqttHostName);
            editor.putString("SAVE_CLIENT_ID", mqttClientID);

            editor.putString("SAVE_USER_FULL_NAME", userFullName);
            editor.putInt("SAVE_USER_AGE", userAge);
            editor.putString("SAVE_USER_ID", userID);
            editor.putString("SAVE_USER_FONE", userFone);

//            editor.putBoolean("SAVE_UPLOADING_STATE", isUploading);

            editor.apply();
        }
    }

    @Override
    public void onClick(View v) {
        // TODO
//        mqttClientID   = "d:" + mqttOrganization + ":" + mqttDeviceType + ":" + mqttDeviceName;
//        SaveUserInputData();
//        if (v.getId() == R.id.action_server_connect) {
//            mqttDeviceName = txtInputDeviceName.getText().toString();
//            mqttAuthMethod = txtInputAuthMethod.getText().toString();
//            mqttAuthToken  = txtInputAuthToken.getText().toString();
//            if (!checkValidInfo()) {
//                Toast.makeText(MQTTActivity.this, "Please fill in the blanks first", Toast.LENGTH_LONG).show();
//            }
//            else {
//                mqttClientID   = "d:" + mqttOrganization + ":" + mqttDeviceType + ":" + mqttDeviceName;
//                serverConnectClicked(v);
//            }
//        }
//        else if (v.getId() == R.id.action_upload) {
//            uploadClicked(v);
//        }
//        else if (v.getId() == R.id.action_save_user_config_data) {
//            Toast.makeText(MQTTActivity.this, "Lưu thành công", Toast.LENGTH_LONG).show();
//            SaveUserInputData();
//            saveUserDataClicked(v);
//        }
        if (v.getId() == R.id.action_save_user_config_data) {
            mqttDeviceName = txtInputDeviceName.getText().toString();
            mqttAuthMethod = txtInputAuthMethod.getText().toString();
            mqttAuthToken  = txtInputAuthToken.getText().toString();

            userFullName     = txtInputUserFullName.getText().toString();
            String stringAge = txtInputUserAge.getText().toString();
            userAge          = Integer.parseInt(stringAge);
            userID           = txtInputUserID.getText().toString();
            userFone         = txtInputUserFone.getText().toString();

            if (!checkValidInfo()) {
                Toast.makeText(MQTTActivity.this, "Vui lòng điền đủ thông tin", Toast.LENGTH_LONG).show();
            }
            else {
                mqttClientID   = "d:" + mqttOrganization + ":" + mqttDeviceType + ":" + mqttDeviceName;
                SaveUserInputData();
                Toast.makeText(MQTTActivity.this, "Lưu thành công!", Toast.LENGTH_LONG).show();
            }
        }
    }

    // invoked when the activity may be temporarily destroyed, save the instance state here
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (txtInputDeviceName.getText().toString() != null &
            txtInputAuthMethod.getText().toString() != null &
            txtInputAuthToken.getText().toString() != null) {
            outState.putString("InputDeviceNameState", txtInputDeviceName.getText().toString());
            outState.putString("InputAuthMethodState", txtInputAuthMethod.getText().toString());
            outState.putString("InputAuthTokenState", txtInputAuthToken.getText().toString());

            outState.putBoolean("PublishState", isUploading);
        }
    }

    private boolean checkValidInfo() {
        if (mqttDeviceName.equals("")) {
            return false;
        }
        if (mqttAuthMethod.equals("")) {
            return false;
        }
        if (mqttAuthToken.equals("")) {
            return false;
        }
        return true;
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

//    private void mqttConnect() {
//        try {
//            IMqttToken MQTTToken = mqttClient.connect(mqttOptions);
//            MQTTToken.setActionCallback(new IMqttActionListener() {
//                @Override
//                public void onSuccess(IMqttToken asyncActionToken) {
//                    String payload = "{" + "\"Client ID\":" + "\"" + mqttClientID + "\"" + "}";
//                    mqttPublish(payload);
//                    connectServerButton.setText(R.string.action_disconnect);
//                    Toast.makeText(MQTTActivity.this, "Đã kết nối với server", Toast.LENGTH_LONG).show();
//                }
//
//                @Override
//                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                    Toast.makeText(MQTTActivity.this, "Kết nối thất bại", Toast.LENGTH_LONG).show();
//                }
//            });
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void mqttDisconnect() {
//        try {
//            IMqttToken disconToken = mqttClient.disconnect();
//            disconToken.setActionCallback(new IMqttActionListener() {
//                @Override
//                public void onSuccess(IMqttToken asyncActionToken) {
//                    connectServerButton.setText(R.string.action_connect);
//                    Toast.makeText(MQTTActivity.this, "Đã ngắt kết nối với server", Toast.LENGTH_LONG).show();
//                }
//
//                @Override
//                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//
//                }
//            });
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }
//
//    }

    private void mqttPublish(String payload) {
        try {
            mqttClient.publish(mqttEventTopic, payload.getBytes(), 0, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public boolean checkMQTTConnectStatus() {
        if (mqttClient == null) {
            return false;
        }
        else if (!mqttClient.isConnected()) {
            return false;
        }
        return true;
    }

//    void serverConnectClicked(final View view) {
//        // Connect to server
//        if (!checkMQTTConnectStatus()) {
//            createMQTTClient();
//            mqttConnect();
//        }
//        else {
//            if (isUploading) {
//                Toast.makeText(MQTTActivity.this, "Please stop uploading first", Toast.LENGTH_LONG).show();
//            }
//            else {
//                mqttDisconnect();
//            }
//        }
//    }

    void saveUserDataClicked(final View view) {
        SaveUserInputData();
    }
        
//    void uploadClicked(final View view) {
//        // Start uploading
//        if (checkMQTTConnectStatus()) {
//            if (!isUploading) {
//                isUploading  = true;
//                uploadDataButton.setText(R.string.action_uploading);
//                String timePayload = "{\"d\":{" + "\"Time value\":" + String.valueOf(7000) + "}}";
//                mqttPublish(timePayload);
//                Toast.makeText(MQTTActivity.this, "Bắt đầu gửi dữ liệu", Toast.LENGTH_LONG).show();
//                publishTimer = new CountDownTimer(6000, 1000) {
//                    @Override
//                    public void onTick(long millisUntilFinished) {
//                        publishCounterValue++;
////                        sharedMeasuredValues = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
////                        float getTemperatureValue = sharedMeasuredValues.getFloat("SHARED_TEMPERATURE_VALUE", 0);
////                        displayTemperature.setText(String.valueOf(getTemperatureValue));
//                    }
//
//                    @Override
//                    public void onFinish() {
//                        Intent mIntent = getIntent();
//                        float UploadedTemperature = mIntent.getFloatExtra("SHARE_TEMPERATURE", 0);
//                        displayTemperature.setText(String.valueOf(UploadedTemperature));
//                        String timePayload = "{\"d\":{" + "\"Time value\":" + String.valueOf(publishCounterValue) + ","
//                                + "\"Body temperature\":" + String.valueOf(UploadedTemperature) + "}}";
//                        mqttPublish(timePayload);
//                        // Test
//                        if ((publishCounterValue % 100) == 0) {
//                            publishCounterValue = 0;
//                        }
//                        publishTimer.start();
//                    }
//                }.start();
//            }
//            else {
//                publishTimer.cancel();
//                isUploading = false;
//                String timePayload = "{\"d\":{" + "\"Time value\":" + String.valueOf(8000) + "}}";
//                mqttPublish(timePayload);
//                Toast.makeText(MQTTActivity.this, "Ngưng gửi dữ liệu", Toast.LENGTH_LONG).show();
//                uploadDataButton.setText(R.string.action_upload);
//            }
//        }
//    }
}
