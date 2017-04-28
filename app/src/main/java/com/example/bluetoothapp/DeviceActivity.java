package com.example.bluetoothapp;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetoothapp.utilities.BluetoothFacade;

import static com.example.bluetoothapp.utilities.BluetoothFacade.BLUETOOTH_DEVICE;
import static com.example.bluetoothapp.utilities.BluetoothFacade.BLUETOOTH_DEVICE_FOLLOWED;
import static com.example.bluetoothapp.utilities.BluetoothFacade.BLUETOOTH_PREFS_FILE;
import static com.example.bluetoothapp.utilities.BluetoothFacade.mUnpair;

public class DeviceActivity extends AppCompatActivity {

    private BluetoothFacade mBluetooth;
    private BluetoothDevice mBluetoothDevice;

    Button mUnpairButton;
    Button mFollowButton;
    TextView mDevnameTextView;

    private SharedPreferences.Editor mEditor;

    private BroadcastReceiver mPairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBluetooth.manageDevicePairing(intent);
        }
    };

    String mDeviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        initView();

        mUnpairButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                mBluetooth.unpairDevice(mBluetoothDevice);
            }
        });

        mFollowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DeviceActivity.this, "You are following this device: " +
                        mBluetoothDevice.getName(), Toast.LENGTH_LONG).show();
                mEditor.putString(BLUETOOTH_DEVICE_FOLLOWED, mBluetoothDevice.getAddress());
                mEditor.commit();
            }
        });

        registerReceiver(mPairingReceiver);

    }

    private void initView() {
        mUnpairButton = (Button) findViewById(R.id.UnpairButton);
        mFollowButton = (Button) findViewById(R.id.FollowButton);
        mDevnameTextView = (TextView) findViewById(R.id.DevNameTextView);
        mBluetooth = new BluetoothFacade();
        mBluetooth.setPairingListener(new BluetoothFacade.OnBluetoothDevicePairingListener() {
            @Override
            public void onPairingStart() {
                Log.v("onPairingStart", "onPairingStart");
            }

            @Override
            public void onWaitingForAuthorization() {
                Log.v("onWaitingForAuth", "onWaitingForAuthorization");
            }

            @Override
            public void onPairedDevice() {
                Log.v("onPairedDevice", "onPairedDevice");
            }

            @Override
            public void onUnpairedDevice(String action) {
                Log.v("onUnpairedDevice", "action: " + action);
                if (action.equals(mUnpair)) {
                    mBluetooth.startDiscovery();
                    finish();
                }
            }
        });

        mBluetoothDevice = mBluetooth.getBluetoothAdapter().getRemoteDevice(getIntent().getStringExtra(BLUETOOTH_DEVICE));
        mDevnameTextView.setText(mBluetoothDevice.getName());
        mDeviceAddress = mBluetoothDevice.getAddress();

        SharedPreferences sharedPreferences = getSharedPreferences(BLUETOOTH_PREFS_FILE, Context.MODE_PRIVATE);
        mEditor = sharedPreferences.edit();
    }

    private void registerReceiver(BroadcastReceiver pairingReceiver) {
        IntentFilter mFilter = new IntentFilter();
        /*Device pairing (actions)*/
        mFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        mFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        mFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        registerReceiver(pairingReceiver, mFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mPairingReceiver);
    }

    @Override
    protected void onRestart() {
        BluetoothDevice device = mBluetooth.getBluetoothAdapter().getRemoteDevice(mDeviceAddress);
        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            finish();
            mBluetooth.startDiscovery();
        }
        super.onRestart();
    }
}
