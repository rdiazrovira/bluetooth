package com.example.bluetoothapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.bluetoothapp.adapter.DeviceAdapter;
import com.example.bluetoothapp.utilities.BluetoothClientConnection;
import com.example.bluetoothapp.utilities.BluetoothFacade;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

import static com.example.bluetoothapp.utilities.BluetoothFacade.BLUETOOTH_DEVICE;
import static com.example.bluetoothapp.utilities.BluetoothFacade.BLUETOOTH_PREFS_FILE;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BLUETOOTH = 1, REQUEST_FINE_LOCATION = 2;
    private static final String MAIN_ACTIVITY_TAG = MainActivity.class.getSimpleName();

    private Button mBluetoothButton;
    private Button mScanButton;
    private RecyclerView mDeviceList;
    private ProgressDialog mDialog, mPDialog;

    private BluetoothFacade mBluetooth;
    private DeviceAdapter mDeviceAdapter;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBluetooth.addBluetoothDevices(intent);
        }
    };

    private BroadcastReceiver mPairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBluetooth.manageDevicePairing(intent);
        }
    };

    private DeviceAdapter.OnDeviceListItemClickListener mClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        mBluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetooth.isEnabled()) {
                    mBluetooth.disable();
                    mBluetoothButton.setText("Enable");
                    mDeviceList.setAdapter(null);
                } else {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
                }
            }
        });

        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetooth.getBluetoothAdapter().isEnabled()) {
                    if (isAccessGrantedToTheLocation()) {
                        mBluetooth.startDiscovery();
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_FINE_LOCATION);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please enable bluetooth!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mClickListener = new DeviceAdapter.OnDeviceListItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            @Override
            public void onItemClick(BluetoothDevice device) {
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    mBluetooth.connect(device);
                } else {
                    mBluetooth.pairDevice(device);
                }
            }

            @Override
            public void onSettingsClick(BluetoothDevice device) {
                launchDeviceActivity(device.getAddress());
            }
        };

        registerReceiver(mReceiver, mPairingReceiver);
    }

    private boolean isAccessGrantedToTheLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "The location permission was granted!", Toast.LENGTH_SHORT).show();
                mBluetooth.startDiscovery();
            } else {
                Toast.makeText(MainActivity.this, "Please grant permission to location access to discover the available devices.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        if (mBluetooth.isEnabled()) {
            mBluetoothButton.setText("Disable");
        } else {
            mBluetoothButton.setText("Enable");
            mDeviceList.setAdapter(null);
        }
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_exit) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            mBluetoothButton.setText("Disable");
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Bluetooth disabled!", Toast.LENGTH_SHORT).show();
            mBluetoothButton.setText("Enable");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unregisterReceiver(mPairingReceiver);
        mBluetooth.cancelDiscovery();
        if (mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    private void initView() {
        mBluetoothButton = (Button) findViewById(R.id.BluetoothButton);
        mScanButton = (Button) findViewById(R.id.ScanButton);
        mDeviceList = (RecyclerView) findViewById(R.id.DeviceListRecyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mDeviceList.setLayoutManager(linearLayoutManager);
        mDeviceList.setHasFixedSize(true);
        mDialog = createDialog("Loading available devices...");
        mPDialog = createDialog("Pairing");

        mBluetooth = new BluetoothFacade(new BluetoothFacade.OnBluetoothDeviceScanListener() {
            @Override
            public void onScanFinishedAndDevicesFound() {
                mDialog.dismiss();
            }

            @Override
            public void onScanFinishedAndDevicesNoFound() {
                mDialog.dismiss();
                Toast.makeText(MainActivity.this, "No bluetooth devices found. The permission to location access is necessary to discover available devices. ", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onScanStarted() {
                mDialog.show();
            }

            @Override
            public void onDeviceFound(ArrayList<BluetoothDevice> devices) {
                mDeviceAdapter = new DeviceAdapter(devices, mClickListener);
                mDeviceList.setAdapter(mDeviceAdapter);
            }
        }, new BluetoothFacade.OnBluetoothDevicePairingListener() {
            @Override
            public void onPairedDevice() {
                if (mPDialog.isShowing()) mPDialog.dismiss();
                mBluetooth.startDiscovery();
            }

            @Override
            public void onUnpairedDevice(boolean unpairing) {
                if (mPDialog.isShowing()) mPDialog.dismiss();
                if (unpairing) mBluetooth.startDiscovery();
            }

            @Override
            public void onPairingStart() {
                mPDialog.show();
            }

            @Override
            public void onWaitingForAuthorization() {
                mPDialog.dismiss();
            }
        }, new BluetoothClientConnection.OnBluetoothClientConnListener() {
            @Override
            public void onSuccessConnection() {
                Toast.makeText(MainActivity.this, "Success connection...", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailedConnection() {
                Toast.makeText(MainActivity.this, "Failed connection...", Toast.LENGTH_LONG).show();
            }
        }, new BluetoothFacade.OnBluetoothDeviceConnectionListener() {
            @Override
            public void onConnectedDevice(BluetoothDevice dev) {
                Toast.makeText(MainActivity.this, "Connected device: " + dev.getName(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onDisconnectedDevice(BluetoothDevice dev) {
                Toast.makeText(MainActivity.this, "Disconnected device: " + dev.getName(), Toast.LENGTH_LONG).show();
            }
        }, this);

        if (mBluetooth.isSupported()) {
            if (mBluetooth.isEnabled()) {
                mBluetoothButton.setText("Disable");
            } else {
                mBluetoothButton.setText("Enable");
            }
        } else {
            Toast.makeText(MainActivity.this, "This device doesn't support Bluetooth!", Toast.LENGTH_SHORT).show();
            mBluetoothButton.setVisibility(View.INVISIBLE);
            mScanButton.setVisibility(View.INVISIBLE);
        }
    }

    public ProgressDialog createDialog(String message) {
        ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        dialog.setTitle("Bluetooth App");
        dialog.setMessage(message);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setIndeterminate(true);
        return dialog;
    }

    private void registerReceiver(BroadcastReceiver receiver, BroadcastReceiver pairingReceiver) {
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mFilter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, mFilter);
        mFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        mFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        mFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        registerReceiver(pairingReceiver, mFilter);
    }

    private void launchDeviceActivity(String deviceAddress) {
        Intent intent = new Intent(this, DeviceActivity.class);
        intent.putExtra(BLUETOOTH_DEVICE, deviceAddress);
        startActivity(intent);
    }

}