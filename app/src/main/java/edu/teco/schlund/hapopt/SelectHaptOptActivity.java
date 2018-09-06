package edu.teco.schlund.hapopt;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

import static edu.teco.schlund.hapopt.BluetoothOn.switchBluetoothOn;

public class SelectHaptOptActivity extends AppCompatActivity {

    final static int CONNECTIONLOST = 13;
    final static int NOBLUETOOTH = 14;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_hapt_opt);
        switchBluetoothOn(this);
    }

    //Triggered by next-Button, sends necessary data to Playground
    public void sendMessage(View view)
    {
        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        Intent intent = new Intent(this, PlayGroundActivity.class);
        intent.putExtra("GameSkill", getResources().getResourceEntryName(radioGroup.getCheckedRadioButtonId()));
        intent.putExtra("GameType", getIntent().getStringExtra("GameType"));
        startActivityForResult(intent, 2);
    }

    @Override
    protected void onResume(){
        super.onResume();
        switchBluetoothOn(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        // check if the request code is same as what is passed  here it is 2
        if(requestCode==2) {
            if(resultCode == CONNECTIONLOST) {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setMessage("Verbindung abgebrochen!").setTitle("Bluetooth Connectivity");
                alert.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                alert.show();
            }
            if(resultCode == NOBLUETOOTH) {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setMessage("Bluetooth kann nicht gestartet werden!").setTitle("Bluetooth Connectivity");
                alert.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                alert.show();
            }
        }
    }
}
