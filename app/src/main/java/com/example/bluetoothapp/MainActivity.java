package com.example.bluetoothapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetoothapp.adapter.DeviceAdapter;
import com.example.bluetoothapp.utilities.BluetoothFacade;

import java.util.ArrayList;

import static com.example.bluetoothapp.utilities.BluetoothFacade.AVAILABLE_BLUETOOTH_DEVICE;
import static com.example.bluetoothapp.utilities.BluetoothFacade.BLUETOOTH_PREFS_FILE;
import static com.example.bluetoothapp.utilities.BluetoothFacade.PAIRED_BLUETOOTH_DEVICE;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BLUETOOTH = 1, REQUEST_FINE_LOCATION = 2;
    private static final String MAIN_ACTIVITY_TAG = MainActivity.class.getSimpleName();
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    private Button mBluetoothButton;
    private Button mScanButton;
    private ListView mDeviceList;
    private ProgressDialog mDialog, mPDialog;

    private BluetoothFacade mBluetooth;
    private DeviceAdapter mDeviceAdapter;

    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBluetooth.addBluetoothDevices(intent);
        }
    };

    private ArrayList<Object> mList;

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

        mDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (mList.get(position) instanceof BluetoothDevice) {
                    final BluetoothDevice bluetoothDevice = (BluetoothDevice) mList.get(position);
                    String error = "";
                    if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                        error = mBluetooth.unpairDevice(bluetoothDevice);
                        if (error.equals("")) {
                            mEditor.clear().commit();
                            //mBluetooth.startDiscovery();
                        } else {
                            Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        error = mBluetooth.pairDevice(bluetoothDevice);
                        if (error.equals("")) {
                            mEditor.putString(PAIRED_BLUETOOTH_DEVICE, bluetoothDevice.getAddress());
                            mEditor.commit();
                            //mBluetooth.startDiscovery();
                        } else {
                            Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    }

                }
            }
        });

        registerReceiver(mReceiver);
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
            if (isAccessGrantedToTheLocation()) {
                mBluetooth.startDiscovery();
            }
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
        if (item.getItemId() == R.id.action_exit) finish();
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
        mBluetooth.cancelDiscovery();
        if (mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    private void initView() {
        mBluetoothButton = (Button) findViewById(R.id.BluetoothButton);
        mScanButton = (Button) findViewById(R.id.ScanButton);
        mDeviceList = (ListView) findViewById(R.id.listView);
        mList = new ArrayList<>();
        mDialog = createDialog("Loading available devices...");
        mPDialog = createDialog("Pairing");
        mFilter = new IntentFilter();

        mSharedPreferences = getSharedPreferences(BLUETOOTH_PREFS_FILE, Context.MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();

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
                mDeviceAdapter = new DeviceAdapter(MainActivity.this, devices);
                mList = mDeviceAdapter.buildList(devices);
                mDeviceList.setAdapter(mDeviceAdapter);
            }
        });

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

    private void registerReceiver(BroadcastReceiver receiver) {
        mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mFilter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, mFilter);
    }

}