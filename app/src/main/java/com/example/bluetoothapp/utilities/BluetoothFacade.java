package com.example.bluetoothapp.utilities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothFacade {

    private final OnBluetoothDeviceScanListener mScanListener;
    private final OnBluetoothDevicePairingListener mPairingListener;
    private final OnBluetoothClientConnListener mConnListener;
    private final OnDeviceFollowedNotificationListener mNotificationListener;

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

    private String mAction;
    private static String mPair = "bluetooth_facade_pair";
    public static String mUnpair = "bluetooth_facade_unpair";
    private static String mConnect = "bluetooth_facade_connect";

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

        void onUnpairedDevice(String action);

    }

    public interface OnBluetoothClientConnListener {

        void onConnectionStarted();

        void onSuccessfulConnection();

        void onFailedConnection();

    }

    public interface OnDeviceFollowedNotificationListener {

        void onDeviceConnected(String deviceName);

        void onDeviceDisconnected(String deviceName);

    }

    public BluetoothFacade(OnBluetoothDeviceScanListener scanListener,
                           OnBluetoothDevicePairingListener pairingListener,
                           OnBluetoothClientConnListener connListener,
                           OnDeviceFollowedNotificationListener notificationListener,
                           Context context) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothDevices = new ArrayList<>();
        mScanListener = scanListener;
        mPairingListener = pairingListener;
        mConnListener = connListener;
        mNotificationListener = notificationListener;
        mContext = context;
        mAction = "";
    }

    public BluetoothFacade(OnBluetoothDevicePairingListener pairingListener) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mScanListener = null;
        mPairingListener = pairingListener;
        mConnListener = null;
        mNotificationListener = null;
        mAction = "";
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

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    public void connectTo(BluetoothDevice device) {
        mAction = mConnect;
        mConnListener.onConnectionStarted();
        device.fetchUuidsWithSdp();
    }

    private void addPairedBluetoothDevices() {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            if (device.getName() != null && isNewDevice(device.getAddress())) {
                mBluetoothDevices.add(device);
            }
        }
    }

    public void manageDeviceDiscovery(Intent intent) {

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
        mAction = mUnpair;
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
    }

    public void pairDevice(BluetoothDevice device) {
        mAction = mPair;
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.getMessage();
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

    public static String deviceType(BluetoothDevice device) {
        return device.getBondState() == BluetoothDevice.BOND_BONDED ? PAIRED_BLUETOOTH_DEVICE :
                AVAILABLE_BLUETOOTH_DEVICE;
    }

    public void manageDevicePairing(Intent intent) {

        String action = intent.getAction();

        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {

            switch (device.getBondState()) {
                case BluetoothDevice.BOND_BONDING:
                    mPairingListener.onPairingStart();
                    Log.v(PAIRING_TAG, "BOND_BONDING");
                    break;
                case BluetoothDevice.BOND_BONDED:
                    mPairingListener.onPairedDevice();
                    Log.v(PAIRING_TAG, "BOND_BONDED");
                    mAction = "";
                    break;
                case BluetoothDevice.BOND_NONE:
                    Log.v(PAIRING_TAG, "Ac: "+mAction);
                    mPairingListener.onUnpairedDevice(mAction);
                    Log.v(PAIRING_TAG, "BOND_NONE");
                    mAction = "";
                    break;
                default:
                    break;
            }
        } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action) && (mAction.equals(mPair)
                || mAction.equals(mUnpair))) {
            mPairingListener.onWaitingForAuthorization();
            Log.v(PAIRING_TAG, "ACTION_ACL_CONNECTED");
        }
    }

    public void manageDeviceConnection(Intent intent) {

        String action = intent.getAction();
        boolean successfulConnection = false;

        if (BluetoothDevice.ACTION_UUID.equals(action) && mAction.equals(mConnect)) {

            Log.v(BLUETOOTH_UTIL_TAG, "Action: " + BluetoothDevice.ACTION_UUID);
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
            UUID[] uuidCandidates = uuidCandidates(uuidExtra);
            Log.v(BLUETOOTH_UTIL_TAG, "Size: " + uuidCandidates.length);
            for (int index = 0; index < uuidCandidates.length; index++) {
                BluetoothClientConnection bcc = new BluetoothClientConnection(device,
                        mBluetoothAdapter, uuidCandidates[index]);
                if (bcc.connect()) {
                    Log.v(BLUETOOTH_UTIL_TAG, "UUID Conn: " + uuidCandidates[index]);
                    successfulConnection = true;
                    break;
                }
                Log.v(BLUETOOTH_UTIL_TAG, "UUID: " + uuidCandidates[index]);
            }
            if (successfulConnection) {
                mConnListener.onSuccessfulConnection();
                mAction = "";
            } else {
                mConnListener.onFailedConnection();
                mAction = "";
            }
        }

    }

    private UUID[] uuidCandidates(Parcelable[] p) {
        int size = p.length + 1;
        UUID[] uuids = new UUID[size];
        for (int index = 0; index < p.length; index++) {
            uuids[index] = UUID.fromString(p[index].toString());
        }
        int lastIndex = size - 1;
        uuids[lastIndex] = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        return uuids;
    }

    public void manageFollowedDevice(Intent intent) {

        String action = intent.getAction();

        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(BLUETOOTH_PREFS_FILE,
                Context.MODE_PRIVATE);

        Log.v(BLUETOOTH_UTIL_TAG, "Action: " + mAction);

        if (sharedPreferences.getString(BLUETOOTH_DEVICE_FOLLOWED, "").equals(device.getAddress())
                && (mAction.equals(""))) {
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                mNotificationListener.onDeviceConnected(device.getName());
                Log.v("Notifications", "ACTION_ACL_CONNECTED");
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                mNotificationListener.onDeviceDisconnected(device.getName());
                Log.v("Notifications", "ACTION_ACL_DISCONNECTED");
            }
        }

    }

}
