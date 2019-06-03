package com.example.android.bluetoothlegatt;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import static com.example.android.bluetoothlegatt.DeviceControlActivity.EXTRAS_DEVICE_ADDRESS;
import static com.example.android.bluetoothlegatt.DeviceControlActivity.EXTRAS_DEVICE_NAME;

public class FingerprintService extends AppCompatActivity {
    private TextView mParaLabel;
    private ImageView mFingerprintImage;
    private FingerprintManager fingerprintManager;
    private String mDeviceName;
    private String mDeviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_view);
        Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        mParaLabel = (TextView) findViewById(R.id.paraLabel);
        mFingerprintImage = (ImageView) findViewById(R.id.fingerprintImage);

        fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);

        if(checkPermissions() == true){
            FingerprintHandler fingerprintHandler = new FingerprintHandler(this);
            fingerprintHandler.startAuth(fingerprintManager, null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.fingerprint_menu, menu);
        return true;
    }

    private boolean checkPermissions(){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            mParaLabel.setText("Android Marshmallow required");
            return false;
        }
        else if(!fingerprintManager.isHardwareDetected()){
            mParaLabel.setText("No fingerprint scanner detected.");
            return false;
        }
        else if(ContextCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED){
            mParaLabel.setText("Fingerprint permission not granted.");
            return false;
        }
        else if(!fingerprintManager.hasEnrolledFingerprints()){
            mParaLabel.setText("No fingerprints saved on device.");
            return false;
        }
        else {
            mParaLabel.setText("Confirm fingerprint to continue.");
            return true;
        }
    }

    public class FingerprintHandler extends FingerprintManager.AuthenticationCallback{

        private Context context;

        public FingerprintHandler (Context context){
            this.context = context;
        }

        public void startAuth(FingerprintManager fingerprintManager, FingerprintManager.CryptoObject cryptoObject){
            CancellationSignal cancellationSignal = new CancellationSignal();
            fingerprintManager.authenticate(cryptoObject, cancellationSignal ,0,this,null);
        }

        @Override
        public void onAuthenticationError(int errorCode, CharSequence errString){
            this.update("Error: " + errString, false);
        }
        @Override
        public void onAuthenticationFailed(){
            this.update("Authentication Failed" , false);
        }
        @Override
        public void onAuthenticationHelp(int helpCode, CharSequence helpString){
            this.update("Error: " + helpString, false);
        }
        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result){
            this.update("Access Granted", true);
        }
        private void update(String s, boolean b){
            mParaLabel.setText(s);

            if(b == false){
                mParaLabel.setTextColor(0xffcc0000);
            }
            else{
                mParaLabel.setTextColor(0xff669900);
                mFingerprintImage.setImageResource(R.mipmap.action_done);
                final Intent controlIntent = new Intent(context, DeviceControlActivity.class);
                controlIntent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, mDeviceName);
                controlIntent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
                startActivity(controlIntent);
            }
        }
    }
}
