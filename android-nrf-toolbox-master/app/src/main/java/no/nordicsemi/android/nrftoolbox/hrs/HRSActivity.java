/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package no.nordicsemi.android.nrftoolbox.hrs;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.achartengine.GraphicalView;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.UUID;
import java.util.Calendar;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.nrftoolbox.FeaturesActivity;
import no.nordicsemi.android.nrftoolbox.R;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileActivity;
import no.nordicsemi.android.nrftoolbox.hrs.settings.SettingsActivity;

/**
 * HRSActivity is the main Heart rate activity. It implements HRSManagerCallbacks to receive callbacks from HRSManager class. The activity supports portrait and landscape orientations. The activity
 * uses external library AChartEngine to show real time graph of HR values.
 */
// TODO The HRSActivity should be rewritten to use the service approach, like other do.
public class HRSActivity extends BleProfileActivity implements HRSManagerCallbacks, View.OnClickListener {
	@SuppressWarnings("unused")
	private final String TAG = "HRSActivity";

	private final static String GRAPH_STATUS = "graph_status";
	private final static String GRAPH_COUNTER = "graph_counter";
	private final static String HR_VALUE = "hr_value";

	private final static int MAX_HR_VALUE = 65535;
	private final static int MIN_POSITIVE_VALUE = 0;
	private final static int REFRESH_INTERVAL = 1000; // 1 second interval

	private Handler mHandler = new Handler();

	private boolean isGraphInProgress = false;

	private TextView mHRSValue, mHRSPosition;

	private int    mHrmValue = 0;
	private String mHrmPosition;
	private int mCounter = 0;

	final String HTS_KEY_COUNT = "HTS_COUNT";
	final String HRS_KEY_COUNT = "HRS_COUNT";
	final String HTS_KEY_VAL_PREFIX = "HTS_VAL_";
	final String HRS_KEY_VAL_PREFIX = "HRS_VAL_";
	private LineChart mChart;
	int[] dataArray;
	float[] HTArray;
	int maxDatasize = 2592000; /* 30 days */

	// Save state
	String PreferenceKey = "SavedKey";

	private String mqttEventTopic   = "iot-2/evt/status/fmt/json";
	private int    mqttKeepAlive    = 10;                          /* in sec */

	private String mqttHostName;
	private String mqttClientID;
	private String mqttDeviceName;
	private String mqttAuthMethod;
	private String mqttAuthToken;

	private static MqttAndroidClient  mqttClient  = null;
	private static MqttConnectOptions mqttOptions = null;
	private static CountDownTimer     publishTimer;

	private boolean isUploading = false;

	Button uploadDataButton;
//	Button connectServerButton;
	Button changeTypeButton;

	@Override
	protected void onCreateView(final Bundle savedInstanceState) {
		setContentView(R.layout.activity_feature_hrs);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		//connectServerButton = findViewById(R.id.action_mqtt_connect);
		uploadDataButton    = findViewById(R.id.action_upload);
		changeTypeButton    = findViewById(R.id.action_change_type);

		SharedPreferences prefs  = getSharedPreferences(PreferenceKey, MODE_PRIVATE);
		mqttDeviceName = prefs.getString("SAVE_INPUT_DEVICE_NAME", null);
		mqttAuthMethod = prefs.getString("SAVE_INPUT_AUTH_METHOD", null);
		mqttAuthToken  = prefs.getString("SAVE_INPUT_AUTH_TOKEN", null);
		mqttHostName   = prefs.getString("SAVE_MQTT_HOST", null);
		mqttClientID   = prefs.getString("SAVE_CLIENT_ID", null);

		boolean redrawGraph = false;
		int savedDataSize = prefs.getInt(HRS_KEY_COUNT, 0);
		if (savedDataSize > 0) {
			redrawGraph = true;
		}

		if (checkMQTTConnectStatus()) {
			//connectServerButton.setText(R.string.action_mqtt_disconnect);
			isUploading = prefs.getBoolean("SAVE_UPLOADING_STATE", false);
			if (isUploading) {
				uploadDataButton.setText(R.string.action_uploading);
			}
			else {
				uploadDataButton.setText(R.string.action_uploading);
			}
		}
		else {
			//connectServerButton.setText(R.string.action_mqtt_connect);
			uploadDataButton.setText(R.string.action_upload);
		}

		//connectServerButton.setOnClickListener(this);
		uploadDataButton.setOnClickListener(this);
		changeTypeButton.setOnClickListener(this);

		setGUI();
		if (redrawGraph) {
			dataArray = new int[savedDataSize];
			for (int i = 0; i < savedDataSize; i++) {
				dataArray[i] = prefs.getInt(HRS_KEY_VAL_PREFIX + i, 0);
			}
			updateGraph(dataArray);
			mChart.invalidate();
		}
	}

