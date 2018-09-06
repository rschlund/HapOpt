package edu.teco.schlund.hapopt;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

    private Button startButton;
    private Button[] fingerButtons;
    private ImageView[] pointFingers;
    private int activeFinger;
    private BlueToothService bleService;

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

        //Make sure bluetooth is on
        switchBluetoothOn(this);

        bleService = new BlueToothService(this);
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
    }

    //if gametype is reaction then rungame produces random delays between indication of button to be clicked
    private void runGame(){
        if(gameType.equals(MainActivity.REACTIONGAME)) {
        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
               //Set active finger
                activateSkills();
                startMillis = System.nanoTime();
            }
        }, getDelay());
        } else {
            activateSkills();
            startMillis = System.nanoTime();
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
            return new Random().nextInt(MAXDELAY);
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
    protected boolean setMotor(String hexValue) {
        return bleService.setMotorCharacteristic(hexToByteArray(hexValue.toCharArray()));
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
}
