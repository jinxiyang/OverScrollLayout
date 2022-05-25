package com.yang.overscrolllayout.demo;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.yang.overscrolllayout.OverScrollLayout;

public class NestedScrollViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nested_scroll_view);

        OverScrollLayout overScrollLayout = findViewById(R.id.overScrollLayout);
        TextView tvTrack = findViewById(R.id.tvTrack);

        overScrollLayout.addOnOverScrollListener((axes, translation, actualTranslation) -> {
            if (translation > 0) {
                tvTrack.setText("向下过度滚动：" + actualTranslation + "  无阻尼值：" + translation);
            } else {
                tvTrack.setText("向上过度滚动：" + actualTranslation + "  无阻尼值：" + translation);
            }
        });
    }
}