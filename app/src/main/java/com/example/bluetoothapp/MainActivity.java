package com.example.bluetoothapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.bluetoothapp.adapter.DeviceAdapter;
import com.example.bluetoothapp.utilities.BluetoothFacade;

import java.util.ArrayList;

import static com.example.bluetoothapp.utilities.BluetoothFacade.BLUETOOTH_PREFS_FILE;
import static com.example.bluetoothapp.utilities.BluetoothFacade.REQUEST_ENABLE_BLUETOOTH;

public class MainActivity extends AppCompatActivity {

    private static final String MAIN_ACTIVITY_TAG = "main_activity_tag";
    private static final String BLUETOOTH_DEVICE_FOLLOWED = "bluetooth_device_followed";

    BluetoothFacade mBluetooth;
    private DeviceAdapter mDeviceAdapter;

    RecyclerView mDeviceList;
    private Button mScanButton;
    private AlertDialog mAdapterDialog;
    private AlertDialog mDeviceDialog;

    private BroadcastReceiver mDiscoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBluetooth.manageDeviceDiscovery(intent);
        }
    };

    private BroadcastReceiver mAdapterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBluetooth.manageAdapter(intent);
        }
    };

    private BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBluetooth.manageConnectionNotifications(intent);
        }
    };

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetooth.isEnabled()) {
                    if (mBluetooth.isDiscovering()) {
                        Log.v(MAIN_ACTIVITY_TAG, "isDiscovering()");
                        mScanButton.setText(R.string.scan);
                        mBluetooth.cancelDiscovery();
                        Log.v(MAIN_ACTIVITY_TAG, "cancelDiscovery()");
                    } else {
                        mScanButton.setText(R.string.stop_scan);
                        mBluetooth.startDiscovery();
                        Log.v(MAIN_ACTIVITY_TAG, "startDiscovery()");
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Bluetooth will need to be " +
                            "enabled on your phone.", Toast.LENGTH_LONG).show();
                }
            }
        });

        registerReceiver();

    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mDiscoveryReceiver, intentFilter);
        intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mAdapterReceiver, intentFilter);
        intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mNotificationReceiver, intentFilter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_exit) {
            Log.v(MAIN_ACTIVITY_TAG, "finish()");
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        Log.v(MAIN_ACTIVITY_TAG, "onDestroy");
        unregisterReceiver(mDiscoveryReceiver);
        unregisterReceiver(mAdapterReceiver);
        unregisterReceiver(mNotificationReceiver);
        mBluetooth.cancelDiscovery();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        Log.v(MAIN_ACTIVITY_TAG, "onResume");
        if (mBluetooth.isSupported()) {
            if (mBluetooth.isEnabled()) {
                mDeviceAdapter.setList(mBluetooth.getBluetoothDevices());
                mDeviceAdapter.notifyDataSetChanged();
            } else {
                mAdapterDialog.show();
            }
            if (mBluetooth.isDiscovering()) {
                mScanButton.setText(R.string.stop_scan);
            } else {
                mScanButton.setText(R.string.scan);
            }
        } else {
            Log.v(MAIN_ACTIVITY_TAG, "This device doesn't support Bluetooth.");
            Toast.makeText(MainActivity.this, "This device doesn't support Bluetooth.",
                    Toast.LENGTH_SHORT).show();
            mScanButton.setVisibility(View.GONE);
        }
        super.onResume();
    }

    private void initView() {
        mDeviceList = (RecyclerView) findViewById(R.id.DeviceListRecyclerView);
        mDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mDeviceList.setHasFixedSize(true);
        mScanButton = (Button) findViewById(R.id.ScanButton);
        mAdapterDialog = createAdapterDialog();
        mDeviceDialog = createDeviceDialog();

        mSharedPreferences = getSharedPreferences(BLUETOOTH_PREFS_FILE, Context.MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();

        mBluetooth = new BluetoothFacade();
        mBluetooth.setScanListener(mScanListener);
        mBluetooth.setAdapterListener(mAdapterListener);
        mBluetooth.setNotificationListener(mNotificationListener);

        mDeviceAdapter = new DeviceAdapter(mBluetooth.getBluetoothDevices(), mItemClickListener);
        mDeviceList.setAdapter(mDeviceAdapter);
    }

    private BluetoothFacade.OnBluetoothDeviceScanListener
            mScanListener = new BluetoothFacade.OnBluetoothDeviceScanListener() {
        @Override
        public void onScanStarted(ArrayList<BluetoothDevice> devices) {
            Log.v(MAIN_ACTIVITY_TAG, "Scan started.");
            mDeviceAdapter.setScanning(true);
            mDeviceAdapter.setList(devices);
            mDeviceAdapter.notifyDataSetChanged();
        }

        @Override
        public void onDeviceFound(ArrayList<BluetoothDevice> devices) {
            Log.v(MAIN_ACTIVITY_TAG, "Device found.");
            mDeviceAdapter.setList(devices);
            mDeviceAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanFinishedAndDevicesFound(ArrayList<BluetoothDevice> devices) {
            Log.v(MAIN_ACTIVITY_TAG, "Scan finished and devices found.");
            mScanButton.setText(R.string.scan);
            mDeviceAdapter.setScanning(false);
            mDeviceAdapter.setList(devices);
            mDeviceAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanFinishedAndDevicesNoFound() {
            Log.v(MAIN_ACTIVITY_TAG, "Scan finished and devices no found.");
            Toast.makeText(MainActivity.this, "Devices no found.", Toast.LENGTH_LONG).show();
        }
    };

    private BluetoothFacade.OnBluetoothAdapterListener
            mAdapterListener = new BluetoothFacade.OnBluetoothAdapterListener() {
        @Override
        public void onEnable() {
            Log.v(MAIN_ACTIVITY_TAG, "Adapter enabled.");
            mDeviceAdapter.setList(mBluetooth.getBluetoothDevices());
            mDeviceAdapter.notifyDataSetChanged();
            if (mBluetooth.isDiscovering()) {
                mScanButton.setText(R.string.stop_scan);
            } else {
                mScanButton.setText(R.string.scan);
            }
        }

        @Override
        public void onDisable() {
            Log.v(MAIN_ACTIVITY_TAG, "Adapter disable.");
            mDeviceAdapter.setList(new ArrayList<BluetoothDevice>());
            mDeviceAdapter.notifyDataSetChanged();
            mScanButton.setText(R.string.scan);
        }
    };

    private BluetoothFacade.OnDeviceFollowedNotificationListener mNotificationListener = new BluetoothFacade.OnDeviceFollowedNotificationListener() {
        @Override
        public void onDeviceConnected(BluetoothDevice device) {
            if (isTheDeviceFollowed(device)) {
                Log.v(MAIN_ACTIVITY_TAG, device.getName() + " connected.");
                Toast.makeText(MainActivity.this, device.getName() + " connected. ",
                        Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onDeviceDisconnected(BluetoothDevice device) {
            if (isTheDeviceFollowed(device)) {
                Log.v(MAIN_ACTIVITY_TAG, device.getName() + " disconnected.");
                Toast.makeText(MainActivity.this, device.getName() + " disconnected. ",
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    private DeviceAdapter.OnItemClickListener mItemClickListener = new DeviceAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(BluetoothDevice device) {
            Log.v(MAIN_ACTIVITY_TAG, device.getName() + " Bond state: " + device.getBondState());
            if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                mDeviceDialog.show();
            } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.v(MAIN_ACTIVITY_TAG, " Device selected: " + device.getName());
                Toast.makeText(MainActivity.this, "You selected this device: " +
                        device.getName(), Toast.LENGTH_LONG).show();
                mEditor.putString(BLUETOOTH_DEVICE_FOLLOWED, device.getAddress());
                mEditor.commit();
            }
        }
    };

    private AlertDialog createAdapterDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(R.string.adapter_dialog_title);
        dialog.setMessage("In order for Bluetooth App, " +
                "Bluetooth will need to be enabled on your phone.\n" +
                "Do you want to continue with bluetooth enable?");
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
                    }
                });
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        return dialog;
    }

    private AlertDialog createDeviceDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(R.string.device_dialog_title);
        dialog.setMessage("In order to use this device, " +
                "you will first need to go to your Bluetooth settings " +
                "and pair it directly with your phone. ");
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Bluetooth Settings",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
                    }
                });
        return dialog;
    }

    private boolean isTheDeviceFollowed(BluetoothDevice device) {
        return mSharedPreferences.getString(BLUETOOTH_DEVICE_FOLLOWED, "").equals(device.getAddress());
    }

}

