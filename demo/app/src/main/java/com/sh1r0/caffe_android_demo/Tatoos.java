package com.sh1r0.caffe_android_demo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class Tatoos extends AppCompatActivity {

    final String[] photos = {"","t01_cuervo.gif", "t02_buho.jpg",
                             "t03_inche_shuriken_loco.gif", "t04_madre_totemica.jpg",
                             "t05_toro.jpeg", "t06_tatuje_abstracto.jpg",
                             "t07_aguila.jpg","t08_dragon.jpg",
                             "t09_espinas.jpg","t10_ni_idea.jpg",
                             "t11_oso.jpg","t12_sol.jpg"};

    public void tattooClick(View view) {
        System.out.println(view.getTag().toString());
        int photoNo = Integer.parseInt(view.getTag().toString());
        Intent intent = new Intent(this, SCamera.class);
        Bundle b = new Bundle();
        b.putString("key", photos[photoNo]);
        intent.putExtras(b);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tatoos);
    }
}
