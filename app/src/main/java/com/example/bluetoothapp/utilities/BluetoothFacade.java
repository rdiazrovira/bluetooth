package com.example.bluetoothapp.utilities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

public class BluetoothFacade {

    private final OnBluetoothDeviceScanListener mScanListener;
    private final OnBluetoothDevicePairingListener mPairingListener;

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> mBluetoothDevices;

    private static final String BLUETOOTH_UTIL_TAG = "bluetooth_util";
    private static final String PAIRING_TAG = "pairing_tag";
    public static final String BLUETOOTH_PREFS_FILE = "com.example.bluetoothapp.preferences";
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

    public BluetoothFacade(OnBluetoothDeviceScanListener scanListener, OnBluetoothDevicePairingListener pairingListener) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothDevices = new ArrayList<>();
        mScanListener = scanListener;
        mPairingListener = pairingListener;
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

    }
}
