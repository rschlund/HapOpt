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

import static edu.teco.schlund.hapopt.BluetoothOn.switchBluetoothOn;

public class PlayGroundActivity extends AppCompatActivity {

    private final static String TAG = PlayGroundActivity.class.getSimpleName();


    final private int SKILLRUNS;
    final private int FINGERS;
    final private int STARTTIMETOLICK;
    final private double REDUCETIMETOCLICK;
    final private String[] MOTORS;

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
    private int bluetootStatus;

    public PlayGroundActivity() {
        STARTTIMETOLICK = 1000;
        FINGERS = 4;
        SKILLRUNS = 40;
        REDUCETIMETOCLICK = 0.98;
        String MOTORSOFF = "00000000";
        String MOTOR1 = "FF000000";
        String MOTOR2 = "00FF0000";
        String MOTOR3 = "0000FF00";
        String MOTOR4 = "000000FF";
        MOTORS = new String[]{MOTOR1, MOTOR2, MOTOR3, MOTOR4,MOTORSOFF};
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_ground);

        //Make sure bluetooth is turned on
        switchBluetoothOn(this);

        gameType = getIntent().getStringExtra("GameType");
        gameSkills = getIntent().getStringExtra("GameSkill");
        TextView advice = findViewById(R.id.adviceText);
        if(gameSkills.equals("radioOpt")||gameSkills.equals("radioBoth")) {
            advice.setText(R.string.use_arrow);
        } else {
            advice.setText(R.string.use_vibration);
        }
        advice.setVisibility(View.VISIBLE);
        initFingerButtons();

