package com.example.assessment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

public class PointOfInterestActivity extends AppCompatActivity implements View.OnClickListener {

    Double latitude;
    Double longitude;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.create_poi);

        Button button = (Button) findViewById(R.id.createBtn);
        button.setOnClickListener(this);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            latitude = extras.getDouble("latitude");
            longitude = extras.getDouble("longitude");
        }

    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();

        EditText nameEdit = (EditText) findViewById(R.id.nameEdit);
        EditText typeEdit = (EditText) findViewById(R.id.typeEdit);
        EditText descEdit = (EditText) findViewById(R.id.descEdit);

        String name = nameEdit.getText().toString();
        String type = typeEdit.getText().toString();
        String desc = descEdit.getText().toString();


        bundle.putString("name", name);
        bundle.putString("type", type);
        bundle.putString("desc", desc);
        bundle.putDouble("lat", latitude);
        bundle.putDouble("lon", longitude);

        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        finish();
    }

}
