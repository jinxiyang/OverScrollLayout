package com.yang.overscrolllayout.demo;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SimpleAdapter.OnClickItemListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<String> list = new ArrayList<>();
        list.add("竖向的RecyclerView Demo");
        list.add("竖向嵌套的RecyclerView Demo");
        list.add("横向的RecyclerView Demo");
        list.add("NestedScrollView Demo");

        SimpleAdapter adapter = new SimpleAdapter(list);
        recyclerView.setAdapter(adapter);
        adapter.setOnClickItemListener(this);

    }

    @Override
    public void onClickItem(int position) {
        switch (position) {
            case 0:
                navigateToRecyclerView(R.layout.activity_recycler_view_vertical_1);
                break;
            case 1:
                navigateToRecyclerView(R.layout.activity_recycler_view_vertical_2);
                break;
            case 2:
                navigateToRecyclerViewHorizontal();
                break;
            case 3:
                navigateToNestedScrollView();
                break;
        }
    }

    private void navigateToRecyclerView(int layoutId) {
        Intent intent = new Intent(this, RecyclerViewActivity.class);
        intent.putExtra("layoutId", layoutId);
        startActivity(intent);
    }

    private void navigateToRecyclerViewHorizontal() {
        Intent intent = new Intent(this, RecyclerViewHorizontalActivity.class);
        startActivity(intent);
    }

    private void navigateToNestedScrollView() {
        Intent intent = new Intent(this, NestedScrollViewActivity.class);
        startActivity(intent);
    }
}