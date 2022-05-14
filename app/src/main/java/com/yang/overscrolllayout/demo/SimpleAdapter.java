package com.yang.overscrolllayout.demo;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.SimpleVH> {

    private int[] array;

    public SimpleAdapter() {
        array = new int[30];
        for (int i = 0; i < array.length; i++) {
            int color;
            switch (i % 6) {
                case 1:
                    color = Color.RED;
                    break;
                case 2:
                    color = Color.GRAY;
                    break;
                case 3:
                    color = Color.BLACK;
                    break;
                case 4:
                    color = Color.YELLOW;
                    break;
                case 5:
                    color = Color.GREEN;
                    break;
                default:
                    color = Color.BLUE;
                    break;
            }
            array[i] = color;
        }
    }


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
        return new SimpleVH(textView);
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleVH holder, int position) {
        holder.textView.setText(String.valueOf(position));
        holder.textView.setBackgroundColor(array[position]);
    }

    @Override
    public int getItemCount() {
        return array.length;
    }

    static class SimpleVH extends RecyclerView.ViewHolder{
        TextView textView;

        public SimpleVH(@NonNull View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }
}
