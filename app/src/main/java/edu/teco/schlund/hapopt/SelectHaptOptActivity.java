package edu.teco.schlund.hapopt;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

public class SelectHaptOptActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_hapt_opt);
    }
    public void sendMessage(View view)
    {
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        Intent intent = new Intent(this, PlayGroundActivity.class);
        intent.putExtra("GameSkill", getResources().getResourceEntryName(radioGroup.getCheckedRadioButtonId()));
        intent.putExtra("GameType", getIntent().getStringExtra("GameType"));
        startActivity(intent);
    }
}
