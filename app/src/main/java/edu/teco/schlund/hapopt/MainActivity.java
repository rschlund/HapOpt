package edu.teco.schlund.hapopt;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import static edu.teco.schlund.hapopt.BluetoothOn.switchBluetoothOn;

public class MainActivity extends AppCompatActivity {

    final static public String SKILLGAME = "SKILLGAME";
    final static public String REACTIONGAME = "REACTIONGAME";
    private MainActivity mainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = this;
        //Make sure Bluetooth is on
        switchBluetoothOn(this);
        registerReceiver(bleUpdateReceiver, bleUpdateIntentFilter());
        Intent bleServiceIntent = new Intent(this, BlueToothService.class);
        startService(bleServiceIntent);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume(){
        super.onResume();
        getApplication();
        //Make sure Bluetooth is on
        switchBluetoothOn(this);
    }

    //Triggered by next-Button, sends necessary data to SelectHaptOpt
    public void sendMessageFast(View view) {
        RadioGroup radioGroup = findViewById(R.id.radioGameType);
        Intent intent = new Intent(this, SelectHaptOptActivity.class);
        if(getResources().getResourceEntryName(radioGroup.getCheckedRadioButtonId()).equals("radioFast"))
            intent.putExtra("GameType", REACTIONGAME);
        else
            intent.putExtra("GameType", SKILLGAME);
        startActivity(intent);
    }

    private final BroadcastReceiver bleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(intent.getAction() == BlueToothService.DEVICENOTFOUND) {
                AlertDialog.Builder alert = new AlertDialog.Builder(mainActivity);
                alert.setMessage(action).setTitle("Bluetooth Connectivity");
                alert.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                alert.show();
            } else {

            }
        }
    };

    private static IntentFilter bleUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BlueToothService.NOBLUETOOTH);
        intentFilter.addAction(BlueToothService.CONNECTIONLOST);
        intentFilter.addAction(BlueToothService.DEVICENOTFOUND);
        return intentFilter;
    }

}
