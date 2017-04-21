package com.example.bluetoothapp.adapter;

import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.bluetoothapp.R;
import com.example.bluetoothapp.utilities.BluetoothFacade;

import java.util.ArrayList;

import static com.example.bluetoothapp.utilities.BluetoothFacade.AVAILABLE_BLUETOOTH_DEVICE;
import static com.example.bluetoothapp.utilities.BluetoothFacade.PAIRED_BLUETOOTH_DEVICE;

public class DeviceAdapter extends RecyclerView.Adapter {

    private ArrayList<Object> mList;
    private OnDeviceListItemClickListener mListener;

    private final int VIEW_HEADER = 0;
    private final int VIEW_ITEM = 1;

    public DeviceAdapter(ArrayList<BluetoothDevice> devices, OnDeviceListItemClickListener listener) {
        this.mList = buildList(devices);
        this.mListener = listener;
    }

    public interface OnDeviceListItemClickListener {
        void onItemClick(BluetoothDevice device);

        void onSettingsClick(BluetoothDevice device);
    }

    public ArrayList<Object> buildList(ArrayList<BluetoothDevice> devices) {
        ArrayList<Object> list = new ArrayList<>();
        int count = 0;
        for (BluetoothDevice device : devices) {
            if (BluetoothFacade.deviceType(device).equals(PAIRED_BLUETOOTH_DEVICE)) {
                if (count == 0) {
                    list.add(PAIRED_BLUETOOTH_DEVICE);
                }
                list.add(device);
                count++;
            }
        }
        count = 0;
        for (BluetoothDevice device : devices) {
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
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        if (viewType == VIEW_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.device_list_header, parent, false);
            viewHolder = new DeviceListHeaderViewHolder(view);
        } else if (viewType == VIEW_ITEM) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.device_list_item, parent, false);
            viewHolder = new DeviceListItemViewHolder(view);
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DeviceListHeaderViewHolder) {
            DeviceListHeaderViewHolder viewHolder = (DeviceListHeaderViewHolder) holder;
            String section = (String) mList.get(position);
            if (section.equals(AVAILABLE_BLUETOOTH_DEVICE)) {
                viewHolder.mSectionName.setText("Available devices");
            } else {
                viewHolder.mSectionName.setText("Paired devices");
            }
        } else if (holder instanceof DeviceListItemViewHolder) {
            DeviceListItemViewHolder viewHolder = (DeviceListItemViewHolder) holder;
            final BluetoothDevice device = (BluetoothDevice) mList.get(position);
            viewHolder.mDeviceNameTextView.setText(device.getName());
            viewHolder.mDeviceImageView.setImageResource(R.drawable.devices);
            viewHolder.mDeviceNameTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onItemClick(device);
                }
            });
            viewHolder.mSettingsImageView.setImageResource(R.drawable.settings);
            if (device.getBondState() != BluetoothDevice.BOND_BONDED)
                viewHolder.mSettingsImageView.setVisibility(View.INVISIBLE);
            viewHolder.mSettingsImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onSettingsClick(device);
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mList.get(position) instanceof String ? VIEW_HEADER : VIEW_ITEM;
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public class DeviceListItemViewHolder extends RecyclerView.ViewHolder {

        TextView mDeviceNameTextView;
        ImageView mDeviceImageView;
        ImageView mSettingsImageView;

        public DeviceListItemViewHolder(View itemView) {
            super(itemView);
            mDeviceNameTextView = (TextView) itemView.findViewById(R.id.DeviceNameTextView);
            mDeviceImageView = (ImageView) itemView.findViewById(R.id.DeviceImageView);
            mSettingsImageView = (ImageView) itemView.findViewById(R.id.SettingsImageView);
        }
    }

    public class DeviceListHeaderViewHolder extends RecyclerView.ViewHolder {

        TextView mSectionName;

        public DeviceListHeaderViewHolder(View itemView) {
            super(itemView);
            mSectionName = (TextView) itemView.findViewById(R.id.SectionNameTextView);
        }
    }

}
