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

    private final OnBluetoothDeviceScanListener mListener;

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> mBluetoothDevices;

    private static final String BLUETOOTH_UTIL_TAG = "bluetooth_util";
    public static final String BLUETOOTH_PREFS_FILE = "com.example.bluetoothapp.preferences";
    public static final String PAIRED_BLUETOOTH_DEVICE = "paired_bluetooth_device";
    public static final String AVAILABLE_BLUETOOTH_DEVICE = "available_bluetooth_device";


    public interface OnBluetoothDeviceScanListener {
        void onScanFinishedAndDevicesFound();

        void onScanFinishedAndDevicesNoFound();

        void onScanStarted();

        void onDeviceFound(ArrayList<BluetoothDevice> devices);
    }

    public BluetoothFacade(OnBluetoothDeviceScanListener listener) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothDevices = new ArrayList<>();
        mListener = listener;
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

    public boolean isDiscovering() {
        return mBluetoothAdapter.isDiscovering();
    }

    public void cancelDiscovery() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    private void addPairedBluetoothDevices() {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            Log.v(BLUETOOTH_UTIL_TAG, "d: " + device.getName());
            if (device.getName() != null && isNewDevice(device.getAddress())) {
                mBluetoothDevices.add(device);
            }
        }
    }

    public void addBluetoothDevices(Intent intent) {

        String action = intent.getAction();

        Log.v(BLUETOOTH_UTIL_TAG, "action: " + action);

        if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
            Log.v(BLUETOOTH_UTIL_TAG, "ACTION_DISCOVERY_STARTED");
            mBluetoothDevices.clear();
            addPairedBluetoothDevices();
            mListener.onScanStarted();
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            Log.v(BLUETOOTH_UTIL_TAG, "ACTION_DISCOVERY_FINISHED");
            if (mBluetoothDevices.size() != 0) {
                mListener.onScanFinishedAndDevicesFound();
            } else {
                mListener.onScanFinishedAndDevicesNoFound();
            }
        } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            Log.v(BLUETOOTH_UTIL_TAG, "ACTION_FOUND");
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device.getName() != null && isNewDevice(device.getName())) {
                Log.v(BLUETOOTH_UTIL_TAG, "dev: " + device.getName());
                mBluetoothDevices.add(device);
            }
            mListener.onDeviceFound(mBluetoothDevices);
        }

    }

    public String unpairDevice(BluetoothDevice bluetoothDevice) {
        Method m = null;
        try {
            m = bluetoothDevice.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(bluetoothDevice, (Object[]) null);
        } catch (IllegalAccessException e) {
            return "Error to pairing devices: " + e.getMessage();
        } catch (InvocationTargetException e) {
            return "Error to pairing devices: " + e.getMessage();
        } catch (NoSuchMethodException e) {
            return "Error to pairing devices: " + e.getMessage();
        }
        return "";
    }

    public String pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            return "Error to pairing devices: " + e.getMessage();
        }
        return "";
    }

    private boolean isNewDevice(String deviceName) {
        for (int index = 0; index < mBluetoothDevices.size(); index++) {
            if (mBluetoothDevices.get(index).getName().equals(deviceName)) {
                return false;
            }
        }
        return true;
    }

    public BluetoothDevice getDevice(int position) {
        return mBluetoothDevices.get(position);
    }

    public void connectAsClient(BluetoothDevice bluetoothDevice, BluetoothClientConnection.OnBluetoothClientConnListener listener) {
        BluetoothClientConnection BTClientConn = new BluetoothClientConnection(bluetoothDevice, mBluetoothAdapter, listener);
        BTClientConn.connect();
    }

    public static String deviceType(BluetoothDevice device) {
        return device.getBondState() == BluetoothDevice.BOND_BONDED ? PAIRED_BLUETOOTH_DEVICE : AVAILABLE_BLUETOOTH_DEVICE;
    }
}
