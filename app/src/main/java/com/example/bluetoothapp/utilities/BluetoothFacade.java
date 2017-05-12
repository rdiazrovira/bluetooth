package com.example.bluetoothapp.utilities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.Set;

public class BluetoothFacade {

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> mBluetoothDevices;

    private OnBluetoothDeviceScanListener mScanListener;
    private OnDeviceFollowedNotificationListener mNotificationListener;
    private OnBluetoothAdapterListener mAdapterListener;

    public static final int REQUEST_ENABLE_BLUETOOTH = 1;
    public static final String BLUETOOTH_PREFS_FILE = "bluetooth.facade.preferences";
    public static final String PAIRED_BLUETOOTH_DEVICE = "paired_bluetooth_device";
    public static final String AVAILABLE_BLUETOOTH_DEVICE = "available_bluetooth_device";

    private static final String BLUETOOTH_FACADE_TAG = "bluetooth_facade";
    private static final String DISCOVERING_TAG = "discovering_tag";
    private static final String NOTIFICATIONS_TAG = "notifications_tag";

    private long mDiscoveryStartTime;
    private static final long mDiscoveryTimeout = 5000;

    public interface OnBluetoothDeviceScanListener {

        void onScanStarted(ArrayList<BluetoothDevice> devices);

        void onDeviceFound(ArrayList<BluetoothDevice> devices);

        void onScanFinishedAndDevicesFound(ArrayList<BluetoothDevice> devices);

        void onScanFinishedAndDevicesNoFound();

    }

    public interface OnDeviceFollowedNotificationListener {

        void onDeviceConnected(BluetoothDevice device);

        void onDeviceDisconnected(BluetoothDevice device);

    }

    public interface OnBluetoothAdapterListener {

        void onEnable();

        void onDisable();

    }

    public BluetoothFacade() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothDevices = new ArrayList<>();
        mBluetoothDevices = getPairedBluetoothDevices();
    }

    public void setScanListener(OnBluetoothDeviceScanListener scanListener) {
        mScanListener = scanListener;
    }

    public void setAdapterListener(OnBluetoothAdapterListener adapterListener) {
        mAdapterListener = adapterListener;
    }

    public void setNotificationListener(OnDeviceFollowedNotificationListener notificationListener) {
        mNotificationListener = notificationListener;
    }

    public boolean isSupported() {
        return mBluetoothAdapter != null;
    }

    public boolean isEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    public boolean isDiscovering() {
        return mBluetoothAdapter.isDiscovering();
    }

    public void startDiscovery() {
        mBluetoothAdapter.startDiscovery();
    }

    public void cancelDiscovery() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    public ArrayList<BluetoothDevice> getBluetoothDevices() {
        mBluetoothDevices.addAll(getPairedBluetoothDevices());
        return mBluetoothDevices;
    }

    public static String getDeviceClass(BluetoothDevice device) {
        int deviceClass = device.getBluetoothClass().getDeviceClass();
        switch (deviceClass) {
            case BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO:
                return "CAR_AUDIO";
            case BluetoothClass.Device.COMPUTER_LAPTOP:
                return "COMPUTER_LAPTOP";
            case BluetoothClass.Device.PHONE_SMART:
                return "PHONE_SMART";
            default:
                return "UNKNOWN";
        }
    }

    public static String getDeviceType(BluetoothDevice device) {
        return device.getBondState() == BluetoothDevice.BOND_BONDED ? PAIRED_BLUETOOTH_DEVICE :
                AVAILABLE_BLUETOOTH_DEVICE;
    }

    public void manageDeviceDiscovery(Intent intent) {

        String action = intent.getAction();

        Log.v(DISCOVERING_TAG, "Action: " + action);

        switch (action) {
            case BluetoothAdapter.ACTION_DISCOVERY_STARTED:

                Log.v(DISCOVERING_TAG, "ACTION_DISCOVERY_STARTED");
                mDiscoveryStartTime = System.currentTimeMillis();
                mBluetoothDevices.clear();
                mBluetoothDevices.addAll(getPairedBluetoothDevices());
                mScanListener.onScanStarted(mBluetoothDevices);
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:

                Log.v(DISCOVERING_TAG, "ACTION_DISCOVERY_FINISHED");
                toFinishDiscovery();
                break;
            case BluetoothDevice.ACTION_FOUND:

                Log.v(DISCOVERING_TAG, "ACTION_FOUND");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName() != null && isNewDevice(device.getName())) {
                    Log.v(DISCOVERING_TAG, "NEW DEVICE: " + device.getName());
                    mBluetoothDevices.add(device);
                    mScanListener.onDeviceFound(mBluetoothDevices);
                }
                if (isDiscoveryTimeFinished()) toFinishDiscovery();
                break;
            default:
                break;

        }

    }

    private ArrayList<BluetoothDevice> getPairedBluetoothDevices() {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        ArrayList<BluetoothDevice> pairedDevices = new ArrayList<>();
        for (BluetoothDevice device : devices) {
            if (device.getName() != null && isNewDevice(device.getName())) {
                Log.v(BLUETOOTH_FACADE_TAG, "PAIRED DEVICE: " + device.getName());
                pairedDevices.add(device);
            }
        }
        return pairedDevices;
    }

    private boolean isDiscoveryTimeFinished() {
        long discoveryEndTime = System.currentTimeMillis();
        return (discoveryEndTime - mDiscoveryStartTime) >= mDiscoveryTimeout;
    }

    private void toFinishDiscovery() {
        cancelDiscovery();
        if (mBluetoothDevices.size() > 0) {
            mScanListener.onScanFinishedAndDevicesFound(mBluetoothDevices);
        } else {
            mScanListener.onScanFinishedAndDevicesNoFound();
        }
    }

    private boolean isNewDevice(String deviceName) {
        for (int index = 0; index < mBluetoothDevices.size(); index++) {
            if (mBluetoothDevices.get(index).getName().equals(deviceName)) {
                return false;
            }
        }
        return true;
    }

    public void manageConnectionNotifications(Intent intent) {

        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        switch (action) {
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                mNotificationListener.onDeviceConnected(device);
                Log.v(NOTIFICATIONS_TAG, "ACTION_ACL_CONNECTED");
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                mNotificationListener.onDeviceDisconnected(device);
                Log.v(NOTIFICATIONS_TAG, "ACTION_ACL_DISCONNECTED");
                break;
        }

    }

    public void manageAdapter(Intent intent) {

        String action = intent.getAction();

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            switch (mBluetoothAdapter.getState()) {
                case BluetoothAdapter.STATE_ON:
                    Log.v(BLUETOOTH_FACADE_TAG, "STATE_ON");
                    mAdapterListener.onEnable();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    Log.v(BLUETOOTH_FACADE_TAG, "STATE_OFF");
                    mAdapterListener.onDisable();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.v(BLUETOOTH_FACADE_TAG, "STATE_TURNING_OFF");
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.v(BLUETOOTH_FACADE_TAG, "STATE_TURNING_ON");
                    break;
            }
        }

    }

}
