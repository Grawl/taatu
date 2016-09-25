package com.sh1r0.caffe_android_demo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class Tatoos extends AppCompatActivity {

    public void tattooClick(View view) {
        System.out.println(view.getTag().toString());
        Intent intent = new Intent(this, SCamera.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tatoos);
    }
}
