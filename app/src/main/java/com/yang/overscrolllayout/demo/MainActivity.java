package com.yang.overscrolllayout.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View.OnClickListener listener = v -> {
            Toast.makeText(MainActivity.this, v.getId() + "", Toast.LENGTH_SHORT).show();
        };
        findViewById(R.id.tv1).setOnClickListener(listener);
        findViewById(R.id.tv2).setOnClickListener(listener);
        findViewById(R.id.tv3).setOnClickListener(listener);
        findViewById(R.id.tv4).setOnClickListener(listener);
        findViewById(R.id.tv5).setOnClickListener(listener);
    }
}