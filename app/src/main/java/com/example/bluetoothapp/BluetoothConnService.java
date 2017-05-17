package com.example.bluetoothapp;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.example.bluetoothapp.utilities.BluetoothFacade;

import static com.example.bluetoothapp.MainActivity.BLUETOOTH_DEVICE_FOLLOWED;
import static com.example.bluetoothapp.utilities.BluetoothFacade.BLUETOOTH_PREFS_FILE;

public class BluetoothConnService extends Service {

    private static final String BLUETOOTH_CONN_SERVICE_TAG = "bluetooth_conn_service";
    private BluetoothFacade mBluetoothFacade;

    private SharedPreferences mSharedPreferences;

    private IntentFilter mFilter;
    private BroadcastReceiver mNotificationsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBluetoothFacade.manageConnectionNotifications(intent);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(BLUETOOTH_CONN_SERVICE_TAG, "onCreate");
        mSharedPreferences = getSharedPreferences(BLUETOOTH_PREFS_FILE, Context.MODE_PRIVATE);

        mBluetoothFacade = new BluetoothFacade();
        mBluetoothFacade.setNotificationListener(new BluetoothFacade.
                OnDeviceFollowedNotificationListener() {
            @Override
            public void onDeviceConnected(BluetoothDevice device) {
                if (isTheDeviceFollowed(device)) {
                    Log.v(BLUETOOTH_CONN_SERVICE_TAG, device.getName() + " connected.");
                    Toast.makeText(getApplicationContext(), device.getName() + " connected. ",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onDeviceDisconnected(BluetoothDevice device) {
                if (isTheDeviceFollowed(device)) {
                    Log.v(BLUETOOTH_CONN_SERVICE_TAG, device.getName() + " disconnected.");
                    Toast.makeText(getApplicationContext(), device.getName() + " disconnected. ",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        mFilter = new IntentFilter();
        mFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        mFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mNotificationsReceiver, mFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(BLUETOOTH_CONN_SERVICE_TAG, "onStartCommand");
        addNotification();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(BLUETOOTH_CONN_SERVICE_TAG, "onDestroy");
        unregisterReceiver(mNotificationsReceiver);
    }

    private boolean isTheDeviceFollowed(BluetoothDevice device) {
        return mSharedPreferences.getString(BLUETOOTH_DEVICE_FOLLOWED, "")
                .equals(device.getAddress());
    }

    private void addNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            notification = new Notification.Builder(this)
                    .setContentTitle(getText(R.string.app_name))
                    .setContentText(getText(R.string.notification_bluetooth))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .setTicker(getText(R.string.app_name))
                    .build();
        }
        startForeground(30, notification);
    }

}