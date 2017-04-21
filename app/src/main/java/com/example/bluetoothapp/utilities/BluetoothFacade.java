package com.example.bluetoothapp.utilities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.example.bluetoothapp.MainActivity;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

import static android.content.SharedPreferences.*;

public class BluetoothFacade {

    private final OnBluetoothDeviceScanListener mScanListener;
    private final OnBluetoothDevicePairingListener mPairingListener;
    private final BluetoothClientConnection.OnBluetoothClientConnListener mConnListener;
    private final OnBluetoothDeviceConnectionListener mConnFollowedDevice;

    private Context mContext;

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> mBluetoothDevices;

    private static final String BLUETOOTH_UTIL_TAG = "bluetooth_util";
    private static final String PAIRING_TAG = "pairing_tag";
    public static final String BLUETOOTH_PREFS_FILE = "com.example.bluetoothapp.preferences";
    public static final String BLUETOOTH_DEVICE = "bluetooth_device";
    public static final String BLUETOOTH_DEVICE_FOLLOWED = "bluetooth_device_followed";
    public static final String PAIRED_BLUETOOTH_DEVICE = "paired_bluetooth_device";
    public static final String AVAILABLE_BLUETOOTH_DEVICE = "available_bluetooth_device";

    private boolean mUnpairing = true;


    public interface OnBluetoothDeviceScanListener {
        void onScanFinishedAndDevicesFound();

        void onScanFinishedAndDevicesNoFound();

        void onScanStarted();

        void onDeviceFound(ArrayList<BluetoothDevice> devices);
    }

    public interface OnBluetoothDevicePairingListener {

        void onPairingStart();

        void onWaitingForAuthorization();

        void onPairedDevice();

        void onUnpairedDevice(boolean paired);

    }

    public interface OnBluetoothDeviceConnectionListener {

        void onConnectedDevice(BluetoothDevice dev);

        void onDisconnectedDevice(BluetoothDevice dev);

    }

    public BluetoothFacade(OnBluetoothDeviceScanListener scanListener, OnBluetoothDevicePairingListener pairingListener, BluetoothClientConnection.OnBluetoothClientConnListener connListener, OnBluetoothDeviceConnectionListener connFollowedDevice, Context context) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothDevices = new ArrayList<>();
        mScanListener = scanListener;
        mPairingListener = pairingListener;
        mConnListener = connListener;
        mConnFollowedDevice = connFollowedDevice;
        mContext = context;
    }

    public BluetoothFacade() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mScanListener = null;
        mPairingListener = null;
        mConnListener = null;
        mConnFollowedDevice = null;
    }

    public boolean isSupported() {
        return mBluetoothAdapter != null ? true : false;
    }

    public boolean isEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    public boolean startDiscovery() {
        return mBluetoothAdapter.startDiscovery();
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    public void disable() {
        cancelDiscovery();
        mBluetoothAdapter.disable();
    }

    public void cancelDiscovery() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    private void addPairedBluetoothDevices() {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            if (device.getName() != null && isNewDevice(device.getAddress())) {
                mBluetoothDevices.add(device);
            }
        }
    }

    public void addBluetoothDevices(Intent intent) {

        String action = intent.getAction();

        if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
            Log.v(BLUETOOTH_UTIL_TAG, "ACTION_DISCOVERY_STARTED");
            mBluetoothDevices.clear();
            addPairedBluetoothDevices();
            mScanListener.onScanStarted();
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            Log.v(BLUETOOTH_UTIL_TAG, "ACTION_DISCOVERY_FINISHED");
            if (mBluetoothDevices.size() != 0) {
                mScanListener.onScanFinishedAndDevicesFound();
            } else {
                mScanListener.onScanFinishedAndDevicesNoFound();
            }
        } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            Log.v(BLUETOOTH_UTIL_TAG, "ACTION_FOUND");
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device.getName() != null && isNewDevice(device.getName())) {
                Log.v(BLUETOOTH_UTIL_TAG, "dev: " + device.getName());
                mBluetoothDevices.add(device);
            }
            mScanListener.onDeviceFound(mBluetoothDevices);
        }

    }

    public void unpairDevice(BluetoothDevice bluetoothDevice) {
        Method m = null;
        try {
            m = bluetoothDevice.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(bluetoothDevice, (Object[]) null);

        } catch (IllegalAccessException e) {
            e.getMessage();
        } catch (InvocationTargetException e) {
            e.getMessage();
        } catch (NoSuchMethodException e) {
            e.getMessage();
        }
        mUnpairing = true;
    }

    public void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.getMessage();
        }
        mUnpairing = false;
    }

    private boolean isNewDevice(String deviceName) {
        for (int index = 0; index < mBluetoothDevices.size(); index++) {
            if (mBluetoothDevices.get(index).getName().equals(deviceName)) {
                return false;
            }
        }
        return true;
    }

    public static String deviceType(BluetoothDevice device) {
        return device.getBondState() == BluetoothDevice.BOND_BONDED ? PAIRED_BLUETOOTH_DEVICE : AVAILABLE_BLUETOOTH_DEVICE;
    }

    public void manageDevicePairing(Intent intent) {

        String action = intent.getAction();
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(BLUETOOTH_PREFS_FILE, Context.MODE_PRIVATE);

        if (sharedPreferences.getString(BLUETOOTH_DEVICE_FOLLOWED, "").equals("")) {

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_BONDING:
                        mPairingListener.onPairingStart();
                        Log.v(PAIRING_TAG, "BOND_BONDING");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        mPairingListener.onPairedDevice();
                        Log.v(PAIRING_TAG, "BOND_BONDED");
                        break;
                    case BluetoothDevice.BOND_NONE:
                        mPairingListener.onUnpairedDevice(mUnpairing);
                        Log.v(PAIRING_TAG, "BOND_NONE");
                        break;
                    default:
                        break;
                }

            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                mPairingListener.onWaitingForAuthorization();
                Log.v(PAIRING_TAG, "ACTION_ACL_CONNECTED");
            }

        } else {

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mConnFollowedDevice.onConnectedDevice(device);
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                mConnFollowedDevice.onDisconnectedDevice(device);

            }

        }


    }

    public void connect(BluetoothDevice dev) {
        BluetoothClientConnection bcc = new BluetoothClientConnection(dev, mBluetoothAdapter, mConnListener);
        bcc.connect();
    }

}
