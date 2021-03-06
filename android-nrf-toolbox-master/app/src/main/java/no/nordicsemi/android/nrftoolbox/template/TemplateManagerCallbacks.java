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

import no.nordicsemi.android.ble.BleManagerCallbacks;

/**
 * Interface {@link TemplateManagerCallbacks} must be implemented by {@link TemplateActivity} in order to receive callbacks from {@link TemplateManager}
 */
public interface TemplateManagerCallbacks extends BleManagerCallbacks {

	// TODO add more callbacks. Callbacks are called when a data has been received/written to a remote device. This is the way how the manager notifies the activity about this event.

	/**
	 * Called when the sensor position information has been obtained from the sensor
	 *
	 * @param device  the bluetooth device from which the value was obtained
	 * @param type
	 *            the sensor position
	 */
	void onRHTSTemperatureTypeFound(final BluetoothDevice device, String type, byte intType);

	/**
	 * Called when a value is received.
	 *
	 * @param device a device from which the value was obtained
	 * @param value the new value
	 */
	void onSampleValueReceived(final BluetoothDevice device, float value, int extraHR);

	/**
	 * Called when a characteristic value is written.
	 *
	 * @param device a device from which the value was obtained
	 * @param value
	 */
	void onCharacteristicValueWritten(final BluetoothDevice device, String stringValue, byte value);

}
