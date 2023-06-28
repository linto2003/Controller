package com.example.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;

public class SocketCreationThread extends Thread {
    private BluetoothAdapter mBlueAdapter;
    private BluetoothSocket mainSocket;
    private final BluetoothDevice device;
    private Result res;

    @SuppressLint("MissingPermission")
    public SocketCreationThread(Result res, BluetoothAdapter mBlueAdapter, BluetoothDevice device) {
        this.mBlueAdapter = mBlueAdapter;
        this.res = res;

        // Creating socket for the devices
        BluetoothSocket temp = null;
        this.device = device;
        Log.d("socket", "SocketCreationThread: " + device);
        try {
            temp = device.createInsecureRfcommSocketToServiceRecord(device.getUuids()[0].getUuid());
            Log.d("socket", "SocketCreationThread: UUID: " + temp);
        } catch (
                IOException | NullPointerException e) {
            Log.e("socket", "makeConnection: failed at socket creation", e);
        }
        mainSocket = temp;
    }

    @SuppressLint("MissingPermission")
    public void run() {
        // Cancelling discovery so that the resources to deny downgrading of performance;
        if (mBlueAdapter.isDiscovering()) {
            mBlueAdapter.cancelDiscovery();
        }

        try {
            mainSocket.connect();
//            res.onSuccess(device.getName() + " connected");
        } catch (IOException e) {
            Log.e("socket", "run: failed at connecting to the device", e);

            try{
//                res.onFailure("Failed to connect to: " + device.getName());
                mainSocket.close();
            } catch (IOException ex) {
                Log.e("socket", "run: failed at closing the socket to the device", ex);
            }
        }
    }

    public void cancel() {
        try {
            mainSocket.close();
        } catch (IOException e) {
            Log.e("socket", "Could not close the client socket", e);
        }
    }

    public BluetoothSocket getMainSocket() {
        return mainSocket;
    }
}
