package com.example.keytools;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class ResumeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.finish();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resume);
    }
}
