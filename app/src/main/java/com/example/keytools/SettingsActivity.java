package com.example.keytools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;


public class SettingsActivity extends AppCompatActivity {

    public static final String APP_PREFERENCES = "settings";
    public static final String APP_PREFERENCES_PAUSE = "pause";
    public static final String APP_PREFERENCES_NSNIFF = "nsniff";
    public static int pause = 0;
    public static int nsniff = 2;
    private final int defnsniff = 2;
    private final int defpause = 0;

    static SharedPreferences mSettings;
    Toast toast;

    private EditText NumSniff;
    private EditText Pause;


    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        NumSniff= findViewById(R.id.NumSniff);
        Pause= findViewById(R.id.Pause);
        NumSniff.setText(String.format("%d",nsniff));
        Pause.setText(String.format("%d",pause));

        View.OnClickListener oclBtn = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()){
                    case R.id.btnCancel:
                        Cancel();
                        break;
                    case R.id.btnApply:
                        Apply();
                        break;
                }
            }
        };
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnApply = findViewById(R.id.btnApply);
        btnCancel.setOnClickListener(oclBtn);
        btnApply.setOnClickListener(oclBtn);

    }


    @SuppressLint("DefaultLocale")
    private void Cancel(){
        NumSniff.setText(String.format("%d",nsniff));
        Pause.setText(String.format("%d",pause));
        nsniff = defnsniff;
        pause = defpause;
        SaveSettings(this);
        this.finish();
    }


    @SuppressLint("DefaultLocale")
    private  void Apply(){

        try {
            int ns = Integer.parseInt(NumSniff.getText().toString());
            if(ns < 2){
                toast = Toast.makeText(this, R.string.Ошибка_ввода_Число_захватов_меньше_2, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                ns = 2;
                NumSniff.setText(String.format("%d",nsniff));
            }
            nsniff = ns;

            int np = Integer.parseInt(Pause.getText().toString());
            if(np > 1500){
                toast = Toast.makeText(this, "Пауза не должна быть больше 1500 !", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                np = 1500;
                Pause.setText(String.format("%d",pause));
            }
            pause = np;
            SaveSettings(this);
            this.finish();

        } catch (NumberFormatException e) {
            Toast toast = Toast.makeText(this, "Ошибка ввода !\n" + e.toString(), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public static boolean LoadSettings(Context context){

        mSettings = context.getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);
        pause = mSettings.getInt(APP_PREFERENCES_PAUSE, pause);
        nsniff = mSettings.getInt(APP_PREFERENCES_NSNIFF, nsniff);

        return true;
    }

    public static boolean SaveSettings(Context context){

        mSettings = context.getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor SetEditor = mSettings.edit();
        SetEditor.putInt(APP_PREFERENCES_PAUSE,pause);
        SetEditor.putInt(APP_PREFERENCES_NSNIFF,nsniff);
        SetEditor.apply();

        return true;
    }

}
