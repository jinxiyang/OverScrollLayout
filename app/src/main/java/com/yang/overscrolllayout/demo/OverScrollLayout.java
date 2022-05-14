package com.yang.overscrolllayout.demo;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

public class OverScrollLayout extends FrameLayout implements NestedScrollingParent3 {

    public static boolean DEBUG = true;

    private static final String TAG = "OverScrollLayout";

    private NestedScrollingParentHelper mNestedScrollingParentHelper;

    private final int[] mNestedScrollingV2ConsumedCompat = new int[2];

    public OverScrollLayout(@NonNull Context context) {
        this(context, null);
    }

    public OverScrollLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverScrollLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        setNestedScrollingEnabled(true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes) {
        return onStartNestedScroll(child, target, axes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
        log("onStartNestedScroll:  " + "  axes:" + axes + "  type:" + type);
        return target instanceof RecyclerView && (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes) {
        onNestedScrollAccepted(child, target, axes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {
        log("onNestedScrollAccepted:  " + "  axes:" + axes + "  type:" + type);
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes, type);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed) {
        onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        Log.i(TAG, "onNestedPreScroll: dy:" + dy);
        onNestedScrollInternal(target, dy, type, consumed);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        Log.i(TAG, "onNestedScroll 1: dyConsumed:"  + dyConsumed + "   dyUnconsumed:" + dyUnconsumed);
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, mNestedScrollingV2ConsumedCompat);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
        Log.i(TAG, "onNestedScroll 2: dyConsumed:"  + dyConsumed + "   dyUnconsumed:" + dyUnconsumed);
//        onNestedScrollInternal(target, dyUnconsumed, type, consumed);
    }

    public void onNestedScrollInternal(@NonNull View target, int dy, int type, @NonNull int[] consumed) {
        //view.canScrollVertically
        //传正值，view内容是否可以向坐标轴负方向移动（垂直向下为正，水平向右为正），列表的滚轴向下移动，即向下滚动
        //传负值，view内容是否可以向坐标轴正方向移动（垂直向下为正，水平向右为正），列表的滚轴向上移动，即向上滚动

        if (dy == 0 || type == ViewCompat.TYPE_NON_TOUCH) return;

        int consumedY = 0;

        float translationY = target.getTranslationY();


        if (dy < 0) {
            // dy < 0  手指向下滑动
            if (!target.canScrollVertically(-1)) {
                //target的内容不能向下移动

                //向下过度移动target
                target.setTranslationY(translationY - dy);
                consumedY = dy;
            } else if (translationY < 0) {
                //translationY < 0  说明target的真实位置向上移动了，target被过度向上移动了

                //向下恢复target的位置
                consumedY = (int) Math.min(0, translationY - dy);
                target.setTranslationY(consumedY);
            }
        } else {
            // dy > 0  手指向上滑动
            if (!target.canScrollVertically(1)) {
                //target的内容不能向上移动


                target.setTranslationY(translationY - dy);
                consumedY = dy;
            } else if (translationY > 0) {
                //translationY > 0  说明target的真实位置向下移动了，target被过度向下移动了

                //向上恢复target的位置
                consumedY = (int) Math.max(0, translationY - dy);
                target.setTranslationY(consumedY);
            }
        }

        consumed[1] = consumedY;
    }

    @Override
    public void onStopNestedScroll(@NonNull View child) {
        onStopNestedScroll(child, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        log("onStopNestedScroll:  " + "  type:" + type);
        mNestedScrollingParentHelper.onStopNestedScroll(target, type);

        float translationY = target.getTranslationY();
        if (translationY != 0) {
            //回正
            target.setTranslationY(0);
        }
    }


    private void log(String text) {
        if (DEBUG) Log.i(TAG, text);
    }
}
