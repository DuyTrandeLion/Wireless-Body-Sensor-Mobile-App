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
package no.nordicsemi.android.nrftoolbox.template;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.support.annotation.NonNull;
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

import no.nordicsemi.android.nrftoolbox.R;
import no.nordicsemi.android.nrftoolbox.mqtt.MQTTActivity;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileService;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileServiceReadyActivity;
import no.nordicsemi.android.nrftoolbox.template.settings.SettingsActivity;
import no.nordicsemi.android.nrftoolbox.template.settings.SettingsFragment;

/**
 * Modify the Template Activity to match your needs.
 */
public class TemplateActivity extends BleProfileServiceReadyActivity<TemplateService.TemplateBinder> implements View.OnClickListener {
	@SuppressWarnings("unused")
	private final String TAG = "ReplaceHTSActivity";

	private static int UINT_ON_VIEW = SettingsFragment.SETTINGS_VARIANT_DEFAULT;

	private final static String GRAPH_STATUS = "graph_status";
	private final static String GRAPH_COUNTER = "graph_counter";
	private final static String HTS_VALUE = "hr_value";

	private final static int REFRESH_INTERVAL = 1000; // 1 second interval

	private Handler mHandler = new Handler();

	private boolean isGraphInProgress = false;

	float mHTSValue;
	int   mHRValue;

	// TODO change view references to match your need
	private TextView mValueView, mRHTSType;
	private TextView mValueUnitView;

	private LineChart mChart;
	float[] dataArray;

	int publishCounterValue;

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
	Button connectServerButton;

