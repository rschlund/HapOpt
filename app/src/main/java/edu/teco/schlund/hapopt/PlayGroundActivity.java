package edu.teco.schlund.hapopt;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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

import static edu.teco.schlund.hapopt.BluetoothOn.switchBluetoothOn;

public class PlayGroundActivity extends AppCompatActivity {

    private final static String TAG = PlayGroundActivity.class.getSimpleName();

    final static public String EXTRA_BYTES = "EXTRA_BYTES";
    final static public String EXTRA_ASIZE = "EXTRA_ASIZE";
    final static public String ACTION_MOTORDATA = "ACTION_MOTORDATA";
    final static public String STARTDETECTION = "STARTDETECTION";

    final private int SKILLRUNS;
    final private int REACTIONRUNS;
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

    private Button startButton;
    private Button[] fingerButtons;
    private ImageView[] pointFingers;
    private int activeFinger;

    private Activity myActivity;
    private Intent bleServiceIntent;

    private Boolean bleError = false;
    private ProgressDialog progressDialog;

    public PlayGroundActivity() {
        STARTTIMETOLICK = 1000;
        FINGERS = 4;
        SKILLRUNS = 40;
        REACTIONRUNS = 15;
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
        myActivity = this;
        //Make sure bluetooth is on
        switchBluetoothOn(this);

        try {
            registerReceiver(bleUpdateReceiver, bleUpdateIntentFilter());
        } catch (Exception e){
            Log.d(TAG, "registered");
        }
        if (!isMyServiceRunning(BlueToothService.class)) {
            bleServiceIntent = new Intent(this, BlueToothService.class);
            startService(bleServiceIntent);
        } else {
            broadcastUpdate(STARTDETECTION);
        }
        progressDialog = ProgressDialog.show(myActivity, "In Arbeit...", "Suche Handschuh...", true,
                false);
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
    }

    @Override
    protected void onStart(){
        super.onStart();
        setStartButton();
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
        bleError = true;
        deactivateSkills();
        unregisterReceiver(bleUpdateReceiver);
    }

    @Override
    protected void onPause(){
        super.onPause();
        bleError = true;
        deactivateSkills();
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

    //Activates buttoon to start game
    private void setStartButton(){
        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(startButtonListener);
        startButton.setVisibility(View.VISIBLE);
        startButton.setEnabled(true);
        startButton.setClickable(true);
    }

    //if gametype is reaction then rungame produces random delays between indication of button to be clicked
    //if gametype is skill rungame defines a gap between two laps
    private void runGame(){
        if(!bleError) {
            delayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Set active finger
                    activateSkills();
                    startMillis = System.nanoTime();
                }
            }, getDelay());
        } else {
            bleError = false;
        }
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

    //Activates skills for a lap in game
    private void activateSkills(){
        activeFinger = (int) (Math.random() * 4);
        activateFingerButtons(true);
        switch (gameSkills) {
            case "radioBoth":
                pointFingers[activeFinger].setVisibility(View.VISIBLE);
                setMotor(MOTORS[activeFinger]);
                break;
            case "radioOpt":
                pointFingers[activeFinger].setVisibility(View.VISIBLE);
                break;
            case "radioHapt":
                setMotor(MOTORS[activeFinger]);
                break;
            default:
                break;
        }
        if(gameType.equals(MainActivity.SKILLGAME))
            runTimetoClick();
    }

    //Deactivates skills between two laps in game
    private void deactivateSkills(){
        activateFingerButtons(false);
        switch (gameSkills) {
            case "radioBoth":
                pointFingers[activeFinger].setVisibility(View.INVISIBLE);
                setMotor(MOTORS[4]);
                break;
            case "radioOpt":
                pointFingers[activeFinger].setVisibility(View.INVISIBLE);
                break;
            case "radioHapt":
                setMotor(MOTORS[4]);
                break;
            default:
                break;
        }
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
        deactivateSkills();
        int maxRuns;
        if(gameType.equals(MainActivity.REACTIONGAME))
            maxRuns = REACTIONRUNS;
        else
            maxRuns = SKILLRUNS;
        if (runs < maxRuns) {
            runs++;
            if (buttonID == fingerButtons[activeFinger].getId())
                countMillis = countMillis + (stopTime - startMillis);
            else
                errorCount++;
            if(gameType.equals(MainActivity.REACTIONGAME))
                runGame();
        } else {
            stopGame();
        }
    }

    //Collects results from a game run
    private void stopGame(){
        if (!bleError) {
            Intent intent = new Intent(this, ResultActivity.class);
            Double averageMillis = 0.0;
            if (runs - errorCount > 0) {
                averageMillis = countMillis * 1.0 / ((runs - errorCount) * 1000000);
                averageMillis = Math.round(averageMillis * 100.0) / 100.0;
            }
            intent.putExtra("GameType", getIntent().getStringExtra("GameType"));
            intent.putExtra("ReactionTime", String.valueOf(averageMillis));
            intent.putExtra("Errors", String.valueOf(errorCount));
            startActivity(intent);
        } else {
            bleError = false;
        }
    }

    //Listener for startButton, initializes values and runs game
    private View.OnClickListener startButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            bleError = false;
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
    protected void setMotor(String hexValue) {
        broadcastUpdate(ACTION_MOTORDATA, hexToByteArray(hexValue.toCharArray()));
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

    private void broadcastUpdate(final String action, byte[] motorData) {
        Intent intent = new Intent(action);
        intent.putExtra(EXTRA_BYTES, motorData);
        intent.putExtra(EXTRA_ASIZE, motorData.length);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action) {
        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    //Receives Error Messages from BlueToothservice
    private final BroadcastReceiver bleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                //Something is Wrong with ble
                case BlueToothService.BLEERROR:
                    bleError = true;
                    if(progressDialog == null){
                        progressDialog = ProgressDialog.show(myActivity, "In Arbeit...", "Suche Handschuh...", true,
                                false);
                    }
                    if(progressDialog.isShowing()) progressDialog.dismiss();
                    AlertDialog.Builder alert = new AlertDialog.Builder(myActivity);
                    alert.setMessage(action).setTitle("Bluetooth Connectivity");
                    alert.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            //Try again to connect device
                            if(isMyServiceRunning(BlueToothService.class)) {
                                bleServiceIntent = new Intent(myActivity, BlueToothService.class);
                                startService(bleServiceIntent);
                            }else {
                                broadcastUpdate(STARTDETECTION);
                            }
                            setStartButton();
                            progressDialog.show();
                        }
                    });
                    alert.setNegativeButton("Nein", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            myActivity.finish();
                        }
                    });

                    alert.show();
                    break;
                case BlueToothService.BLEOK:
                    if(progressDialog.isShowing()) progressDialog.dismiss();
                    break;
            }
        }
    };

    private static IntentFilter bleUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BlueToothService.BLEERROR);
        intentFilter.addAction(BlueToothService.BLEOK);
        return intentFilter;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (service != null && serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
            return false;
    }

}
