package com.yang.overscrolllayout.demo;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yang.overscrolllayout.OverScrollLayout;

public class RecyclerViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int layoutId = getIntent().getIntExtra("layoutId", 0);
        setContentView(layoutId);

        OverScrollLayout overScrollLayout = findViewById(R.id.overScrollLayout);
        TextView tvTrack = findViewById(R.id.tvTrack);

        overScrollLayout.addOnOverScrollListener((axes, translation, actualTranslation) -> {
            if (translation > 0) {
                tvTrack.setText("向下过度滚动：" + actualTranslation + "  无阻尼值：" + translation);
            } else {
                tvTrack.setText("向上过度滚动：" + actualTranslation + "  无阻尼值：" + translation);
            }
        });


        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        SimpleAdapter adapter = new SimpleAdapter(ViewGroup.LayoutParams.MATCH_PARENT, 200, 20);
        adapter.setOnClickItemListener(position -> {
            Toast.makeText(RecyclerViewActivity.this, "您点击了" + position + "个", Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(adapter);

    }
}