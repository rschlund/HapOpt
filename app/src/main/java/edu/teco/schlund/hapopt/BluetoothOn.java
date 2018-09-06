package edu.teco.schlund.hapopt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.DialogInterface;
import android.content.Intent;

/**
 * Created by Rainer on 06.09.2018.
 */

class BluetoothOn {

    private final static int REQUEST_ENABLE_BT = 1;

    static public void switchBluetoothOn(Activity activity){
        BluetoothAdapter btAdapter = ((BluetoothManager) activity.getSystemService(android.content.Context.BLUETOOTH_SERVICE)).getAdapter();
        //Check if Bluetooth is supported
        if (btAdapter == null) {
            // Device does not support Bluetooth
            AlertDialog.Builder alert = new AlertDialog.Builder(activity);
            alert.setMessage("Bluetooth wird von diesem Gerät nicht unterstützt!").setTitle("Bluetooth Connectivity");
            alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                    System.exit(0);
                }
            });
            alert.show();
        }
         /*Activate Bluetooth if not active*/
        if (!btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }
}
