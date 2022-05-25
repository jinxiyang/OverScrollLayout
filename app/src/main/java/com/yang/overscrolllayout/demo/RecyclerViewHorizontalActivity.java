package com.yang.overscrolllayout.demo;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yang.overscrolllayout.OverScrollLayout;

public class RecyclerViewHorizontalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view_horizontal);

        OverScrollLayout overScrollLayout = findViewById(R.id.overScrollLayout);
        TextView tvTrack = findViewById(R.id.tvTrack);

        overScrollLayout.addOnOverScrollListener((axes, translation, actualTranslation) -> {
            if (translation > 0) {
                tvTrack.setText("向右过度滚动：" + actualTranslation + "  无阻尼值：" + translation);
            } else {
                tvTrack.setText("向左过度滚动：" + actualTranslation + "  无阻尼值：" + translation);
            }
        });


        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        SimpleAdapter adapter = new SimpleAdapter(200, ViewGroup.LayoutParams.MATCH_PARENT, 15);
        adapter.setOnClickItemListener(position -> {
            Toast.makeText(RecyclerViewHorizontalActivity.this, "您点击了" + position + "个", Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(adapter);
    }
}