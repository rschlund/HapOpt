package edu.teco.schlund.hapopt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;
import java.util.UUID;

import edu.teco.schlund.hapopt.bluetooth.BluetoothDetectActivity;

public class PlayGroundActivity extends AppCompatActivity {

    final private int REACTIONRUNS = 15;
    final private int SKILLRUNS = 50;
    final private int FINGERS = 4;
    final private int MAXDELAY = 3000;
    final private int STARTTIMETOLICK = 1000;
    final private double REDUCETIMETOCLICK = 0.96;
    final private int SKILLDELAY = 250;

    private Handler delayHandler = new Handler();
    private long startMillis;
    private long countMillis;
    private int runs;
    private int errorCount;
    private String gameType;
    private String gameSkills;
    private int timeToClick;
    private boolean clicked;
    private ProgressDialog progressDialog;

    private Button startButton;
    private Button[] fingerButtons;
    private ImageView[] pointFingers;
    private int activeFinger;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_ground);
        fingerButtons = new Button[FINGERS];
        fingerButtons[0] = findViewById(R.id.finger1Button);
        fingerButtons[1] = findViewById(R.id.finger2Button);
        fingerButtons[2] = findViewById(R.id.finger3Button);
        fingerButtons[3] = findViewById(R.id.finger4Button);
        initFingerButtons();
        pointFingers = new ImageView[FINGERS];
        pointFingers[0] =  findViewById(R.id.pointFinger1);
        pointFingers[0].setVisibility(View.INVISIBLE);
        pointFingers[1] = findViewById(R.id.pointFinger2);
        pointFingers[1].setVisibility(View.INVISIBLE);
        pointFingers[2] = findViewById(R.id.pointFinger3);
        pointFingers[2].setVisibility(View.INVISIBLE);
        pointFingers[3] = findViewById(R.id.pointFinger4);
        pointFingers[3].setVisibility(View.INVISIBLE);
        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(startButtonListener);
    }

    private void initFingerButtons(){
        for(int i = 0; i < FINGERS; i++){
            fingerButtons[i].setClickable(false);
            fingerButtons[i].setOnClickListener(fingerClickListener);
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
        startButton.setVisibility(View.INVISIBLE);
        startButton.setEnabled(true);
        startButton.setClickable(true);
        gameType = getIntent().getStringExtra("GameType");
        gameSkills = getIntent().getStringExtra("GameSkill");
        TextView advice = findViewById(R.id.adviceText);
        if(gameType.equals(MainActivity.SKILLGAME))
            advice.setText("Klick den Button bevor der Pfeil verschwindet!");
        advice.setVisibility(View.VISIBLE);
        if(gameSkills.equals("radioHapt")||gameSkills.equals("radioBoth")) {
            progressDialog = ProgressDialog.show(this, "Working..", "Trying to connect to BLEDevice...", true,
                    false);
            startBluetoothDetection();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        startButton.setVisibility(View.VISIBLE);
        startButton.setEnabled(true);
        startButton.setClickable(true);
        //TODO: checken welches Spiel
        //TODO: Views setzen
        //TODO Button wählen
        //TODO: Zeit starten
        //TODO: Pfeil anzeigen
        //TODO: Richtiger Button geklickt? Ja Zeit stoppen, nein Fehler zählen
        //TODO: Versuche hochzählen -> Button wählen
    }

    private void runGame(){
        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
               //Set active finger
                activateSkills();
                startMillis = System.nanoTime();
            }
        }, getDelay());
    }

    private void activateSkills(){
        activeFinger = (int) (Math.random() * 4);
        activateFingerButtons(true);
        switch (gameSkills) {
            case "radioBoth":
            case "radioOpt":
                pointFingers[activeFinger].setVisibility(View.VISIBLE);
                if(gameType.equals(MainActivity.SKILLGAME))
                    runTimetoClick();
                break;
            case "radioHapt":
                break;
            default:
                break;
        }
    }

    private void runTimetoClick(){
        timeToClick = (int) Math.round(timeToClick * REDUCETIMETOCLICK);
        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!clicked) {
                    deactivateSkills();
                    if (runs < SKILLRUNS) {
                        errorCount++;
                        runs++;
                        runGame();
                    } else
                        stopGame();
                } else {
                    clicked = false;
                    runGame();
                }
            }
        }, timeToClick);
    }

    private void deactivateSkills(){
        activateFingerButtons(false);
        switch (gameSkills) {
            case "radioBoth":
            case "radioOpt":
                pointFingers[activeFinger].setVisibility(View.INVISIBLE);
                break;
            case "radioHapt":
                break;
            default:
                break;
        }
    }

    private int getDelay(){

        switch (gameType){
            case MainActivity.REACTIONGAME:
                return new Random().nextInt(MAXDELAY);
            case MainActivity.SKILLGAME:
                return SKILLDELAY;
        }
        return 0;
    }

    private void activateFingerButtons(boolean activate){
        for(int i = 0; i < FINGERS; i++){
            fingerButtons[i].setClickable(activate);
        }
    }

    private View.OnClickListener fingerClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //Check which button is clicked
            fingerButtonClicked(v.getId());
        }
    };

    private void fingerButtonClicked(int buttonID){

        clicked = true;
        long stopTime = System.nanoTime();
        deactivateSkills();

        switch(gameType){
            case MainActivity.REACTIONGAME:
                if(runs < REACTIONRUNS){
                    runs++;
                    if(buttonID == fingerButtons[activeFinger].getId())
                        countMillis = countMillis + (stopTime - startMillis);
                    else
                        errorCount++;
                    runGame();
                } else {
                    stopGame();
                }
                break;
            case MainActivity.SKILLGAME:
                if(runs < SKILLRUNS) {
                    runs++;
                    runs++;
                    if(buttonID == fingerButtons[activeFinger].getId())
                        countMillis = countMillis + (stopTime - startMillis);
                    else
                        errorCount++;
                } else stopGame();
                break;
            default:
                break;
        }
    }

    private void stopGame(){
        Intent intent = new Intent(this, ResultActivity.class);
        Double averageMillis = countMillis * 1.0/(runs*1000000);
        averageMillis = Math.round(averageMillis*100.0)/100.0;
        intent.putExtra("GameType", getIntent().getStringExtra("GameType"));
        intent.putExtra("ReactionTime", String.valueOf(averageMillis));
        intent.putExtra("Errors", String.valueOf(errorCount));
        startActivity(intent);

    }
    private View.OnClickListener startButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            findViewById(R.id.adviceText).setVisibility(View.INVISIBLE);
            errorCount = 0;
            runs = 0;
            countMillis = 0;
            clicked = false;
            timeToClick = STARTTIMETOLICK;
            startButton.setVisibility(View.INVISIBLE);
            startButton.setEnabled(false);
            startButton.setClickable(false);
            Toast toast = Toast.makeText(PlayGroundActivity.this, "Los geht's!", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.TOP,0,200);
            toast.show();
            delayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    runGame();
                }
            }, Toast.LENGTH_SHORT);
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////BLE AREA /////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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



    public void startBluetoothDetection(){
        BluetoothAdapter btAdapter = ((BluetoothManager) getSystemService(android.content.Context.BLUETOOTH_SERVICE)).getAdapter();
        //Check if Bluetooth is supported
        if (btAdapter == null) {
            // Device does not support Bluetooth
            progressDialog.dismiss();
            final Activity factivity = (Activity) this;
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setMessage("Bluetooth not supported!").setTitle("Bluetooth Connectivity");
            alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                    factivity.finish();
                }
            });
            alert.show();
            //TODO: Fehlerbehandlung
            return;
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
        initialize();
    }


    // Device scan callback.
    private ScanCallback bleScanCallback = new ScanCallback() {
        // Is triggered for every Bluetooth device found
        // Teco Devices will be identified by device name
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if(device.getName() != null && device.getName().contains("TECO-AQNode")) {
                gloveDevice = device;
                btScanner.stopScan(bleScanCallback);
//                AsyncTask.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        btScanner.stopScan(bleScanCallback);
//                    }
//                });
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
                btScanner.startScan(bleScanCallback);
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

    public void initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                //TODO:Fehler abfangen
                return;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            //TODO: Fehler abfangen
            return;
        }

        connect();
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
    public void connect( ) {
        // Previously connected device.  Try to reconnect.
        final String address = gloveDevice.getAddress();
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return;
            } else {
                //TODO:Fehler behandeln
                return;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            //TODO:Fehler behandeln
            return;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(progressDialog.isShowing())
                    progressDialog.dismiss();
            }
        }, SCAN_PERIOD);

        mBluetoothDeviceAddress = address;
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
                    progressDialog.dismiss();
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
