package edu.teco.schlund.hapopt;

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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.UUID;

public class BlueToothService extends Service {

    private final static String TAG = BlueToothService.class.getSimpleName();
    public final static String BLEERROR = "Bluetoothfehler! Nochmal versuchen?";
    public final static String BLEOK = "BLEOK";

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

    public BlueToothService() { }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerReceiver(playGroundUpdateReceiver,playGroundUpdateIntentFilter());
        startBluetoothDetection();
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {}

    @Override
    public IBinder onBind(Intent intent) {
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
                    broadcastUpdate(BLEERROR);
                    Log.d(TAG, "Status Fehler!");
                    e.printStackTrace();
                }
                startDetectingDevices();
            } else {
                broadcastUpdate(BLEERROR);
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
                    broadcastUpdate(BLEERROR);
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
                broadcastUpdate(BLEERROR);
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
            broadcastUpdate(BLEERROR);
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
                    broadcastUpdate(BLEOK);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i(TAG, "Disconnected from GATT server.");
                    broadcastUpdate(BLEERROR);
                    break;
            }
        }

        //The necessary services for running game has been discovered -> ready to start
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = mBluetoothGatt.getService(HAPTOPT_SERVICE_UUID);
                motorControlCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                broadcastUpdate(BLEOK);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                broadcastUpdate(BLEERROR);
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

    //Sets motor indicating which finger button should be clicked
    protected boolean setMotorCharacteristic(byte[] byteValue) {
        //BluetoothManager ble_manager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        //if (mBluetoothGatt != null && ble_manager!= null && ble_manager.getConnectionState(mBluetoothGatt.getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED) {
        if (mBluetoothGatt != null && mBluetoothManager!= null && mBluetoothManager.getConnectionState(mBluetoothGatt.getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED) {
            motorControlCharacteristic.setValue(byteValue);
            boolean status = mBluetoothGatt.writeCharacteristic(motorControlCharacteristic);
            //Device returns error
            if (!status) {
                Log.d(TAG, "Status Fehler!");
                broadcastUpdate(BLEERROR);
                return false;
            }
            return true;
        } else {
            broadcastUpdate(BLEERROR);
            return false;
        }
    }

    private void broadcastUpdate(final String action) {
        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private final BroadcastReceiver playGroundUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setMotorCharacteristic(intent.getByteArrayExtra(PlayGroundActivity.EXTRA_BYTES));
        }
    };

    private static IntentFilter playGroundUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PlayGroundActivity.ACTION_MOTORDATA);
        return intentFilter;
    }
}
