package com.example.bluetoothapp.adapter;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.bluetoothapp.R;
import com.example.bluetoothapp.utilities.BluetoothFacade;

import java.util.ArrayList;

import static com.example.bluetoothapp.utilities.BluetoothFacade.AVAILABLE_BLUETOOTH_DEVICE;
import static com.example.bluetoothapp.utilities.BluetoothFacade.PAIRED_BLUETOOTH_DEVICE;

public class DeviceAdapter extends RecyclerView.Adapter {

    private ArrayList<Object> mList;
    private OnItemClickListener mItemClickListener;
    private final int VIEW_HEADER = 0;
    private final int VIEW_ITEM = 1;
    private boolean mScanning = false;

    public interface OnItemClickListener {
        void onItemClick(BluetoothDevice device);
    }

    public DeviceAdapter(ArrayList<BluetoothDevice> devices,
                         OnItemClickListener itemClickListener) {
        mList = buildList(devices);
        mItemClickListener = itemClickListener;
    }

    public void setList(ArrayList<BluetoothDevice> devices) {
        mList = buildList(devices);
    }

    public void setScanning(boolean scanning) {
        mScanning = scanning;
    }

    private ArrayList<Object> buildList(ArrayList<BluetoothDevice> devices) {
        ArrayList<Object> list = new ArrayList<>();
        if (getPaired(devices).size() > 0) {
            list.add(PAIRED_BLUETOOTH_DEVICE);
            list.addAll(getPaired(devices));
        }
        if (getAvailable(devices).size() > 0 || isScanning()) {
            list.add(AVAILABLE_BLUETOOTH_DEVICE);
            list.addAll(getAvailable(devices));
        }
        return list;
    }

    private ArrayList<BluetoothDevice> getPaired(ArrayList<BluetoothDevice> devices) {
        ArrayList<BluetoothDevice> pairedDevices = new ArrayList<>();
        ArrayList<BluetoothDevice> otherPairedDevices = new ArrayList<>();
        for (BluetoothDevice device : devices) {
            if (BluetoothFacade.getDeviceType(device).equals(PAIRED_BLUETOOTH_DEVICE)) {
                if (hasPriority(device)) {
                    pairedDevices.add(device);
                } else {
                    otherPairedDevices.add(device);
                }
            }
        }
        pairedDevices.addAll(otherPairedDevices);
        return pairedDevices;
    }

    private ArrayList<BluetoothDevice> getAvailable(ArrayList<BluetoothDevice> devices) {
        ArrayList<BluetoothDevice> availableDevices = new ArrayList<>();
        ArrayList<BluetoothDevice> otherAvailableDevices = new ArrayList<>();
        for (BluetoothDevice device : devices) {
            if (BluetoothFacade.getDeviceType(device).equals(AVAILABLE_BLUETOOTH_DEVICE)) {
                if (hasPriority(device)) {
                    availableDevices.add(device);
                } else {
                    otherAvailableDevices.add(device);
                }
            }
        }
        availableDevices.addAll(otherAvailableDevices);
        return availableDevices;
    }

    private boolean hasPriority(BluetoothDevice device) {
        return BluetoothFacade.getDeviceClassDescription(device).equals("AUDIO_VIDEO_HANDSFREE");
    }

    private boolean isScanning() {
        return mScanning;
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
            viewHolder.mScanningTextView.setVisibility(View.INVISIBLE);
            viewHolder.mScanningProgressBar.setVisibility(View.INVISIBLE);
            if (section.equals(AVAILABLE_BLUETOOTH_DEVICE)) {
                viewHolder.mSectionNameTextView.setText(R.string.available_devices);
                if (isScanning()) {
                    viewHolder.mScanningTextView.setVisibility(View.VISIBLE);
                    viewHolder.mScanningProgressBar.setVisibility(View.VISIBLE);
                }
            } else {
                viewHolder.mSectionNameTextView.setText(R.string.paired_devices);
            }
        } else if (holder instanceof DeviceListItemViewHolder) {
            DeviceListItemViewHolder viewHolder = (DeviceListItemViewHolder) holder;
            final BluetoothDevice device = (BluetoothDevice) mList.get(position);
            viewHolder.mDeviceNameTextView.setText(device.getName());
            viewHolder.mDeviceImageView.setImageResource(getImage(device));
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mItemClickListener.onItemClick(device);
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

    private class DeviceListItemViewHolder extends RecyclerView.ViewHolder {

        TextView mDeviceNameTextView;
        ImageView mDeviceImageView;

        DeviceListItemViewHolder(View itemView) {
            super(itemView);
            mDeviceNameTextView = (TextView) itemView.findViewById(R.id.DeviceNameTextView);
            mDeviceImageView = (ImageView) itemView.findViewById(R.id.DeviceImageView);
        }
    }

    private class DeviceListHeaderViewHolder extends RecyclerView.ViewHolder {

        private TextView mSectionNameTextView;
        private TextView mScanningTextView;
        private ProgressBar mScanningProgressBar;

        DeviceListHeaderViewHolder(View itemView) {
            super(itemView);
            mSectionNameTextView = (TextView) itemView.findViewById(R.id.SectionNameTextView);
            mScanningTextView = (TextView) itemView.findViewById(R.id.ScanningTextView);
            mScanningProgressBar = (ProgressBar) itemView.findViewById(R.id.ScanningProgressBar);
        }
    }

    private int getImage(BluetoothDevice device) {
        int deviceClass = device.getBluetoothClass().getDeviceClass();
        switch (deviceClass) {
            case BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE:
                return R.drawable.handsfree;
            case BluetoothClass.Device.COMPUTER_LAPTOP:
                return R.drawable.computer;
            case BluetoothClass.Device.PHONE_SMART:
                return R.drawable.phone;
            default:
                return R.drawable.unknown;
        }
    }

}
