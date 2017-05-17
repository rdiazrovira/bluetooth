package com.example.bluetoothapp.utilities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BluetoothFacade {

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> mBluetoothDevices;

    private OnBluetoothDeviceScanListener mScanListener;
    private OnDeviceFollowedNotificationListener mNotificationListener;
    private OnBluetoothAdapterListener mAdapterListener;

    public static final int REQUEST_ENABLE_BLUETOOTH = 1;
    public static final int REQUEST_FINE_LOCATION = 2;
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

    public static String getDeviceClassDescription(BluetoothDevice device) {
        Map<Integer, String> deviceClasses = getDeviceClasses();
        int deviceClass = device.getBluetoothClass().getDeviceClass();
        return deviceClasses.get(deviceClass) == null ? "UNKNOWN" : deviceClasses.get(deviceClass);
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
                Log.v(BLUETOOTH_FACADE_TAG, "Dev-name: " + device.getName() +
                        " / Dev-class: " + getDeviceClassDescription(device));
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
            Log.v(BLUETOOTH_FACADE_TAG, "Dev-name: " + device.getName() +
                    " / Dev-class: " + getDeviceClassDescription(device));
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

    private static Map<Integer, String> getDeviceClasses() {
        Map<Integer, String> deviceClasses = new HashMap<>();
        deviceClasses.put(BluetoothClass.Device.COMPUTER_UNCATEGORIZED, "COMPUTER_UNCATEGORIZED");
        deviceClasses.put(BluetoothClass.Device.COMPUTER_DESKTOP, "COMPUTER_DESKTOP");
        deviceClasses.put(BluetoothClass.Device.COMPUTER_SERVER, "COMPUTER_SERVER");
        deviceClasses.put(BluetoothClass.Device.COMPUTER_LAPTOP, "COMPUTER_LAPTOP");
        deviceClasses.put(BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA,
                "COMPUTER_HANDHELD_PC_PDA");
        deviceClasses.put(BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA,
                "COMPUTER_PALM_SIZE_PC_PDA");
        deviceClasses.put(BluetoothClass.Device.COMPUTER_WEARABLE, "COMPUTER_WEARABLE");
        deviceClasses.put(BluetoothClass.Device.PHONE_UNCATEGORIZED, "PHONE_UNCATEGORIZED");
        deviceClasses.put(BluetoothClass.Device.PHONE_CELLULAR, "PHONE_CELLULAR");
        deviceClasses.put(BluetoothClass.Device.PHONE_CORDLESS, "PHONE_CORDLESS");
        deviceClasses.put(BluetoothClass.Device.PHONE_SMART, "PHONE_SMART");
        deviceClasses.put(BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY, "PHONE_MODEM_OR_GATEWAY");
        deviceClasses.put(BluetoothClass.Device.PHONE_ISDN, "PHONE_ISDN");
        deviceClasses.put(BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED,
                "AUDIO_VIDEO_UNCATEGORIZED");
        deviceClasses.put(BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET,
                "AUDIO_VIDEO_WEARABLE_HEADSET");
        deviceClasses.put(BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE, "AUDIO_VIDEO_HANDSFREE");
        deviceClasses.put(BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE, "AUDIO_VIDEO_MICROPHONE");
        deviceClasses.put(BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER, "AUDIO_VIDEO_LOUDSPEAKER");
        deviceClasses.put(BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES, "AUDIO_VIDEO_HEADPHONES");
        deviceClasses.put(BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO,
                "AUDIO_VIDEO_PORTABLE_AUDIO");
        deviceClasses.put(BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO, "AUDIO_VIDEO_CAR_AUDIO");
        deviceClasses.put(BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX, "AUDIO_VIDEO_SET_TOP_BOX");
        deviceClasses.put(BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO, "AUDIO_VIDEO_HIFI_AUDIO");
        deviceClasses.put(BluetoothClass.Device.AUDIO_VIDEO_VCR, "AUDIO_VIDEO_VCR");
        deviceClasses.put(BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA,
                "AUDIO_VIDEO_VIDEO_CAMERA");
        deviceClasses.put(BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER, "AUDIO_VIDEO_CAMCORDER");
        deviceClasses.put(BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR,
                "AUDIO_VIDEO_VIDEO_MONITOR");
        deviceClasses.put(BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER,
                "AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER");
        deviceClasses.put(BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING,
                "AUDIO_VIDEO_VIDEO_CONFERENCING");
        deviceClasses.put(BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY,
                "AUDIO_VIDEO_VIDEO_GAMING_TOY");
        deviceClasses.put(BluetoothClass.Device.WEARABLE_UNCATEGORIZED, "WEARABLE_UNCATEGORIZED");
        deviceClasses.put(BluetoothClass.Device.WEARABLE_WRIST_WATCH, "WEARABLE_WRIST_WATCH");
        deviceClasses.put(BluetoothClass.Device.WEARABLE_PAGER, "WEARABLE_PAGER");
        deviceClasses.put(BluetoothClass.Device.WEARABLE_JACKET, "WEARABLE_JACKET");
        deviceClasses.put(BluetoothClass.Device.WEARABLE_HELMET, "WEARABLE_HELMET");
        deviceClasses.put(BluetoothClass.Device.WEARABLE_GLASSES, "WEARABLE_GLASSES");
        deviceClasses.put(BluetoothClass.Device.TOY_UNCATEGORIZED, "TOY_UNCATEGORIZED");
        deviceClasses.put(BluetoothClass.Device.TOY_ROBOT, "TOY_ROBOT");
        deviceClasses.put(BluetoothClass.Device.TOY_VEHICLE, "TOY_VEHICLE");
        deviceClasses.put(BluetoothClass.Device.TOY_DOLL_ACTION_FIGURE, "TOY_DOLL_ACTION_FIGURE");
        deviceClasses.put(BluetoothClass.Device.TOY_CONTROLLER, "TOY_CONTROLLER");
        deviceClasses.put(BluetoothClass.Device.TOY_GAME, "TOY_GAME");
        deviceClasses.put(BluetoothClass.Device.HEALTH_UNCATEGORIZED, "HEALTH_UNCATEGORIZED");
        deviceClasses.put(BluetoothClass.Device.HEALTH_BLOOD_PRESSURE, "HEALTH_BLOOD_PRESSURE");
        deviceClasses.put(BluetoothClass.Device.HEALTH_THERMOMETER, "HEALTH_THERMOMETER");
        deviceClasses.put(BluetoothClass.Device.HEALTH_WEIGHING, "HEALTH_WEIGHING");
        deviceClasses.put(BluetoothClass.Device.HEALTH_GLUCOSE, "HEALTH_GLUCOSE");
        deviceClasses.put(BluetoothClass.Device.HEALTH_PULSE_OXIMETER, "HEALTH_PULSE_OXIMETER");
        deviceClasses.put(BluetoothClass.Device.HEALTH_PULSE_RATE, "HEALTH_PULSE_RATE");
        deviceClasses.put(BluetoothClass.Device.HEALTH_DATA_DISPLAY, "HEALTH_DATA_DISPLAY");
        return deviceClasses;
    }

}
