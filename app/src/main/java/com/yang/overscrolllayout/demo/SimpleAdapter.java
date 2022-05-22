package com.yang.overscrolllayout.demo;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.SimpleVH> {

    @NonNull
    @Override
    public SimpleVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView textView = new TextView(parent.getContext());
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600);
        params.topMargin = 10;
        params.bottomMargin = 10;
        textView.setLayoutParams(params);
        textView.setTextSize(14f);
        textView.setGravity(Gravity.CENTER);
        textView.setTextColor(Color.WHITE);
        textView.setBackgroundColor(Color.GRAY);
        return new SimpleVH(textView);
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleVH holder, int position) {
        holder.textView.setText(String.valueOf(position));
    }

    @Override
    public int getItemCount() {
        return 8;
    }

    static class SimpleVH extends RecyclerView.ViewHolder{
        TextView textView;

        public SimpleVH(@NonNull View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }
}
