package edu.teco.schlund.hapopt;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import static edu.teco.schlund.hapopt.BluetoothOn.switchBluetoothOn;


//Shows results from PlayGroundActivity
public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        switchBluetoothOn(this);

        TextView reactionTime = findViewById(R.id.reactionTime);
        TextView skillValue = findViewById(R.id.skillValue);

        reactionTime.setText(getIntent().getStringExtra("ReactionTime") + " ms");
        skillValue.setText(getIntent().getStringExtra("Errors"));
    }

    @Override
    protected void onResume(){
        super.onResume();
        switchBluetoothOn(this);
    }

}
