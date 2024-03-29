/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DeviceControlActivity extends Activity implements JoystickView.JoystickListener{
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private boolean mCruiseControl = false;
    private boolean mBrake = false;
    private boolean mJerk = true;
    private int[] data = new int [2];

    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            String string_x = Integer.toString(data[0]);
            String string_y = Integer.toString(data[1]);
            String message = string_x + "," + string_y;
            mBluetoothLeService.writeCustomCharacteristic(message);
        }
    };


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
            mConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            mConnected = false;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu();
            }
        }
    };

    //The "main" function called when the activity starts.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        data[0] = 0;
        data[1] = 0;
        JoystickView jView = new JoystickView(this);
        super.onCreate(savedInstanceState);
        setContentView(jView);

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 1000,100);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        getActionBar().setTitle("WHEELS");
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    //Code called everytime the joystick is moved.
    @Override
    public void onJoystickMoved(float x, float y) {
        //Log.d("Main Method", "x: " + x + " y: " + y);
        int integer_x = Math.round(x);
        int integer_y = Math.round(y);
        data[0] = integer_x;
        data[1] = integer_y;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    //Called when you press back on your phone.
    @Override
    public void onBackPressed(){
        timerTask.cancel();
        mBluetoothLeService.disconnect();
        final Intent restart = new Intent(this, DeviceScanActivity.class);
        startActivity(restart);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_cruise:
                toggleControl(1);
                return true;
            case R.id.menu_diagnostics:
                timerTask.cancel();
                mBluetoothLeService.writeCustomCharacteristic("D");
                final Intent diagIntent = new Intent(this, DiagnosticsService.class);
                diagIntent.putExtra(EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
                startActivity(diagIntent);
                return true;
            case R.id.menu_brake:
                toggleControl(2);
                return true;
            case R.id.menu_jerk:
                toggleControl(3);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Code to toggle cruise control.
    private void toggleControl(int id){
        switch(id){
            case 1:
                mBluetoothLeService.writeCustomCharacteristic("C");
                if(mCruiseControl == false){
                    Toast.makeText(getApplicationContext(), "Cruise Control: ENABLED", Toast.LENGTH_SHORT).show();
                    mCruiseControl = true;
                }
                else{
                    Toast.makeText(getApplicationContext(),"Cruise Control: DISABLED", Toast.LENGTH_SHORT).show();
                    mCruiseControl = false;
                }
                break;
            case 2:
                mBluetoothLeService.writeCustomCharacteristic("B");
                if(mBrake == false){
                    Toast.makeText(getApplicationContext(), "Auto Brake: ENABLED", Toast.LENGTH_SHORT).show();
                    mBrake = true;
                }
                else{
                    Toast.makeText(getApplicationContext(),"Auto Brake: DISABLED", Toast.LENGTH_SHORT).show();
                    mBrake = false;
                }
                break;
            case 3:
                mBluetoothLeService.writeCustomCharacteristic("J");
                if(mJerk == false){
                    Toast.makeText(getApplicationContext(), "Jerk Limiter: ENABLED", Toast.LENGTH_SHORT).show();
                    mJerk = true;
                }
                else{
                    Toast.makeText(getApplicationContext(),"Jerk Limiter: DISABLED", Toast.LENGTH_SHORT).show();
                    mJerk = false;
                }
                break;
        }
    }

    //IntentFilter to be sent to the Broadcast Receiver.
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
