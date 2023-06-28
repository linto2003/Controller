package com.example.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DiscoveredDevicesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private TextView deviceName;
    private ArrayList<BluetoothDevice> devices;
    private Context context;
    private ConnectionHelper connect;

    DiscoveredDevicesAdapter(ArrayList<BluetoothDevice> devices, Context context, ConnectionHelper connect) {
        this.devices = devices;
        this.context = context;
        this.connect = connect;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.discovered_card, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        deviceName.setText(devices.get(position).getName());
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.disDeviceName);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connect.makeConnection(devices.get(getLayoutPosition()));
                }
            });
        }
    }
}
