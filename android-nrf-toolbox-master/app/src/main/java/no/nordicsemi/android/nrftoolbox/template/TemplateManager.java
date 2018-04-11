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

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import java.util.Deque;
import java.util.LinkedList;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.log.Logger;
import no.nordicsemi.android.nrftoolbox.R;
import no.nordicsemi.android.nrftoolbox.parser.TemplateParser;
import no.nordicsemi.android.nrftoolbox.parser.TemperatureTypeParser;

/**
 * Modify to template manager to match your requirements.
 */
public class TemplateManager extends BleManager<TemplateManagerCallbacks> {
	private static final String TAG = "ReplaceHTS";

	private final static int HIDE_MSB_8BITS_OUT_OF_32BITS = 0x00FFFFFF;
	private final static int HIDE_MSB_8BITS_OUT_OF_16BITS = 0x00FF;
	private final static int SHIFT_LEFT_8BITS = 8;
	private final static int SHIFT_LEFT_16BITS = 16;
	private final static int GET_BIT24 = 0x00400000;
	private final static int FIRST_BIT_MASK = 0x01;

	/** The service UUID */
	public final static UUID SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb"); // TODO change the UUID to your match your service
	/** The characteristic UUID */
	private static final UUID MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb"); // TODO change the UUID to your match your characteristic

	/** The characteristic UUID */
	private static final UUID RHTS_TEMPERATURE_TYPE_CHARACTERISTIC_UUID = UUID.fromString("00002a1d-0000-1000-8000-00805f9b34fb");

	// TODO add more services and characteristics, if required
	private BluetoothGattCharacteristic mCharacteristic, mRHTSTypeCharacteristic;

	public TemplateManager(final Context context) {
		super(context);
	}

	@Override
	protected BleManagerGattCallback getGattCallback() {
		return mGattCallback;
	}

	/**
	 * BluetoothGatt callbacks for connection/disconnection, service discovery, receiving indication, etc
	 */
	private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {

		@Override
		protected Deque<Request> initGatt(final BluetoothGatt gatt) {
			final LinkedList<Request> requests = new LinkedList<>();
			// TODO initialize your device, enable required notifications and indications, write what needs to be written to start working
//			requests.add(Request.newEnableNotificationsRequest(mCharacteristic));
			if (mRHTSTypeCharacteristic != null) {
				requests.add(Request.newReadRequest(mRHTSTypeCharacteristic));
			}
			requests.add(Request.newEnableNotificationsRequest(mCharacteristic));
			return requests;
		}

		@Override
		protected boolean isRequiredServiceSupported(final BluetoothGatt gatt) {
			final BluetoothGattService service = gatt.getService(SERVICE_UUID);
			if (service != null) {
				mCharacteristic = service.getCharacteristic(MEASUREMENT_CHARACTERISTIC_UUID);
			}
			return mCharacteristic != null;
		}

		@Override
		protected boolean isOptionalServiceSupported(final BluetoothGatt gatt) {
			final BluetoothGattService service = gatt.getService(SERVICE_UUID);
			if (service != null) {
				mRHTSTypeCharacteristic = service.getCharacteristic(RHTS_TEMPERATURE_TYPE_CHARACTERISTIC_UUID);
			}
			return mRHTSTypeCharacteristic != null;
		}

		@Override
		protected void onDeviceDisconnected() {
			mRHTSTypeCharacteristic = null;
			mCharacteristic         = null;
		}

		// TODO implement data handlers. Methods below are called after the initialization is complete.

		@Override
		protected void onDeviceReady() {
			super.onDeviceReady();

			// TODO initialization is now ready. The activity is being notified using TemplateManagerCallbacks#onDeviceReady() method.
			// This method may be removed from this class if not required as the super class implementation handles this event.
		}

		@Override
		protected void onCharacteristicNotified(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
			// TODO this method is called when a notification has been received
			// This method may be removed from this class if not required

			Logger.a(mLogSession, "\"" + TemplateParser.parse(characteristic) + "\" received");

//			int value;
//			final int flags = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//			if ((flags & 0x01) > 0) {
//				value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
//			} else {
//				value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
//			}
			//This will send callback to the Activity when new value is received from HR device

			final int flags = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
			int temperatureValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
			final float displayTeperature = temperatureValue/(float)(100.0);
			mCallbacks.onSampleValueReceived(gatt.getDevice(), displayTeperature);

		}

//		private int flagsDecode(byte[] data) {
//
//		}

		@Override
		protected void onCharacteristicIndicated(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
			// TODO this method is called when an indication has been received
			// This method may be removed from this class if not required
		}

		@Override
		protected void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
			// TODO this method is called when the characteristic has been read
			// This method may be removed from this class if not required
			Logger.a(mLogSession, "\"" + TemperatureTypeParser.parse(characteristic) + "\" received");
			final String temperatureType = getBodyTemperatureType(characteristic.getValue()[0]);
			mCallbacks.onRHTSTemperatureTypeFound(gatt.getDevice(), temperatureType);
		}
		/**
		 * This method will decode and return Heart rate sensor position on body
		 */
		private String getBodyTemperatureType(final byte bodyTemperatureTypeValue) {
			final String[] locations = getContext().getResources().getStringArray(R.array.rhts_locations);
			if (bodyTemperatureTypeValue > locations.length)
				return getContext().getString(R.string.rhts_locations_other);
			return locations[bodyTemperatureTypeValue];
		}

		@Override
		protected void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
			// TODO this method is called when the characteristic has been written
			// This method may be removed from this class if not required
		}
	};

	/**
	 * This method decode temperature value received from Health Thermometer device First byte {0} of data is flag and first bit of flag shows unit information of temperature. if bit 0 has value 1
	 * then unit is Fahrenheit and Celsius otherwise Four bytes {1 to 4} after Flag bytes represent the temperature value in IEEE-11073 32-bit Float format
	 */
	private double decodeTemperature(byte[] data) throws Exception {
		double temperatureValue;
		byte flag = data[0];
		byte exponential = data[4];
		short firstOctet = convertNegativeByteToPositiveShort(data[1]);
		short secondOctet = convertNegativeByteToPositiveShort(data[2]);
		short thirdOctet = convertNegativeByteToPositiveShort(data[3]);
		int mantissa = ((thirdOctet << SHIFT_LEFT_16BITS) | (secondOctet << SHIFT_LEFT_8BITS) | (firstOctet)) & HIDE_MSB_8BITS_OUT_OF_32BITS;
		mantissa = getTwosComplimentOfNegativeMantissa(mantissa);
		temperatureValue = (mantissa * Math.pow(10, exponential));

		/*
		 * Conversion of temperature unit from Fahrenheit to Celsius if unit is in Fahrenheit
		 * Celsius = (Fahrenheit -32) 5/9
		 */
		if ((flag & FIRST_BIT_MASK) != 0) {
			temperatureValue = (float) ((temperatureValue - 32) * (5 / 9.0));
		}
		return temperatureValue;
	}
	private short convertNegativeByteToPositiveShort(byte octet) {
		if (octet < 0) {
			return (short) (octet & HIDE_MSB_8BITS_OUT_OF_16BITS);
		} else {
			return octet;
		}
	}
	private int getTwosComplimentOfNegativeMantissa(int mantissa) {
		if ((mantissa & GET_BIT24) != 0) {
			return ((((~mantissa) & HIDE_MSB_8BITS_OUT_OF_32BITS) + 1) * (-1));
		} else {
			return mantissa;
		}
	}

}