	@Override
	protected void onCreateView(final Bundle savedInstanceState) {
		// TODO modify the layout file(s). By default the activity shows only one field - the Heart Rate value as a sample
		setContentView(R.layout.activity_feature_template);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		connectServerButton = findViewById(R.id.action_mqtt_connect);
		uploadDataButton    = findViewById(R.id.action_upload);

		SharedPreferences prefs  = getSharedPreferences(PreferenceKey, MODE_PRIVATE);
		mqttDeviceName = prefs.getString("SAVE_INPUT_DEVICE_NAME", null);
		mqttAuthMethod = prefs.getString("SAVE_INPUT_AUTH_METHOD", null);
		mqttAuthToken  = prefs.getString("SAVE_INPUT_AUTH_TOKEN", null);
		mqttHostName   = prefs.getString("SAVE_MQTT_HOST", null);
		mqttClientID   = prefs.getString("SAVE_CLIENT_ID", null);

		if (checkMQTTConnectStatus()) {
			connectServerButton.setText(R.string.action_mqtt_disconnect);
			isUploading = prefs.getBoolean("SAVE_UPLOADING_STATE", false);
			if (isUploading) {
				uploadDataButton.setText(R.string.action_uploading);
			}
			else {
				uploadDataButton.setText(R.string.action_uploading);
			}
		}
		else {
			connectServerButton.setText(R.string.action_mqtt_connect);
			uploadDataButton.setText(R.string.action_upload);
		}



		connectServerButton.setOnClickListener(this);
		uploadDataButton.setOnClickListener(this);

		setGUI();
		startShowGraph();
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
				Toast.makeText(TemplateActivity.this, "Connection Lost!", Toast.LENGTH_LONG).show();
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
					connectServerButton.setText(R.string.action_mqtt_disconnect);
					mqttPublish(payload);
					Toast.makeText(TemplateActivity.this, "Đã kết nối với server", Toast.LENGTH_LONG).show();
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					Toast.makeText(TemplateActivity.this, "Kết nối thất bại", Toast.LENGTH_LONG).show();
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
					connectServerButton.setText(R.string.action_mqtt_connect);
					Toast.makeText( TemplateActivity.this, "Đã ngắt kết nối với server", Toast.LENGTH_LONG).show();
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
		if (v.getId() == R.id.action_mqtt_connect) {
			if (!checkValidInfo()) {
				Toast.makeText(this, "Vui lòng điền đẩy đủ cấu hình mạng", Toast.LENGTH_LONG).show();
			}
			else {
				serverConnectClicked(v);
			}
		}
		else if (v.getId() == R.id.action_upload) {
			uploadClicked(v);
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
				Toast.makeText(this, "Please stop uploading first", Toast.LENGTH_LONG).show();
			}
			else {
				mqttDisconnect();
			}
		}
	}

	void uploadClicked(final View view) {
		// Start uploading
		if (checkMQTTConnectStatus()) {
			if (!isUploading) {
				isUploading  = true;
				uploadDataButton.setText(R.string.action_uploading);
				String timePayload = "{\"d\":{" + "\"Time value\":" + String.valueOf(7000) + "}}";
				mqttPublish(timePayload);
				Toast.makeText(TemplateActivity.this, "Bắt đầu gửi dữ liệu", Toast.LENGTH_LONG).show();
				publishTimer = new CountDownTimer(11000, 1000) {
					@Override
					public void onTick(long millisUntilFinished) {
					}

					@Override
					public void onFinish() {
						String SensorValues = "{\"d\":{" + "\"Heart Rate value\":" + String.valueOf(mHRValue) + ","
								+ "\"Body temperature\":" + String.valueOf(mHTSValue) + "}}";
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
				Toast.makeText(TemplateActivity.this, "Ngưng gửi dữ liệu", Toast.LENGTH_LONG).show();
				uploadDataButton.setText(R.string.action_upload);
			}
		}
	}

	private void SaveUploadState() {
		SharedPreferences.Editor editor = getSharedPreferences(PreferenceKey, MODE_PRIVATE).edit();
		editor.putBoolean("SAVE_UPLOADING_STATE", isUploading);
		editor.apply();
	}

	void startShowGraph() {
		isGraphInProgress = true;
		mRepeatTask.run();

	}

	void stopShowGraph() {
		isGraphInProgress = false;
		mHandler.removeCallbacks(mRepeatTask);
	}

	private void setGUI() {
		// TODO assign your views to fields
		mValueView = findViewById(R.id.value);
		mRHTSType  = findViewById(R.id.type);
		mValueUnitView = findViewById(R.id.value_unit);

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

		drawDefaultGraph();
	}

	@Override
	protected void onInitialize(final Bundle savedInstanceState) {
		LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, makeIntentFilter());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		SaveUploadState();
		stopShowGraph();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
	}


	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
		super.onRestoreInstanceState(savedInstanceState, persistentState);

		isGraphInProgress = savedInstanceState.getBoolean(GRAPH_STATUS);
		int mCounter = savedInstanceState.getInt(GRAPH_COUNTER);
		mHTSValue = savedInstanceState.getFloat(HTS_VALUE);

		if (isGraphInProgress)
			startShowGraph();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(GRAPH_STATUS, isGraphInProgress);
		outState.putInt(GRAPH_COUNTER, dataArray.length);
		outState.putFloat(HTS_VALUE, mHTSValue);
	}

	private void drawDefaultGraph() {
		LimitLine upperLimit = new LimitLine(0, "");
		if (UINT_ON_VIEW == SettingsFragment.SETTINGS_VARIANT_C) {
			upperLimit = new LimitLine(43f, "Cao nhất");
		}
		else if (UINT_ON_VIEW == SettingsFragment.SETTINGS_VARIANT_F) {
			upperLimit = new LimitLine(111f, "Cao nhất");
		}
		upperLimit.setLineWidth(4f);
		upperLimit.enableDashedLine(10f, 10f, 0f);
		upperLimit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
		upperLimit.setLineColor(Color.RED);
		upperLimit.setTextSize(12f);

		LimitLine lowerLimit = new LimitLine(0, "");
		if (UINT_ON_VIEW == SettingsFragment.SETTINGS_VARIANT_C) {
			lowerLimit = new LimitLine(25f, "Thấp nhất");
		}
		else if (UINT_ON_VIEW == SettingsFragment.SETTINGS_VARIANT_F) {
			lowerLimit = new LimitLine(77f, "Thấp nhất");
		}
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
	protected void setDefaultUI() {
		// TODO clear your UI
		mValueView.setText(R.string.not_available_value);
		mRHTSType.setText(R.string.not_available_value);
		setUnits();
	}

	private void setUnits() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		final int unit = Integer.parseInt(preferences.getString(SettingsFragment.SETTINGS_TEMP_UNIT, String.valueOf(SettingsFragment.SETTINGS_VARIANT_DEFAULT)));
		UINT_ON_VIEW = unit;
		switch (unit) {
			case SettingsFragment.SETTINGS_VARIANT_C:
				mValueUnitView.setText(R.string.template_unit_celsius);
				break;
			case SettingsFragment.SETTINGS_VARIANT_F:
				mValueUnitView.setText(R.string.template_unit_fahrenheit);
				break;
			default: break;
		}
	}

	@Override
	protected void onServiceBinded(final TemplateService.TemplateBinder binder) {
		// not used
	}

	@Override
	protected void onServiceUnbinded() {
		// not used
	}

	@Override
	protected int getLoggerProfileTitle() {
		return R.string.template_feature_title;
	}

	@Override
	protected int getAboutTextId() {
		return R.string.template_about_text;
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
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
		return R.string.template_default_name;
	}

	@Override
	protected UUID getFilterUUID() {
		// TODO this method may return the UUID of the service that is required to be in the advertisement packet of a device in order to be listed on the Scanner dialog.
		// If null is returned no filtering is done.
		return TemplateManager.SERVICE_UUID;
	}

	@Override
	protected Class<? extends BleProfileService> getServiceClass() {
		return TemplateService.class;
	}

	@Override
	public void onServicesDiscovered(final BluetoothDevice device, final boolean optionalServicesFound) {
		// this may notify user or show some views
	}

	@Override
	public void onDeviceReady(final BluetoothDevice device) {
		startShowGraph();
	}

	private void setValueOnView(final float value, final String type) {
		// TODO assign the value to a view
		setUnits();
		float displayValue = 0;
		switch (UINT_ON_VIEW) {
			case SettingsFragment.SETTINGS_VARIANT_C:
				displayValue = value + (float)0.0;
				break;
			case SettingsFragment.SETTINGS_VARIANT_F:
				displayValue = value * (float)1.8 + (float)32.0;
				break;
			default: break;
		}

		mValueView.setText(String.valueOf(displayValue));
		mRHTSType.setText(String.valueOf(type));

		if (mHTSValue > 0) {
			if (dataArray == null) {
				dataArray = new float[1];
				dataArray[0] = mHTSValue;
			}
			else {
				float[] newDataArray = new float[dataArray.length + 1];
				for (int i = 0; i < dataArray.length; i++) {
					newDataArray[i] = dataArray[i];
				}
				newDataArray[dataArray.length] = mHTSValue;
				dataArray = newDataArray;
			}
			setData(dataArray);
			mChart.invalidate();
		}
	}

	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();

			if (TemplateService.BROADCAST_RHTS_MEASUREMENT.equals(action)) {
				final float value = intent.getFloatExtra(TemplateService.EXTRA_DATA, 0);
				mHTSValue = value;
				final int extraHR = intent.getIntExtra(TemplateService.EXTRA_HEART_RATE_DATA, 0);
				mHRValue  = extraHR;
				// Update GUI
				setValueOnView(value, TemplateService.displayTemperatureType);
			}
		}
	};

	private static IntentFilter makeIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(TemplateService.BROADCAST_RHTS_MEASUREMENT);
		return intentFilter;
	}

	private Runnable mRepeatTask = new Runnable() {
		@Override
		public void run() {
//			if (mHTSValue > 0) {
//				if (dataArray == null) {
//					dataArray = new float[1];
//					dataArray[0] = mHTSValue;
//				}
//				else {
//					float[] newDataArray = new float[dataArray.length + 1];
//					for (int i = 0; i < dataArray.length; i++) {
//						newDataArray[i] = dataArray[i];
//					}
//					newDataArray[dataArray.length] = mHTSValue;
//					dataArray = newDataArray;
//				}
//				setData(dataArray);
//				mChart.invalidate();
//				sharedMeasuredValues = PreferenceManager.getDefaultSharedPreferences(TemplateActivity.this);
//				SharedPreferences.Editor sharedMeasuredValuesEditor = sharedMeasuredValues.edit();
//				sharedMeasuredValuesEditor.putFloat("SHARED_TEMPERATURE_VALUE", mHTSValue);
//				sharedMeasuredValuesEditor.apply();
//				//startActivity(myIntent);
//			}
			if (isGraphInProgress)
				mHandler.postDelayed(mRepeatTask, REFRESH_INTERVAL);
		}
	};

	private void setData(float[] range) {
		ArrayList<Entry> values = new ArrayList<Entry>();

		for (int i = 0; i < range.length; i++) {
			values.add(new Entry(i, range[i]));
		}

		LineDataSet temperatureDataSet;

		if (mChart.getData() != null &&
				mChart.getData().getDataSetCount() > 0) {

			temperatureDataSet = (LineDataSet) mChart.getData().getDataSetByIndex(0);
			temperatureDataSet.setValues(values);
			mChart.getData().notifyDataChanged();
			mChart.notifyDataSetChanged();
		}
		else {
			// create a dataset and give it a type
			temperatureDataSet = new LineDataSet(values, "Nhiệt độ hiện tại");
			temperatureDataSet.setLineWidth(2f);
			temperatureDataSet.setColor(Color.RED);

			ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
			dataSets.add(temperatureDataSet);

			LineData data = new LineData(dataSets);
			data.setValueTextSize(8f);
			data.setValueTextColor(Color.BLUE);

			// set data
			mChart.setData(data);
		}
	}

}