	private boolean checkMQTTConnectStatus() {
		if (mqttClient == null) {
			return false;
		}
		else if (!mqttClient.isConnected()) {
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
				Toast.makeText(HRSActivity.this, "Connection Lost!", Toast.LENGTH_LONG).show();
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
					String payload = "{" + "\"Client ID\":" + "\"" + mqttClientID + "\"" + "}";
					//connectServerButton.setText(R.string.action_mqtt_disconnect);
					mqttPublish(payload);
					Toast.makeText(HRSActivity.this, "Đã kết nối với server", Toast.LENGTH_LONG).show();
					uploadClicked();
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					Toast.makeText(HRSActivity.this, "Kết nối thất bại", Toast.LENGTH_LONG).show();
				}
			});
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	private void mqttDisconnect() {
		try {
			IMqttToken disconToken = mqttClient.disconnect();
			disconToken.setActionCallback(new IMqttActionListener() {
				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					//connectServerButton.setText(R.string.action_mqtt_connect);
					Toast.makeText( HRSActivity.this, "Đã ngắt kết nối với server", Toast.LENGTH_LONG).show();
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

				}
			});
		} catch (MqttException e) {
			e.printStackTrace();
		}

	}

	private void mqttPublish(String payload) {
		try {
			mqttClient.publish(mqttEventTopic, payload.getBytes(), 0, false);
		} catch (MqttException e) {
			e.printStackTrace();
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

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.action_upload) {
			if (!checkValidInfo()) {
				Toast.makeText(this, "Vui lòng điền đẩy đủ cấu hình mạng", Toast.LENGTH_LONG).show();
			}
			else {
				if (checkMQTTConnectStatus()) {
					/* Stop uploading first then disconnect with the broker */
					uploadClicked();
					if (!isUploading) {
						serverConnectClicked(v);
					}
				}
				else {
					/* Connect to the MQTT broker then start uploading */
					serverConnectClicked(v);
				}
			}
		}
		else if (v.getId() == R.id.action_change_type) {
			if (checkValidInfo()) {
				Toast.makeText(HRSActivity.this, "Thay đổi vị trí đo thành công", Toast.LENGTH_LONG).show();
				HRSManager.sendNewCharacteristicValue((byte)0);
			}
			else {
				Toast.makeText(HRSActivity.this, "Vui lòng kết nối với thiết bị", Toast.LENGTH_LONG).show();
			}
		}
	}

	void serverConnectClicked(final View view) {
		// Connect to server
		if (!checkMQTTConnectStatus()) {
			createMQTTClient();
			mqttConnect();
		}
		else {
			if (isUploading) {
				Toast.makeText(this, "Stop uploading", Toast.LENGTH_LONG).show();
			}
			else {
				mqttDisconnect();
			}
		}
	}

	void uploadClicked() {
		// Start uploading
		if (checkMQTTConnectStatus()) {
			if (!isUploading) {
				isUploading  = true;
				uploadDataButton.setText(R.string.action_uploading);
				String timePayload = "{\"d\":{" + "\"Time value\":" + String.valueOf(7000) + "}}";
				mqttPublish(timePayload);
				Toast.makeText(HRSActivity.this, "Bắt đầu gửi dữ liệu", Toast.LENGTH_LONG).show();
				publishTimer = new CountDownTimer(11000, 1000) {
					@Override
					public void onTick(long millisUntilFinished) {
					}

					@Override
					public void onFinish() {
//						String SensorValues = "{\"d\":{" + "\"Heart Rate value\":" + String.valueOf(mHRValue) + ","
//								+ "\"Body temperature\":" + String.valueOf(mHTSValue) + "}}";
						String SensorValues = "{\"d\":{" + "\"Heart Rate value\":" + String.valueOf(mHrmValue) + "}}";
						mqttPublish(SensorValues);
						publishTimer.start();
					}
				}.start();
			}
			else {
				publishTimer.cancel();
				isUploading = false;
				String timePayload = "{\"d\":{" + "\"Time value\":" + String.valueOf(8000) + "}}";
				mqttPublish(timePayload);
				Toast.makeText(HRSActivity.this, "Ngưng gửi dữ liệu", Toast.LENGTH_LONG).show();
				uploadDataButton.setText(R.string.action_upload);
			}
		}
	}

	private void SaveUploadState() {
		SharedPreferences.Editor editor = getSharedPreferences(PreferenceKey, MODE_PRIVATE).edit();
		editor.putBoolean("SAVE_UPLOADING_STATE", isUploading);
		int dataSize;
		if (dataArray != null) {
			dataSize = dataArray.length;
			editor.putInt(HRS_KEY_COUNT, dataSize);
			for (int i = 0; i < dataSize; i++) {
				editor.putInt(HRS_KEY_VAL_PREFIX + i, dataArray[i]);
			}
		}

		editor.apply();
	}

	private void setGUI() {
		mHRSValue = findViewById(R.id.text_hrs_value);
		mHRSPosition = findViewById(R.id.text_hrs_position);

		mChart = findViewById(R.id.chart1);
		mChart.setDrawGridBackground(false);

		// no description text
		mChart.getDescription().setEnabled(false);

		// enable touch gestures
		mChart.setTouchEnabled(true);

		// enable scaling and dragging
		mChart.setDragEnabled(true);
		mChart.setScaleEnabled(true);

		// if disabled, scaling can be done on x- and y-axis separately
		mChart.setPinchZoom(true);

		mChart.getAxisRight().setEnabled(false);

		showGraph();
	}

	private void showGraph() {
		LimitLine upperLimit = new LimitLine(200, "Nhịp tim tối đa");
		upperLimit.setLineWidth(4f);
		upperLimit.enableDashedLine(10f, 10f, 0f);
		upperLimit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
		upperLimit.setLineColor(Color.RED);
		upperLimit.setTextSize(12f);

		LimitLine lowerLimit = new LimitLine(65, "Nhịp tim tối thiểu");
		lowerLimit.setLineWidth(4f);
		lowerLimit.enableDashedLine(10f, 10f, 0f);
		lowerLimit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
		lowerLimit.setLineColor(Color.BLUE);
		lowerLimit.setTextSize(12f);

		YAxis leftAxis = mChart.getAxisLeft();
		leftAxis.removeAllLimitLines();
		leftAxis.addLimitLine(upperLimit);
		leftAxis.addLimitLine(lowerLimit);

		// limit lines are drawn behind data (and not on top)
		leftAxis.setDrawLimitLinesBehindData(true);
	}

	@Override
	protected void onStart() {
		super.onStart();

		final Intent intent = getIntent();
		if (!isDeviceConnected() && intent.hasExtra(FeaturesActivity.EXTRA_ADDRESS)) {
			final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(getIntent().getByteArrayExtra(FeaturesActivity.EXTRA_ADDRESS));
			onDeviceSelected(device, device.getName());

			intent.removeExtra(FeaturesActivity.EXTRA_APP);
			intent.removeExtra(FeaturesActivity.EXTRA_ADDRESS);
		}
	}

	@Override
	protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		isGraphInProgress = savedInstanceState.getBoolean(GRAPH_STATUS);
		mCounter = savedInstanceState.getInt(GRAPH_COUNTER);
		mHrmValue = savedInstanceState.getInt(HR_VALUE);

		if (isGraphInProgress)
			startShowGraph();
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putBoolean(GRAPH_STATUS, isGraphInProgress);
		outState.putInt(GRAPH_COUNTER, mCounter);
		outState.putInt(HR_VALUE, mHrmValue);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		SaveUploadState();
		stopShowGraph();
	}

	@Override
	protected int getLoggerProfileTitle() {
		return R.string.hrs_feature_title;
	}

	@Override
	protected int getAboutTextId() {
		return R.string.hrs_about_text;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.settings_and_about, menu);
		return true;
	}

	@Override
	protected boolean onOptionsItemSelected(final int itemId) {
		switch (itemId) {
			case R.id.action_settings:
				final Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				break;
		}
		return true;
	}

	@Override
	protected int getDefaultDeviceName() {
		return R.string.hrs_default_name;
	}

	@Override
	protected UUID getFilterUUID() {
		return HRSManager.HR_SERVICE_UUID;
	}

	private void updateGraph(int[] range) {
		ArrayList<Entry> values = new ArrayList<Entry>();

		for (int i = 0; i < range.length; i++) {
			values.add(new Entry(i, range[i]));
		}

		LineDataSet HRDataSet;

		if (mChart.getData() != null &&
			mChart.getData().getDataSetCount() > 0) {

			HRDataSet = (LineDataSet) mChart.getData().getDataSetByIndex(0);
			HRDataSet.setValues(values);
			mChart.getData().notifyDataChanged();
			mChart.notifyDataSetChanged();
		}
		else {
			// create a dataset and give it a type
			HRDataSet = new LineDataSet(values, "Nhịp tim hiện tại");
			HRDataSet.setLineWidth(2f);
			HRDataSet.setColor(Color.RED);

			ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
			dataSets.add(HRDataSet);

			LineData data = new LineData(dataSets);
			data.setValueTextSize(8f);
			data.setValueTextColor(Color.BLUE);

			// set data
			mChart.setData(data);
		}

	}

	private Runnable mRepeatTask = new Runnable() {
		@Override
		public void run() {
			if (mHrmValue > 0) {
				if (dataArray == null) {
					dataArray = new int[1];
					dataArray[0] = mHrmValue;
				}
				else {
					int[] newDataArray = new int[dataArray.length + 1];
					for (int i = 0; i < dataArray.length; i++) {
						newDataArray[i] = dataArray[i];
					}
					newDataArray[dataArray.length] = mHrmValue;
					dataArray = newDataArray;
					updateGraph(dataArray);
					mChart.invalidate();
				}
			}
			if (isGraphInProgress)
				mHandler.postDelayed(mRepeatTask, REFRESH_INTERVAL);
		}
	};

	void startShowGraph() {
		isGraphInProgress = true;
		mRepeatTask.run();
	}

	void stopShowGraph() {
		isGraphInProgress = false;
		mHandler.removeCallbacks(mRepeatTask);
	}

	@Override
	protected BleManager<HRSManagerCallbacks> initializeManager() {
		final HRSManager manager = HRSManager.getInstance(getApplicationContext());
		manager.setGattCallbacks(this);
		return manager;
	}

	private void setHRSValueOnView(final int value) {
		runOnUiThread(() -> {
			if (value >= MIN_POSITIVE_VALUE && value <= MAX_HR_VALUE) {
				mHRSValue.setText(Integer.toString(value));
			} else {
				mHRSValue.setText(R.string.not_available_value);
			}
		});
	}

	private void setHRSPositionOnView(final String position) {
		runOnUiThread(() -> {
			if (position != null) {
				mHRSPosition.setText(position);
				mHrmPosition = position;
			} else {
				mHRSPosition.setText(R.string.not_available);
			}
		});
	}

	@Override
	public void onServicesDiscovered(final BluetoothDevice device, final boolean optionalServicesFound) {
		// this may notify user or show some views
	}

	@Override
	public void onDeviceReady(final BluetoothDevice device) {
		startShowGraph();
	}

	@Override
	public void onHRSensorPositionFound(final BluetoothDevice device, final String position, final byte intPosition) {
		setHRSPositionOnView(position);
	}

	@Override
	public void onCharacteristicValueWritten(final BluetoothDevice device, String stringValue, byte value) {
		setHRSPositionOnView(stringValue);
	}

	@Override
	public void onHRValueReceived(final BluetoothDevice device, int value) {
		mHrmValue = value;
		setHRSValueOnView(mHrmValue);
	}

	@Override
	public void onDeviceDisconnected(final BluetoothDevice device) {
		super.onDeviceDisconnected(device);
		runOnUiThread(() -> {
			mHRSValue.setText(R.string.not_available_value);
			mHRSPosition.setText(R.string.not_available);
			stopShowGraph();
		});
	}

	@Override
	protected void setDefaultUI() {
		mHRSValue.setText(R.string.not_available_value);
		mHRSPosition.setText(R.string.not_available);
		clearGraph();
	}

	private void clearGraph() {
	}
}
