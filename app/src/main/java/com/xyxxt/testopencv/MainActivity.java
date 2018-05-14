package com.xyxxt.testopencv;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void btnCamara(View view){
        startActivity(new Intent(this, OpenCVCamera.class));
    }

    public void btnImage(View view){
        startActivity(new Intent(this, OpenCVImage.class));
    }
}
