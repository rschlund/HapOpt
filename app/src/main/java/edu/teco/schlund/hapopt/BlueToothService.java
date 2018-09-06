package edu.teco.schlund.hapopt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.UUID;

public class BlueToothService extends Service {

    private final static String TAG = BlueToothService.class.getSimpleName();

    //High level manager used to obtain an instance of an BluetoothAdapter and to conduct overall Bluetooth Management
    private BluetoothManager mBluetoothManager;

    /**
     *This class provides methods to perform scan related operations for Bluetooth LE devices. An application can scan for a particular
     * type of Bluetooth LE devices using ScanFilter. It can also request different types of callbacks for delivering the result.
     */
    private BluetoothLeScanner btScanner;

    //This class provides Bluetooth GATT functionality to enable communication with Bluetooth Smart or Smart Ready devices.
    //Can be used to initiate connection to devices found by BluetoothLeScanner
    private BluetoothGatt mBluetoothGatt;
    //Characteristic for switching motors on and off
    private BluetoothGattCharacteristic motorControlCharacteristic;

    private final static UUID HAPTOPT_SERVICE_UUID =
            UUID.fromString("713d0000-503e-4c75-ba94-3148f18d941e");
    private final static UUID CHARACTERISTIC_UUID =
            UUID.fromString("713d0003-503e-4c75-ba94-3148f18d941e");

    private BluetoothDevice gloveDevice;


    private Handler mHandler = new Handler();
    //Defines the scan period for BLE devices
    private static final long SCAN_PERIOD = 5000;

    private ProgressDialog progressDialog;
    private Activity activity;

    public BlueToothService(Activity activity) {
        this.activity = activity;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void startBluetoothDetection(){
        if(mBluetoothGatt!= null && gloveDevice != null){
            connect();
        } else {
        /*
        * Represents the local device Bluetooth adapter. The BluetoothAdapter lets you perform fundamental Bluetooth tasks,
        * such as initiate device discovery, query a list of bonded (paired) devices, instantiate a BluetoothDevice using a known MAC address,
        * and create a BluetoothServerSocket to listen for connection requests from other devices, and start a scan for Bluetooth LE devices.
        * */
            BluetoothAdapter mBluetoothAdapter;
            // For API level 18 and above, get a reference to BluetoothAdapter through
            // BluetoothManager.
            mBluetoothManager = (BluetoothManager) getSystemService(android.content.Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager != null) {
                mBluetoothAdapter = mBluetoothManager.getAdapter();
                try {
                    btScanner = mBluetoothAdapter.getBluetoothLeScanner();
                } catch (NullPointerException e) {
                    Intent intent = new Intent();
                    activity.setResult(SelectHaptOptActivity.NOBLUETOOTH, intent);
                    activity.finish();//finishing activity
                    Log.d(TAG, "Status Fehler!");
                    e.printStackTrace();
                }
                startDetectingDevices();
            } else {
                Intent intent = new Intent();
                activity.setResult(SelectHaptOptActivity.NOBLUETOOTH, intent);
                activity.finish();
            }
        }
    }

    // Device scan callback.
    private ScanCallback bleScanCallback = new ScanCallback() {
        // Is triggered for every Bluetooth device found
        // Teco Devices will be identified by device name
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if(device.getName() != null && device.getName().contains("TECO Wearable 4")) {
                gloveDevice = device;
            }
        }
    };


    /*
    * Scanning for Devices
    */
    void startDetectingDevices() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(bleScanCallback);
            }
        });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        btScanner.stopScan(bleScanCallback);
                    }
                });
                if(gloveDevice!= null) {
                    connect();
                } else {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                    alert.setMessage("Handschuh nicht gefunden! Nochmal versuchen?").setTitle("Bluetooth Connectivity");
                    alert.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            progressDialog.show();
                            startDetectingDevices();
                        }
                    });
                    alert.setNegativeButton("Nein", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            activity.finish();
                        }
                    });
                    alert.show();
                }
            }
        }, SCAN_PERIOD);

    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     *
     */
    private void connect( ) {
        // if previously connected, try to reconnect.
        if(mBluetoothGatt!= null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (!mBluetoothGatt.connect()) {
                Log.w(TAG, "Unable to reconnect to device.");
                Intent intent = new Intent();
                activity.setResult(SelectHaptOptActivity.CONNECTIONLOST, intent);
                activity.finish();//finishing activity
            }
            //Not yet connected
        } else {
            // We want to directly connect to the device, so we are setting the autoConnect
            // parameter to false.
            mBluetoothGatt = gloveDevice.connectGatt(this, false, mGattCallback);
            Log.d(TAG, "Trying to create a new connection.");
        }

        if(mBluetoothGatt == null){
            Log.w(TAG, "Unable to connect to device");
            Intent intent = new Intent();
            activity.setResult(SelectHaptOptActivity.CONNECTIONLOST, intent);
            activity.finish();//finishing activity
        }
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState){
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(TAG, "Connected to GATT server.");
                    mBluetoothGatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i(TAG, "Disconnected from GATT server.");
                    break;
            }
        }

        //The necessary services for running game has been discovered -> ready to start
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = mBluetoothGatt.getService(HAPTOPT_SERVICE_UUID);
                motorControlCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                Intent intent=new Intent();
                activity.setResult(SelectHaptOptActivity.CONNECTIONLOST,intent);
                activity.finish();//finishing activity
            }
        }

        //Unnecessary part as data are only written not read
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        }
    };

}
