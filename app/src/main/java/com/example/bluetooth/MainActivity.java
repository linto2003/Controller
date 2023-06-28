package com.example.bluetooth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity implements ConnectionHelper, Result {

    private Button discBlueBtn, goUp, goDown, closeModalBtn, refreshBtn;
    private FloatingActionButton goFront, goLeft, goRight, goBack;
    private TableLayout devicesPanel, controllerMain;
    private TextView blueConStatus, blueAvailStatus, blueRequestTurnOn;
    private BluetoothAdapter mBlueAdapter;
    private ArrayList<BluetoothDevice> availableDevices, pairedDevices;
    private HashMap<Object, String> pairedStatus;
    private RecyclerView pairedRecyclerView, discoveredRecyclerView;
    private BroadcastReceiver blueState, blueDevices;
    private SocketCreationThread connection;
    private DiscoveredDevicesAdapter discoveredDevicesAdapter;
    private PairedDevicesAdapter pairedDevicesAdapter;
    private BluetoothService.ConnectedThread sendMessages;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        // Creating a bluetooth manager for all bluetooth activities;
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        mBlueAdapter = bluetoothManager.getAdapter();

        // Guard clause: Checking if a bluetooth adapter is installed or not;
        if (mBlueAdapter == null) {
            // Device doesn't support Bluetooth
            blueAvailStatus.setText(Constants.BLUE_NOT_AVAILABLE);
            return;
        }

        managePermissions();

        initialBlueSetup();
    }

    private void init() {
        devicesPanel = findViewById(R.id.modal);
        controllerMain = findViewById(R.id.controllerMain);
        availableDevices = new ArrayList<>();
        pairedStatus = new HashMap<>();

        discBlueBtn = findViewById(R.id.discBtn);
        blueConStatus = findViewById(R.id.blueConStatus);
        blueAvailStatus = findViewById(R.id.blueAvailStatus);
        blueRequestTurnOn = findViewById(R.id.blueRequestTurnOn);
        closeModalBtn = findViewById(R.id.closeModalBtn);
        refreshBtn = findViewById(R.id.refreshBtn);

        discoveredRecyclerView = findViewById(R.id.discoveredRecyclerView);
        discoveredRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        pairedRecyclerView = findViewById(R.id.pairedRecyclerView);
        pairedRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        goUp = findViewById(R.id.up);
        goDown = findViewById(R.id.down);
        goFront = findViewById(R.id.forwardBtn);
        goLeft = findViewById(R.id.leftBtn);
        goRight = findViewById(R.id.rightBtn);
        goBack = findViewById(R.id.backwardBtn);
    }

    private void initialBlueSetup() {
        ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION);
        blueAvailStatus.setText(Constants.BLUE_AVAILABLE);
        // checking if the bluetooth is already turned on;
        if (mBlueAdapter.isEnabled()) {
            blueConStatus.setText(Constants.BLUE_ON);
        } else {
            blueConStatus.setText(Constants.BLUE_OFF);
            blueRequestTurnOn.setText(Constants.BLUE_REQUEST);
        }

        // Setting up a receiver for the changes in the state of the bluetooth adapter;
        IntentFilter blueStatusFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        blueState = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                // checking if the received signal is regarding the bluetooth adapter or not
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {

                    // getting the specific change in the bluetooth adapter
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                    if (state == BluetoothAdapter.STATE_ON) {
                        blueConStatus.setText(Constants.BLUE_ON);
                        blueRequestTurnOn.setText("");
                    } else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                        blueConStatus.setText(Constants.BLUE_TURNING_OFF);
                    } else if (state == BluetoothAdapter.STATE_OFF) {
                        blueConStatus.setText(Constants.BLUE_OFF);
                        blueRequestTurnOn.setText(Constants.BLUE_REQUEST);
                        devicesPanel.setVisibility(View.GONE);
                        controllerMain.setVisibility(View.VISIBLE);
                    } else if (state == BluetoothAdapter.STATE_TURNING_ON) {
                        blueConStatus.setText(Constants.BLUE_TURNING_ON);
                    }
                }
            }
        };
        registerReceiver(blueState, blueStatusFilter);

        closeModalBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            public void onClick(View v) {
                try {
                    if (mBlueAdapter.isDiscovering()) {
                        mBlueAdapter.cancelDiscovery();
                    }
                    // unregistering the ACTION_FOUND receiver from discovering of devices
                    if (blueDevices != null) {
                        unregisterReceiver(blueDevices);
                    }
                    devicesPanel.setVisibility(View.GONE);
                    controllerMain.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Log.d("scan", "onClick: " + e);
                }
            }
        });

        discBlueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Guard Clause for bluetooth adapter presence
                if (mBlueAdapter == null) return;
                // checking if the bluetooth is turned on now;
                if (mBlueAdapter.isEnabled()) {
                    blueConStatus.setText(Constants.BLUE_ON);

                    controllerMain.setVisibility(View.GONE);
                    devicesPanel.setVisibility(View.VISIBLE);
                    discoverDevices();
                } else {
                    blueConStatus.setText(Constants.BLUE_OFF);
                    blueRequestTurnOn.setText(Constants.BLUE_REQUEST);
                }
            }
        });

        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            public void onClick(View v) {
                if (mBlueAdapter.isDiscovering()) {
                    ExecutorService threadPool = Executors.newCachedThreadPool();
                    Future<Boolean> futureTask = threadPool.submit(() -> mBlueAdapter.cancelDiscovery());

                    while (!futureTask.isDone()) {
                        System.out.println("FutureTask is not finished yet...");
                    }

                    boolean result;
                    try {
                        result = futureTask.get();
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    threadPool.shutdown();
                    if (result) {
                        // unregistering the ACTION_FOUND receiver from discovering of devices
                        if (blueDevices != null) {
                            unregisterReceiver(blueDevices);
                        }
                        discoverDevices();
                    }
                }
            }
        });

        goLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sendMessages != null) {
                    sendMessages.write("left".getBytes());
                    Log.d("messages", "Go left");
                }
            }
        });

        goRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sendMessages != null) {
                    sendMessages.write("right".getBytes());
                    Log.d("messages", "Go right");
                }
            }
        });

        goFront.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sendMessages != null) {
                    sendMessages.write("front".getBytes());
                    Log.d("messages", "Go front");
                }
            }
        });

        goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sendMessages != null) {
                    sendMessages.write("back".getBytes());
                    Log.d("messages", "Go back");
                }
            }
        });

        goUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sendMessages != null) {
                    sendMessages.write("up".getBytes());
                    Log.d("messages", "Go up");
                }
            }
        });

        goDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sendMessages != null) {
                    sendMessages.write("down".getBytes());
                    Log.d("messages", "Go down");
                }
            }
        });
    }

    private void setRecyclers(boolean discovered) {
        if (discovered) {
            discoveredDevicesAdapter = new DiscoveredDevicesAdapter(availableDevices, getApplicationContext(), MainActivity.this);
            discoveredRecyclerView.setAdapter(discoveredDevicesAdapter);
        }
        pairedDevicesAdapter = new PairedDevicesAdapter(pairedDevices, pairedStatus, getApplicationContext(), this);
        pairedRecyclerView.setAdapter(pairedDevicesAdapter);
    }

    @SuppressLint("MissingPermission")
    private void discoverDevices() {
        // Getting the already known / paired devices;
        pairedDevices = new ArrayList<>(mBlueAdapter.getBondedDevices());
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice pairedDevice : pairedDevices) {
                pairedStatus.put(pairedDevice, "offline");
            }
            setRecyclers(false);
        }

        // Getting the devices that aren't paired but are available
        mBlueAdapter.startDiscovery();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        blueDevices = new BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Discovery has found a device. Get the BluetoothDevice object and its info from the Intent.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (pairedDevices.contains(device)) {
                        pairedStatus.put(device, "online");
                    } else {
                        availableDevices.add(device);
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    setRecyclers(true);
                }
            }
        };
        registerReceiver(blueDevices, filter);
    }

    private void managePermissions() {
        // Checking if the permission for bluetooth connect is given or not; Because it is necessary before we make a request for bluetooth;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = {"android.permission.BLUETOOTH_CONNECT", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"};
                requestPermissions(permissions, Constants.REQUEST_BASE_PERMISSIONS); // performs the requesting of the permission; result is sent to onRequestPermissionsResult();
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = {"android.permission.BLUETOOTH_CONNECT", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"};
                requestPermissions(permissions, Constants.REQUEST_BASE_PERMISSIONS); // performs the requesting of the permission; result is sent to onRequestPermissionsResult();
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, @NonNull int[] grantResults) {
        if (permissions.length != 0 && grantResults.length != 0) {
            if (requestCode == Constants.REQUEST_BASE_PERMISSIONS) {
                for (int num : grantResults) {
                    if (num != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                initialBlueSetup();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // unregistering the adapter state receiver
        if (blueState != null) {
            unregisterReceiver(blueState);
        }
        // unregistering the ACTION_FOUND receiver from discovering of devices
        if (blueDevices != null) {
            unregisterReceiver(blueDevices);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void makeConnection(BluetoothDevice device) {
        String conStatus = device.getName() + " to be connected";
        blueConStatus.setText(conStatus);
        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            device.createBond();
            BroadcastReceiver bond = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                        final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothAdapter.ERROR);
                        if (state == BluetoothDevice.BOND_BONDED) {
                            availableDevices.remove(device);
                            pairedDevices.add(device);
                            setRecyclers(true);
                            connection = new SocketCreationThread(MainActivity.this, mBlueAdapter, device);
                            try {
                                connection.start();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            };
            registerReceiver(bond, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        } else {
            connection = new SocketCreationThread(this, mBlueAdapter, device);
            connection.start();
        }
    }

    @Override
    public void onSuccess(String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                blueConStatus.setText(message);
                devicesPanel.setVisibility(View.GONE);
                controllerMain.setVisibility(View.VISIBLE);
                sendMessages = new BluetoothService.ConnectedThread(connection.getMainSocket());
            }
        });
    }

    @Override
    public void onFailure(String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                blueConStatus.setText(message);
                devicesPanel.setVisibility(View.GONE);
                controllerMain.setVisibility(View.VISIBLE);
            }
        });
    }
}