package com.example.bluetoothapp.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.bluetoothapp.R;
import com.example.bluetoothapp.utilities.BluetoothFacade;

import java.util.ArrayList;

import static com.example.bluetoothapp.utilities.BluetoothFacade.AVAILABLE_BLUETOOTH_DEVICE;
import static com.example.bluetoothapp.utilities.BluetoothFacade.BLUETOOTH_PREFS_FILE;
import static com.example.bluetoothapp.utilities.BluetoothFacade.PAIRED_BLUETOOTH_DEVICE;

public class DeviceAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<BluetoothDevice> mDevices;
    private ArrayList<Object> mList;

    public DeviceAdapter(Context context, ArrayList<BluetoothDevice> devices) {
        this.mContext = context;
        this.mDevices = devices;
        this.mList = buildList(devices);
    }

    public ArrayList<Object> buildList(ArrayList<BluetoothDevice> devices) {
        ArrayList<Object> list = new ArrayList<>();
        int count = 0;
        for (BluetoothDevice device : mDevices) {
            if (BluetoothFacade.deviceType(device).equals(PAIRED_BLUETOOTH_DEVICE)) {
                if (count == 0) {
                    list.add(PAIRED_BLUETOOTH_DEVICE);
                }
                list.add(device);
                count++;
            }
        }
        count = 0;
        for (BluetoothDevice device : mDevices) {
            if (BluetoothFacade.deviceType(device).equals(AVAILABLE_BLUETOOTH_DEVICE)) {
                if (count == 0) {
                    list.add(AVAILABLE_BLUETOOTH_DEVICE);
                }
                list.add(device);
                count++;
            }
        }
        return list;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {

            viewHolder = new ViewHolder();

            if (mList.get(position) instanceof String) {

                String header = (String) mList.get(position);
                convertView = LayoutInflater.from(mContext).inflate(R.layout.device_list_header, null);
                viewHolder.mTitleTextView = (TextView) convertView.findViewById(R.id.titleTextView);

                if (header.equals(AVAILABLE_BLUETOOTH_DEVICE)) {
                    viewHolder.mTitleTextView.setText("Available devices");
                } else {
                    viewHolder.mTitleTextView.setText("Paired devices");
                }

            } else if (mList.get(position) instanceof BluetoothDevice) {

                convertView = LayoutInflater.from(mContext).inflate(R.layout.device_list_item, null);
                viewHolder.mNameDeviceTextView = (TextView) convertView.findViewById(R.id.nameDeviceTextView);
                viewHolder.mDeviceImageView = (ImageView) convertView.findViewById(R.id.deviceImageView);

                BluetoothDevice device = (BluetoothDevice) mList.get(position);

                viewHolder.mNameDeviceTextView.setText(device.getName());
                viewHolder.mDeviceImageView.setImageResource(R.drawable.devices);

            }
            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView mNameDeviceTextView;
        ImageView mDeviceImageView;
        TextView mTitleTextView;
    }

}
