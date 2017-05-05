package com.example.bluetoothapp.utilities;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

class BluetoothClientConnection extends Thread {

    private static final String BLUETOOTH_CLIENT_CONN = "bluetooth-client-conn";

    private BluetoothSocket mBluetoothSocket;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothAdapter mBluetoothAdapter;

    private UUID mUUID = UUID.fromString("00001112-0000-1000-8000-00805f9b34fb");
    //00000000-deca-fade-deca-deafdecacaff
    //0000112f-0000-1000-8000-00805f9b34fb
    //00001101-0000-1000-8000-00805f9b34fb

    /*
    Serial Port Profile
    00001101-0000-1000-8000-00805f9b34fb
    A2DP - Fuente de audio
    0000110a-0000-1000-8000-00805f9b34fb
    Perfil de objetos - OPP
    00001105-0000-1000-8000-00805f9b34fb
    PANU - Networking
    00001115-0000-1000-8000-00805f9b34fb
    Perfil de auriculares - HSP
    00001112-0000-1000-8000-00805f9b34fb
    Perfil de manos libres - HFP
    0000111f-0000-1000-8000-00805f9b34fb
*/

    BluetoothClientConnection(BluetoothDevice bluetoothDevice, BluetoothAdapter bluetoothAdapter, UUID uuid) {
        mBluetoothDevice = bluetoothDevice;
        mBluetoothAdapter = bluetoothAdapter;
        mUUID = uuid;
        //mUUID = UUID.fromString("00001116-0000-1000-8000-00805f9b34fb");
                //0000110a-0000-1000-8000-00805f9b34fb
    }

    // PAN profile Connected
    //BluetoothEventManager
    //CONNECTION_STATE_CHANGED

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    boolean connect() {

        mBluetoothAdapter.cancelDiscovery();

        try {
            if (mBluetoothSocket != null) {
                mBluetoothSocket.close();
            }
            mBluetoothSocket = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(mUUID);
        } catch (IOException e) {
            Log.v(BLUETOOTH_CLIENT_CONN, "create socket: " + e);
            return false;
        }

        try {
            mBluetoothSocket.connect();
            Log.v(BLUETOOTH_CLIENT_CONN, "successful connection (" + mUUID + ") ");
            return true;
        } catch (IOException e) {
            Log.v(BLUETOOTH_CLIENT_CONN, "failed connection (" + mUUID + "): " + e);
            return false;
        }


    }


}
