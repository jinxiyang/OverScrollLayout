package com.yang.overscrolllayout;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.OverScroller;

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

    private OverScroller mScroller;

    private int mMinimumFlingVelocity;

    private int mScreenHeightPixels;

    private View mContentView;

    //滚动view虚拟的位移距离，因为有阻尼，实际没有消耗这么多
    private int mContentVewVirtualTranslationY = 0;
    //滚动views实际的位移距离
    private int mContentVewActualTranslationY = 0;

    private MockFlingRunnable mockFlingRunnable;
    private Runnable mOverScrollRunnable = null;
    private Runnable mSpringBackRunnable = null;

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
        mScroller = new OverScroller(context);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        mScreenHeightPixels = getResources().getDisplayMetrics().heightPixels;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child instanceof RecyclerView) {
                mContentView = child;
                break;
            }
        }

        ensureContentView();
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
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

        if (type == ViewCompat.TYPE_TOUCH) {
            abortAnimation();
        }
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed) {
//        Log.i(TAG, "onNestedPreScroll 1");
        onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        onNestedScrollInternal(dy, type, consumed);
        Log.i(TAG, "onNestedPreScroll 2: dy:" + dy + "  consumed:" + consumed[1]);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
//        Log.i(TAG, "onNestedScroll 1");
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
//        Log.i(TAG, "onNestedScroll 2");
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, mNestedScrollingV2ConsumedCompat);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
        onNestedScrollInternal(dyUnconsumed, type, consumed);
        Log.i(TAG, "onNestedScroll 3: dyConsumed:"  + dyConsumed + "   dyUnconsumed:" + dyUnconsumed);

        if (type == ViewCompat.TYPE_NON_TOUCH && dyUnconsumed != 0 && !mScroller.isFinished() && mockFlingRunnable != null) {
            //NestedScrollingChild fling到了边界，不能再消耗滚动距离，开启过度滚动并回弹

            float currVelocity = mockFlingRunnable.getCurrVelocity();
            abortAnimation();
            if (Math.abs(currVelocity) >= mMinimumFlingVelocity) {
                overScroll(target, (int) currVelocity);
            }
        }
    }

    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        Log.i(TAG, "onNestedPreFling : velocityX:"  + velocityX + "   velocityY:" + velocityY + "   translationY:" + target.getTranslationY());

        int translationY = getContentVewVirtualTranslationY();
        if (translationY != 0) {
            springBack();
            return true;
        }
        return false;
    }

    @Override
    public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
        Log.i(TAG, "onNestedFling : velocityX:"  + velocityX + "   velocityY:" + velocityY + "  consumed:" + consumed);
        if (consumed) {
            mockNestedScrollingChildFling(target, -(int) velocityY);
        }
        return false;
    }

    @Override
    public void onStopNestedScroll(@NonNull View child) {
        onStopNestedScroll(child, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        int oldNestedScrollAxes = getNestedScrollAxes();
        mNestedScrollingParentHelper.onStopNestedScroll(target, type);
        int nestedScrollAxes = getNestedScrollAxes();
        int translationY = getContentVewVirtualTranslationY();
        log("onStopNestedScroll:" +
                "  type:" + type +
                "  translationY:" + translationY +
                "  nestedScrollAxes:" + nestedScrollAxes +
                "  oldNestedScrollAxes:" + oldNestedScrollAxes
        );

        //回弹，触摸滚动和惯性滚动都停止的情况下，target不在原位置，则触发回弹
        if (nestedScrollAxes == ViewGroup.SCROLL_AXIS_NONE) {
            springBack();
        }
    }

    public void onNestedScrollInternal(int dy, int type, @NonNull int[] consumed) {
        if (dy == 0 || type == ViewCompat.TYPE_NON_TOUCH) return;

        int translationY = getContentVewVirtualTranslationY();

        int newTranslationY = translationY;

        if (dy < 0) {
            // dy < 0  手指向下滑动
            if (!contentViewCanScrollDown()) {
                //target的内容不能向下移动

                //向下过度移动target
                newTranslationY = translationY - dy;
            } else if (translationY < 0) {
                //translationY < 0  说明target的真实位置向上移动了，target被过度向上移动了

                //向下恢复target的位置，恢复到0时NestedScrollingParent不再消耗，让NestedScrollingChild滑动
                newTranslationY = Math.min(0, translationY - dy);
            }
        } else {
            // dy > 0  手指向上滑动
            if (!contentViewCanScrollUp()) {
                //target的内容不能向上移动

                //向上恢复target的位置
                newTranslationY = translationY - dy;
            } else if (translationY > 0) {
                //translationY > 0  说明target的真实位置向下移动了，target被过度向下移动了

                //向上恢复target的位置，恢复到0时NestedScrollingParent不再消耗，让NestedScrollingChild滑动
                newTranslationY = Math.max(0, translationY - dy);
            }
        }

        if (translationY != newTranslationY) {
            translateContentView(newTranslationY);
            consumed[1] += (int) (translationY - newTranslationY);
        }
    }

    private int getContentVewVirtualTranslationY() {
        return mContentVewVirtualTranslationY;
    }

    private int getContentVewActualTranslationY() {
        return mContentVewActualTranslationY;
    }

    private void setContentViewTranslationY(int virtualTranslationY, int actualTranslationY) {
        ensureContentView();
        mContentVewVirtualTranslationY = virtualTranslationY;
        mContentVewActualTranslationY = actualTranslationY;
        mContentView.setTranslationY(actualTranslationY);
        Log.i(TAG, "setContentViewTranslationY: Virtual:" + virtualTranslationY + "  actual:" + actualTranslationY);
    }

    private void translateContentView(int translationY) {
        int computedTranslationY = computeDampedSlipDistance(translationY);
        setContentViewTranslationY(translationY, computedTranslationY);
    }

    private void ensureContentView() {
        if (mContentView == null) {
            throw new RuntimeException("mContentView cannot be null !");
        }
    }

    private boolean contentViewCanScrollUp(){
        //view.canScrollVertically
        //传正值，view内容是否可以向坐标轴负方向移动（垂直向下为正，水平向右为正），列表的滚轴向下移动，即向下滚动
        //传负值，view内容是否可以向坐标轴正方向移动（垂直向下为正，水平向右为正），列表的滚轴向上移动，即向上滚动
        ensureContentView();
        return mContentView.canScrollVertically(1);
    }

    private boolean contentViewCanScrollDown(){
        //view.canScrollVertically
        //传正值，view内容是否可以向坐标轴负方向移动（垂直向下为正，水平向右为正），列表的滚轴向下移动，即向下滚动
        //传负值，view内容是否可以向坐标轴正方向移动（垂直向下为正，水平向右为正），列表的滚轴向上移动，即向上滚动
        ensureContentView();
        return mContentView.canScrollVertically(-1);
    }

    private void log(String text) {
        if (DEBUG) Log.i(TAG, text);
    }

    /**
     * 计算阻尼滑动距离
     *
     * 公式 y = M(1-100^(-x/H))
     *
     * M：过度滑动的最大距离
     * H：阻尼系数，H值越大，阻尼越大
     *
     * @param translation 原始应该滑动的距离
     * @return int, 计算结果
     */
    public int computeDampedSlipDistance(int translation) {
        //translationY > 0  说明target的真实位置向下移动了

        if (translation == 0) {
            return 0;
        }

        int x = Math.abs(translation);
        int M = getOverScrollMaxDistance();
        double H = M * 8.75;

        //公式 y = M(1-100^(-x/H))
        double y = (M * (1 - Math.pow(100, -x / H)));
        return (int) (y * (translation / x));
    }

    public int reverseComputeDampedSlipDistance(int distance) {
        if (distance == 0) {
            return 0;
        }

        int y = Math.abs(distance);
        double M = getOverScrollMaxDistance();
        double H = M * 8.75;

        double x = (Math.log(1 - y / M) / Math.log(100) * (-H));
        return (int) (x * (distance / y));
    }

    public int getOverScrollMaxDistance() {
        return Math.max(mScreenHeightPixels * 2 / 3, getHeight());
    }

    public void abortAnimation(){
        Log.i(TAG, "abortAnimation: ");
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
        if (mOverScrollRunnable != null) {
            mOverScrollRunnable = null;
        }
        if (mockFlingRunnable != null) {
            mockFlingRunnable = null;
        }

        if (mSpringBackRunnable != null) {
            mSpringBackRunnable = null;
        }
    }

    private void mockNestedScrollingChildFling(View target, int velocityY) {
        Log.i(TAG, "mockNestedScrollingChildFling: velocityY:  " + velocityY);
        mScroller.fling(0, 0, 0, velocityY, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        mockFlingRunnable = new MockFlingRunnable(target, velocityY);
        ViewCompat.postOnAnimation(target, mockFlingRunnable);
    }

    private class MockFlingRunnable implements Runnable {

        private final View view;

        //速度正负纠正因子
        private final int factor;

        public MockFlingRunnable(View view, int velocity) {
            this.view = view;
            if (velocity == 0) {
                factor = 1;
            } else {
                factor = velocity / Math.abs(velocity);
            }
        }

        @Override
        public void run() {
            if (mScroller.computeScrollOffset()) {
                ViewCompat.postOnAnimation(view, this);

                //这个值没有正负，正负根据initialVelocityY修正
                float currVelocity = getCurrVelocity();
                Log.i(TAG, "MockFlingRunnable run: "
                        + "  currVelocity:" + currVelocity
                        + "  >= :" + (Math.abs(currVelocity) >= mMinimumFlingVelocity)
                );
            }
        }

        public float getCurrVelocity(){
            if (mScroller.isFinished()) {
                return 0;
            }
            return factor * mScroller.getCurrVelocity();
        }
    }

    /**
     * 惯性滑动后回弹动画
     */
    private void overScroll(@NonNull View target, int velocityY) {
        Log.i(TAG, "overScroll: "
                + "  velocityY:" + velocityY
                + "  translationY:" + target.getTranslationY()
                + "  canScrollDown:" + contentViewCanScrollDown()
                + "  canScrollUp:" + contentViewCanScrollUp()
        );
        mOverScrollRunnable = new OverScrollRunnable(target, velocityY);
        ViewCompat.postOnAnimation(target, mOverScrollRunnable);
    }

    class OverScrollRunnable implements Runnable {
        int mFrame = 0;
        long mLastTime;
        int mOffset = 0;

        float mVelocity;

        View view;

        public OverScrollRunnable(View view, float mVelocity) {
            this.mVelocity = mVelocity;
            this.view = view;
            mLastTime = AnimationUtils.currentAnimationTimeMillis();
        }

        @Override
        public void run() {
            if (mOverScrollRunnable == this) {
                mVelocity *= Math.pow(0.85f, ++mFrame * 2);//回弹滚动数度衰减
                long now = AnimationUtils.currentAnimationTimeMillis();
                float t = 1f * (now - mLastTime) / 1000;
                float velocity = mVelocity * t;

                Log.i(TAG, "OverScrollRunnable run: " + mVelocity + "  " + velocity);

                // 还有速度时，就加剧过度滑动
                if (Math.abs(velocity) >= 1) {
                    mLastTime = now;
                    mOffset += velocity;
                    translateContentView(mOffset);
                    ViewCompat.postOnAnimation(view, this);
                } else {
                    // 没有速度后，通过 reboundAnimator，回弹至初始位置
                    mOverScrollRunnable = null;
                    springBack();
                }
            }
        }
    }

    private void springBack(){
        ensureContentView();

        int translationY = getContentVewVirtualTranslationY();
        if (translationY != 0 && mScroller.springBack(0, translationY, 0, 0, 0, 0)) {
            Log.i(TAG, "springBack: translationY: " + translationY);
            mSpringBackRunnable = new SpringBackRunnable(mContentView);
            ViewCompat.postOnAnimation(mContentView, mSpringBackRunnable);
        }
    }

    private class SpringBackRunnable implements Runnable {

        private final View view;

        public SpringBackRunnable(View view) {
            this.view = view;
        }

        @Override
        public void run() {
            if (mScroller.computeScrollOffset()) {
                Log.i(TAG, "SpringBackRunnable run: " + mScroller.getCurrY());;
                translateContentView(mScroller.getCurrY());
                ViewCompat.postOnAnimation(view, this);
            }
        }
    }
}
