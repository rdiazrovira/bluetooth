package com.example.bluetoothapp.utilities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class BluetoothClientConnection extends Thread {

    BluetoothSocket mBluetoothSocket;
    BluetoothDevice mBluetoothDevice;
    BluetoothAdapter mBluetoothAdapter;
    OnBluetoothClientConnListener mListener;

    private static final String BLUETOOTH_CLIENT_CONN = "bluetooth-client-conn";
    public static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public interface OnBluetoothClientConnListener {

        void onSuccessConnection();

        void onFailedConnection();

    }

    public BluetoothClientConnection(BluetoothDevice bluetoothDevice, BluetoothAdapter bluetoothAdapter, OnBluetoothClientConnListener listener) {
        mBluetoothDevice = bluetoothDevice;
        mBluetoothAdapter = bluetoothAdapter;
        mListener = listener;

        try {
            mBluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(mUUID);

        } catch (IOException e) {
            Log.v(BLUETOOTH_CLIENT_CONN, "create socket: " + e.getMessage());
        }

    }

    public void connect() {

        mBluetoothAdapter.cancelDiscovery();

        try {
            mBluetoothSocket.connect();
            mListener.onSuccessConnection();
        } catch (IOException e) {
            mListener.onFailedConnection();
            Log.v(BLUETOOTH_CLIENT_CONN, "connect: " + e.getMessage());
            try {
                mBluetoothSocket.close();
            } catch (IOException ioe) {
                Log.v(BLUETOOTH_CLIENT_CONN, "close: " + ioe.getMessage());
            }
        }

    }


}
