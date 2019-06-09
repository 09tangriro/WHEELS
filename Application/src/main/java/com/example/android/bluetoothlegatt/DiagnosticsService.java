/*
EDIT THE LAYOUT IN activity_diagnostics_service.xml

Currently this acts as a demo but it's made for you to change for your needs!
 */


package com.example.android.bluetoothlegatt;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import static com.example.android.bluetoothlegatt.DeviceControlActivity.EXTRAS_DEVICE_ADDRESS;

public class DiagnosticsService extends AppCompatActivity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;
    private TextView mLabel;

    //-------------MANIPULATE THESE VARIABLES------------//
    private String data;        //This is the data read from the Arduino, do with it as you please!
    private final static int PERIOD = 100;     // This is the time period at which the app will read data (i.e. read data once every PERIOD ms)
    private final static int DELAY = 500;      // This is the delay before the first read (recommended not to change).
    //--------------------------------------------------//

    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            data = mBluetoothLeService.readCustomCharacteristic();
            //--------------MANIPULATE DATA HERE---------------//
            mLabel.setText(data); //<- Set text of the text view like this!

        }
    };

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
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostics_service);
        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        mLabel = (TextView) findViewById(R.id.textView4); //<- Instantiate text views like this!

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        if(mBluetoothLeService != null){
            mBluetoothLeService.connect(mDeviceAddress);
        }

        Timer timer = new Timer();

        timer.scheduleAtFixedRate(timerTask, DELAY, PERIOD);
    }
    @Override
    public void onBackPressed(){
        timerTask.cancel();
        final Intent goBack = new Intent(this, DeviceControlActivity.class);
        goBack.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        startActivity(goBack);
    }
}
