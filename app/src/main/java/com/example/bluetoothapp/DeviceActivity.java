package com.example.bluetoothapp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetoothapp.utilities.BluetoothFacade;

import static com.example.bluetoothapp.utilities.BluetoothFacade.BLUETOOTH_DEVICE;
import static com.example.bluetoothapp.utilities.BluetoothFacade.BLUETOOTH_DEVICE_FOLLOWED;
import static com.example.bluetoothapp.utilities.BluetoothFacade.BLUETOOTH_PREFS_FILE;

public class DeviceActivity extends AppCompatActivity {

    private BluetoothFacade mBluetooth;
    private BluetoothDevice mBluetoothDevice;

    Button mUnpairButton;
    Button mFollowButton;
    TextView mDevnameTextView;

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        initView();

        mUnpairButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetooth.unpairDevice(mBluetoothDevice);
                finish();
            }
        });

        mFollowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DeviceActivity.this, "You are following this device: " + mBluetoothDevice.getName(), Toast.LENGTH_LONG).show();
                mEditor.putString(BLUETOOTH_DEVICE_FOLLOWED, mBluetoothDevice.getAddress());
                mEditor.commit();
            }
        });

    }

    private void initView(){
        mUnpairButton = (Button) findViewById(R.id.UnpairButton);
        mFollowButton = (Button) findViewById(R.id.FollowButton);
        mDevnameTextView = (TextView) findViewById(R.id.DevNameTextView);
        mBluetooth = new BluetoothFacade();

        mBluetoothDevice = mBluetooth.getBluetoothAdapter().getRemoteDevice(getIntent().getStringExtra(BLUETOOTH_DEVICE));
        mDevnameTextView.setText(mBluetoothDevice.getName());

        mSharedPreferences = getSharedPreferences(BLUETOOTH_PREFS_FILE, Context.MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();
    }

}
