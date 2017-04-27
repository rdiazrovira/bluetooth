package com.example.bluetoothapp.utilities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class BluetoothClientConnection extends Thread {

    private static final String BLUETOOTH_CLIENT_CONN = "bluetooth-client-conn";

    private BluetoothSocket mBluetoothSocket;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothAdapter mBluetoothAdapter;
    BluetoothFacade.OnBluetoothClientConnListener mListener;

    private UUID mUUID = UUID.fromString("00001112-0000-1000-8000-00805f9b34fb");
    //00000000-deca-fade-deca-deafdecacaff
    //00001101-0000-1000-8000-00805f9b34fb

    public BluetoothClientConnection(BluetoothDevice bluetoothDevice, BluetoothAdapter bluetoothAdapter, UUID uuid) {
        mBluetoothDevice = bluetoothDevice;
        mBluetoothAdapter = bluetoothAdapter;
        mListener = null;
        mUUID = uuid;
    }

    public boolean connect() {

        mBluetoothAdapter.cancelDiscovery();

        try {
            if (mBluetoothSocket != null) {
                mBluetoothSocket.close();
            }
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(mUUID);
        } catch (IOException e) {
            Log.v(BLUETOOTH_CLIENT_CONN, "create socket: " + e.getMessage());
            return false;
        }

        try {
            mBluetoothSocket.connect();
            return true;
        } catch (IOException e) {
            Log.v(BLUETOOTH_CLIENT_CONN, "connect: " + e.getMessage());
            try {
                mBluetoothSocket.close();
            } catch (IOException ioe) {
                Log.v(BLUETOOTH_CLIENT_CONN, "close: " + ioe.getMessage());
            }
            return false;
        }


    }


}
