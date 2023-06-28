package com.example.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

public class PairedDevicesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private TextView deviceName, tag;
    private final ArrayList<BluetoothDevice> devices;
    private Context context;
    private final ConnectionHelper connect;
    private final HashMap<Object, String> pairedStatus;

    PairedDevicesAdapter(ArrayList<BluetoothDevice> devices, HashMap<Object, String> pairedStatus, Context context, ConnectionHelper connect) {
        this.devices = devices;
        this.context = context;
        this.connect = connect;
        this.pairedStatus = pairedStatus;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.paired_card, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        deviceName.setText(devices.get(position).getName());
        String status = pairedStatus.get(devices.get(position));

        if (status != null && status.equals("online")) tag.setText(R.string.greenBullet);
        else tag.setText(R.string.redBullet);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.pairedDeviceName);
            tag = itemView.findViewById(R.id.tag);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getLayoutPosition();
                    BluetoothDevice clickedDevice = devices.get(position);
                    if (pairedStatus.get(clickedDevice).equals("online")) connect.makeConnection(clickedDevice);
                }
            });
        }
    }
}
