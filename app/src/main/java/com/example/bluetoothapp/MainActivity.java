package com.example.bluetoothapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.example.bluetoothapp.utilities.BluetoothFacade;

import java.util.ArrayList;

import static com.example.bluetoothapp.utilities.BluetoothFacade.BLUETOOTH_DEVICE;
import static com.example.bluetoothapp.utilities.BluetoothFacade.mUnpair;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BLUETOOTH = 1, REQUEST_FINE_LOCATION = 2;
    private static final String MAIN_ACTIVITY_TAG = MainActivity.class.getSimpleName();
    public static final String ACTION_TAG = "action";

    private Button mBluetoothButton;
    private Button mScanButton;
    private RecyclerView mDeviceList;
    private ProgressDialog mDialog, mPDialog, mConnDialog;

    private BluetoothFacade mBluetooth;
    private DeviceAdapter mDeviceAdapter;

    private BroadcastReceiver mDiscoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBluetooth.manageDeviceDiscovery(intent);
        }
    };

    private BroadcastReceiver mPairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBluetooth.manageDevicePairing(intent);
        }
    };

    private BroadcastReceiver mConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBluetooth.manageDeviceConnection(intent);
        }
    };

    private BroadcastReceiver mNotificationsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBluetooth.manageFollowedDevice(intent);
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
                    Toast.makeText(MainActivity.this, "Please enable bluetooth!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        mClickListener = new DeviceAdapter.OnDeviceListItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            @Override
            public void onItemClick(BluetoothDevice device) {
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    mBluetooth.connectTo(device);
                } else {
                    mBluetooth.pairDevice(device);
                }
            }

            @Override
            public void onSettingsClick(BluetoothDevice device) {
                launchDeviceActivity(device.getAddress());
            }
        };

        registerReceiver(mDiscoveryReceiver, mPairingReceiver, mConnectionReceiver);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "The location permission was granted!",
                        Toast.LENGTH_SHORT).show();
                mBluetooth.startDiscovery();
            } else {
                Toast.makeText(MainActivity.this, "Please grant permission to location access to " +
                        "discover the available devices.", Toast.LENGTH_LONG).show();
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
        unregisterReceiver(mDiscoveryReceiver);
        unregisterReceiver(mPairingReceiver);
        unregisterReceiver(mConnectionReceiver);
        unregisterReceiver(mNotificationsReceiver);
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
        mPDialog = createDialog("Pairing..");
        mConnDialog = createDialog("Connecting..");

        mBluetooth = new BluetoothFacade(new BluetoothFacade.OnBluetoothDeviceScanListener() {
            @Override
            public void onScanFinishedAndDevicesFound() {
                mDialog.dismiss();
            }

            @Override
            public void onScanFinishedAndDevicesNoFound() {
                mDialog.dismiss();
                Toast.makeText(MainActivity.this, "No bluetooth devices found. The permission to " +
                                "location access is necessary to discover available devices. ",
                        Toast.LENGTH_LONG).show();
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
            public void onUnpairedDevice(String action) {
                if (mPDialog.isShowing()) mPDialog.dismiss();
            }

            @Override
            public void onPairingStart() {
                mPDialog.show();
            }

            @Override
            public void onWaitingForAuthorization() {
                mPDialog.dismiss();
            }
        }, new BluetoothFacade.OnBluetoothClientConnListener() {
            @Override
            public void onConnectionStarted() {
                mConnDialog.show();
            }

            @Override
            public void onSuccessfulConnection() {
                Log.v(MAIN_ACTIVITY_TAG, "onSuccessfulConnection");
                Toast.makeText(MainActivity.this, "Successful connection..", Toast.LENGTH_LONG)
                        .show();
                mConnDialog.dismiss();
            }

            @Override
            public void onFailedConnection() {
                Toast.makeText(MainActivity.this, "Failed connection...", Toast.LENGTH_LONG).show();
                mConnDialog.dismiss();
            }
        }, new BluetoothFacade.OnDeviceFollowedNotificationListener() {
            @Override
            public void onDeviceConnected(String deviceName) {
                Toast.makeText(MainActivity.this, deviceName + " connected", Toast.LENGTH_LONG)
                        .show();
            }

            @Override
            public void onDeviceDisconnected(String deviceName) {
                Toast.makeText(MainActivity.this, deviceName + " disconnected", Toast.LENGTH_LONG)
                        .show();
            }
        }, this);

        if (mBluetooth.isSupported()) {
            if (mBluetooth.isEnabled()) {
                mBluetoothButton.setText("Disable");
            } else {
                mBluetoothButton.setText("Enable");
            }
        } else {
            Toast.makeText(MainActivity.this, "This device doesn't support Bluetooth!",
                    Toast.LENGTH_SHORT).show();
            mBluetoothButton.setVisibility(View.INVISIBLE);
            mScanButton.setVisibility(View.INVISIBLE);
        }
    }

    public ProgressDialog createDialog(String message) {
        ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        dialog.setTitle("Bluetooth App");
        dialog.setMessage(message);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setIndeterminate(false);
        return dialog;
    }

    private void registerReceiver(BroadcastReceiver discoveryReceiver,
                                  BroadcastReceiver pairingReceiver,
                                  BroadcastReceiver connectionReceiver) {
        IntentFilter mFilter = new IntentFilter();
        /*Devices discovery (actions)*/
        mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mFilter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryReceiver, mFilter);
        mFilter = new IntentFilter();
        /*Device pairing (actions)*/
        mFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        mFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        mFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        registerReceiver(pairingReceiver, mFilter);
        mFilter = new IntentFilter();
        /*Device connection*/
        mFilter.addAction(BluetoothDevice.ACTION_UUID);
        registerReceiver(connectionReceiver, mFilter);
        mFilter = new IntentFilter();
        /*Notifications of device followed*/
        mFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        mFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mNotificationsReceiver, mFilter);
    }

    private void launchDeviceActivity(String deviceAddress) {
        Intent intent = new Intent(this, DeviceActivity.class);
        intent.putExtra(BLUETOOTH_DEVICE, deviceAddress);
        startActivity(intent);
    }

}

