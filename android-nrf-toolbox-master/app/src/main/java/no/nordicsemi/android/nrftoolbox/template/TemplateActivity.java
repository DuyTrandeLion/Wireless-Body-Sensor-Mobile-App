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
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;

import java.util.UUID;
import java.util.Calendar;

import no.nordicsemi.android.nrftoolbox.R;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileService;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileServiceReadyActivity;
import no.nordicsemi.android.nrftoolbox.template.settings.SettingsActivity;
import no.nordicsemi.android.nrftoolbox.template.settings.SettingsFragment;

/**
 * Modify the Template Activity to match your needs.
 */
public class TemplateActivity extends BleProfileServiceReadyActivity<TemplateService.TemplateBinder> {
	@SuppressWarnings("unused")
	private final String TAG = "ReplaceHTSActivity";

	private final static String GRAPH_STATUS = "graph_status";
	private final static String GRAPH_COUNTER = "graph_counter";
	private final static String RHTS_VALUE = "rhts_value";

	private final static float MAX_HTS_VALUE = (float)150.0;
	private final static float MIN_POSITIVE_VALUE = (float)0.0;
	private final static int REFRESH_INTERVAL = 500; // 1 second interval

	private static int UINT_ON_VIEW;

	// TODO change view references to match your need
	private TextView mValueView, mRHTSType;
	private TextView mValueUnitView;

	private LineChart mChart;
	private SeekBar mSeekBarX, mSeekBarY;
	private TextView tvX, tvY;

	private float mCounter = 0;

	@Override
	protected void onCreateView(final Bundle savedInstanceState) {
		// TODO modify the layout file(s). By default the activity shows only one field - the Heart Rate value as a sample
		setContentView(R.layout.activity_feature_template);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setGUI();

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

		LimitLine llXAxis = new LimitLine(10f, "Index 10");
		llXAxis.setLineWidth(4f);
		llXAxis.enableDashedLine(10f, 10f, 0f);
		llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
		llXAxis.setTextSize(10f);

		YAxis leftAxis = mChart.getAxisLeft();
		leftAxis.removeAllLimitLines();

		// limit lines are drawn behind data (and not on top)
		leftAxis.setDrawLimitLinesBehindData(true);

		mChart.getAxisRight().setEnabled(false);

		LineData data = new LineData();
		data.setValueTextColor(Color.BLACK);
	}


	@Override
	protected void onInitialize(final Bundle savedInstanceState) {
		LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, makeIntentFilter());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
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
	}

	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();

			if (TemplateService.BROADCAST_RHTS_MEASUREMENT.equals(action)) {
				final float value = intent.getFloatExtra(TemplateService.EXTRA_DATA, 0);
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

	private void setData(float[] range) {

	}

}
