package edu.teco.schlund.hapopt.bluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.UUID;

public class BluetoothDetectActivity extends AppCompatActivity {
    private final static String TAG = BluetoothDetectActivity.class.getSimpleName();

    private BluetoothLeScanner btScanner;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;


    private final static UUID SMARTAQ_SERVICE_UUID =
            UUID.fromString("6e57fcf9-8064-4995-a3a8-e5ca44552192");
    private final static UUID CHARACTERISTIC_UUID =
            UUID.fromString("7a812f99-06fa-4d89-819d-98e9aafbd4ef");
    private final static UUID DESCRIPTOR_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    public final static String ACTION_GATT_CONNECTED =
            "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "ACTION_DATA_AVAILABLE";


    private final static int REQUEST_ENABLE_BT = 1;

    private String bleDeviceAdress;
    private Intent gattServiceIntent;
    private BluetoothDevice gloveDevice;

    // Stops scanning after 15 seconds.
    private Handler mHandler = new Handler();
    private static final long SCAN_PERIOD = 20000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BluetoothAdapter btAdapter = ((BluetoothManager) getSystemService(android.content.Context.BLUETOOTH_SERVICE)).getAdapter();
        //Check if Bluetooth is supported
        if (btAdapter == null) {
            // Device does not support Bluetooth
            final Activity activity = (Activity) this;
            AlertDialog.Builder alert = new AlertDialog.Builder(activity);
            alert.setMessage("Bluetooth not supported!").setTitle("Bluetooth Connectivity");
            alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                    activity.finish();
                }
            });
            alert.show();
        }
        try {
            btScanner = btAdapter.getBluetoothLeScanner();
        } catch (NullPointerException e){
            //TODO: Fehler behandeln
        }
        /*Activate Bluetooth if not active*/
        if (!btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

    }

    // Device scan callback.
    private ScanCallback bleScanCallback = new ScanCallback() {
        // Is triggered for every Bluetooth device found
        // Teco Devices will be identified by device name
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if(device.getName() != null && device.getName().contains("TECO-AQNode")) {
            }
        }
    };

    /*
     * Scanning for SmartAQNet Services
     */
    public void startDetectingDevices() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(new ScanCallback() {
                    // Is triggered for every Bluetooth device found
                    // Teco Devices will be identified by device name
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        BluetoothDevice device = result.getDevice();
                        if (device.getName() != null && device.getName().contains("TECO-AQNode")) {
                            gloveDevice = device;
                            btScanner.stopScan(bleScanCallback);
                            connect();
                            //TODO: Is that needed?
//                            AsyncTask.execute(new Runnable() {
//                                @Override
//                                public void run() {
//                                    btScanner.stopScan(bleScanCallback);
//                                }
//                            });
                        }
                    }
                }
                );
            }
        });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(bleScanCallback);
                //TODO: Is that needed?
//                AsyncTask.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        btScanner.stopScan(bleScanCallback);
//                    }
//                });
                //TODO: return to SelectHaptOptActivity
                //noDeviceDetected();
            }
        }, SCAN_PERIOD);
    }

    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect( ) {
        // Previously connected device.  Try to reconnect.
        final String address = gloveDevice.getAddress();
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            switch (newState){
                case BluetoothProfile.STATE_CONNECTED:
                    intentAction = ACTION_GATT_CONNECTED;
                    Log.i(TAG, "Connected to GATT server.");
                    mBluetoothGatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    intentAction = ACTION_GATT_DISCONNECTED;
                    Log.i(TAG, "Disconnected from GATT server.");
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = mBluetoothGatt.getService(SMARTAQ_SERVICE_UUID);
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        }
    };

}