        if(gameSkills.equals("radioHapt")||gameSkills.equals("radioBoth") && bluetootStatus == BluetoothProfile.STATE_DISCONNECTED) {
            progressDialog = ProgressDialog.show(this, "In Arbeit...", "Suche Handschuh...", true,
                    false);
            startBluetoothDetection();
        }
    }

    //Initializes buttons to be pressed during test and Arrows indicating button to be pressed
    private void initFingerButtons(){
        fingerButtons = new Button[FINGERS];
        fingerButtons[0] = findViewById(R.id.finger1Button);
        fingerButtons[1] = findViewById(R.id.finger2Button);
        fingerButtons[2] = findViewById(R.id.finger3Button);
        fingerButtons[3] = findViewById(R.id.finger4Button);
        pointFingers = new ImageView[FINGERS];
        pointFingers[0] =  findViewById(R.id.pointFinger1);
        pointFingers[0].setVisibility(View.INVISIBLE);
        pointFingers[1] = findViewById(R.id.pointFinger2);
        pointFingers[1].setVisibility(View.INVISIBLE);
        pointFingers[2] = findViewById(R.id.pointFinger3);
        pointFingers[2].setVisibility(View.INVISIBLE);
        pointFingers[3] = findViewById(R.id.pointFinger4);
        pointFingers[3].setVisibility(View.INVISIBLE);
        for(int i = 0; i < FINGERS; i++){
            fingerButtons[i].setClickable(false);
            fingerButtons[i].setOnClickListener(fingerClickListener);
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(startButtonListener);
        startButton.setVisibility(View.VISIBLE);
        startButton.setEnabled(true);
        startButton.setClickable(true);
    }

    @Override
    protected void onResume(){
        super.onResume();
        //Make sure bluetooth is on
        switchBluetoothOn(this);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        //Close Gattserver
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    //if gametype is reaction then rungame produces random delays between indication of button to be clicked
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

    //Defines delay for Finger button to be clicked in Skill Game
    //Delay is slowly decreased to increase difficulty
    private void runTimetoClick(){
        timeToClick = (int) Math.round(timeToClick * REDUCETIMETOCLICK);
        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //User didn't click in time Count error
                if(!clicked) {
                    if (deactivateSkills()) {
                        if (runs < SKILLRUNS) {
                            errorCount++;
                            runs++;
                            runGame();
                        } else
                            stopGame();
                    }
                } else {
                    clicked = false;
                    runGame();
                }
            }
        }, timeToClick);
    }

    //Activates skills for one lap in game
    private void activateSkills(){
        activeFinger = (int) (Math.random() * 4);
        activateFingerButtons(true);
        switch (gameSkills) {
            case "radioBoth":
                pointFingers[activeFinger].setVisibility(View.VISIBLE);
                //setMotor(MOTORS[activeFinger]);
                setMotor(MOTORS[activeFinger]);
                break;
            case "radioOpt":
                pointFingers[activeFinger].setVisibility(View.VISIBLE);
                break;
            case "radioHapt":
                //setMotor(MOTORS[activeFinger]);
                setMotor(MOTORS[activeFinger]);
                break;
            default:
                break;
        }
        if(gameType.equals(MainActivity.SKILLGAME))
            runTimetoClick();
    }

    //Deactivates skills between two laps in game
    private boolean deactivateSkills(){
        boolean noError = true;
        activateFingerButtons(false);
        switch (gameSkills) {
            case "radioBoth":
                pointFingers[activeFinger].setVisibility(View.INVISIBLE);
                noError = setMotor(MOTORS[4]);
                break;
            case "radioOpt":
                pointFingers[activeFinger].setVisibility(View.INVISIBLE);
                break;
            case "radioHapt":
                noError = setMotor(MOTORS[4]);
                break;
            default:
                break;
        }
        return noError;
    }

    private int getDelay(){
        int MAXDELAY = 3000;
        int SKILLDELAY = 250;
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

    //Checks which if correct finger button was clicked by user
    private void fingerButtonClicked(int buttonID){
        clicked = true;
        long stopTime = System.nanoTime();
        if (deactivateSkills()) {
            switch (gameType) {
                case MainActivity.REACTIONGAME:
                    int REACTIONRUNS = 15;
                    if (runs < REACTIONRUNS) {
                        runs++;
                        if (buttonID == fingerButtons[activeFinger].getId())
                            countMillis = countMillis + (stopTime - startMillis);
                        else
                            errorCount++;
                        runGame();
                    } else {
                        stopGame();
                    }
                    break;
                case MainActivity.SKILLGAME:
                    if (runs < SKILLRUNS) {
                        runs++;
                        if (buttonID == fingerButtons[activeFinger].getId())
                            countMillis = countMillis + (stopTime - startMillis);
                        else
                            errorCount++;
                    } else {
                        stopGame();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    //Collects results from a game run
    private void stopGame(){
        Intent intent = new Intent(this, ResultActivity.class);
        Double averageMillis = 0.0;
        if (runs-errorCount>0) {
            averageMillis = countMillis * 1.0 / ((runs - errorCount) * 1000000);
            averageMillis = Math.round(averageMillis * 100.0) / 100.0;
        }
        intent.putExtra("GameType", getIntent().getStringExtra("GameType"));
        intent.putExtra("ReactionTime", String.valueOf(averageMillis));
        intent.putExtra("Errors", String.valueOf(errorCount));
        startActivity(intent);

    }

    //Listener for startButton, initializes values and runs game
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
            }, Toast.LENGTH_LONG);
        }
    };

    //Sets motor indicating which finger button should be clicked
    private boolean setMotor(String hexValue) {
        //BluetoothManager ble_manager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        //if (mBluetoothGatt != null && ble_manager!= null && ble_manager.getConnectionState(mBluetoothGatt.getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED) {
        if (mBluetoothGatt != null && mBluetoothManager!= null && mBluetoothManager.getConnectionState(mBluetoothGatt.getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED) {
                byte[] bytes = hexToByteArray(hexValue.toCharArray());
                motorControlCharacteristic.setValue(bytes);
                boolean status = mBluetoothGatt.writeCharacteristic(motorControlCharacteristic);
                //Device returns error
                if (!status) {
                    Intent intent=new Intent();
                    setResult(SelectHaptOptActivity.CONNECTIONLOST,intent);
                    finish();//finishing activity
                    Log.d(TAG, "Status Fehler!");
                    return false;
                }
                return true;
        } else {
            Intent intent=new Intent();
            setResult(SelectHaptOptActivity.CONNECTIONLOST,intent);
            finish();//finishing activity
            return false;
        }
    }

    //Motor speed is given in Hex values and must be turned to byte to be transferred to device
    public static byte[] hexToByteArray(final char[] data) {

        final int len = data.length;

        // Handle empty string - omitted for clarity's sake

        final byte[] out = new byte[len >> 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; j < len; i++) {
            int f =  Character.digit(data[j], 16) << 4;
            j++;
            f = f | Character.digit(data[j], 16);
            j++;
            out[i] = (byte) (f & 0xFF);
        }

        return out;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////BLE AREA /////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //High level manager used to obtain an instance of an BluetoothAdapter and to conduct overall Bluetooth Management
    BluetoothManager mBluetoothManager;

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
                    setResult(SelectHaptOptActivity.NOBLUETOOTH, intent);
                    finish();//finishing activity
                    Log.d(TAG, "Status Fehler!");
                    e.printStackTrace();
                }
                startDetectingDevices();
            } else {
                Intent intent = new Intent();
                setResult(SelectHaptOptActivity.NOBLUETOOTH, intent);
                finish();
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

        final Activity fActivity = this;
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
                    AlertDialog.Builder alert = new AlertDialog.Builder(fActivity);
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
                            fActivity.finish();
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
                setResult(SelectHaptOptActivity.CONNECTIONLOST, intent);
                finish();//finishing activity
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
            setResult(SelectHaptOptActivity.CONNECTIONLOST, intent);
            finish();//finishing activity
        }
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState){
                case BluetoothProfile.STATE_CONNECTED:
                    bluetootStatus = BluetoothProfile.STATE_CONNECTED;
                    Log.i(TAG, "Connected to GATT server.");
                    mBluetoothGatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    bluetootStatus = BluetoothProfile.STATE_DISCONNECTED;
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
                setResult(SelectHaptOptActivity.CONNECTIONLOST,intent);
                finish();//finishing activity
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
