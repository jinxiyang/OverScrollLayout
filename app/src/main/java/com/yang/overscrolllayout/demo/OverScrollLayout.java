package com.yang.overscrolllayout.demo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
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

    private View mScrollView;

    private int mMinimumFlingVelocity;


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
        onNestedScrollInternal(target, dy, type, consumed);
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
        onNestedScrollInternal(target, dyUnconsumed, type, consumed);
        Log.i(TAG, "onNestedScroll 3: dyConsumed:"  + dyConsumed + "   dyUnconsumed:" + dyUnconsumed);

        if (type == ViewCompat.TYPE_NON_TOUCH && dyUnconsumed != 0 && !mScroller.isFinished()) {
            //NestedScrollingChild fling到了边界，不能再消耗滚动距离，开启过度滚动并回弹

            float currVelocity = mScroller.getCurrVelocity();
            abortAnimation();
            if (Math.abs(currVelocity) >= mMinimumFlingVelocity) {
                overScroll(target, (int) currVelocity);
            }
        }
    }

    private boolean parentFling = false;


    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        Log.i(TAG, "onNestedPreFling : velocityX:"  + velocityX + "   velocityY:" + velocityY + "   translationY:" + target.getTranslationY());

        int translationY = (int) target.getTranslationY();
        if (translationY != 0) {
            //回弹，触摸滚动和惯性滚动都停止的情况下，target不在原位置，则触发回弹
            mScroller.springBack(0, translationY, 0, 0, 0, 0);
            FlingRunnable flingRunnable = new FlingRunnable(target);
            ViewCompat.postOnAnimation(target, flingRunnable);
            return true;
        }
        return false;
    }

    private void overScroll(@NonNull View target, int velocityY) {
        //TODO  动画：过度滑动并回弹

        //view.canScrollVertically
        //传正值，view内容是否可以向坐标轴负方向移动（垂直向下为正，水平向右为正），列表的滚轴向下移动，即向下滚动
        //传负值，view内容是否可以向坐标轴正方向移动（垂直向下为正，水平向右为正），列表的滚轴向上移动，即向上滚动
        boolean canScrollDown = target.canScrollVertically(-1);
        boolean canScrollUp = target.canScrollVertically(1);
        Log.i(TAG, "overScroll: "
                + "  velocityY:" + velocityY
                + "  translationY:" + target.getTranslationY()
                + "  canScrollDown:" + canScrollDown
                + "  canScrollUp:" + canScrollUp
        );
        animSpinnerBounce(target, velocityY);
    }

    @Override
    public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
        Log.i(TAG, "onNestedFling : velocityX:"  + velocityX + "   velocityY:" + velocityY + "  consumed:" + consumed);
        if (consumed) {
            mockNestedScrollingChildFling(target, (int) velocityY);
        }
        return false;
    }

    private void mockNestedScrollingChildFling(View target, int velocityY) {
        mScroller.fling(0, 0, 0, -velocityY, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        ViewCompat.postOnAnimation(target, new MockFlingRunnable(target));
    }

    private class MockFlingRunnable implements Runnable {

        private final View view;

        public MockFlingRunnable(View view) {
            this.view = view;
        }

        @Override
        public void run() {
            if (mScroller.computeScrollOffset()) {
                int finalY = mScroller.getFinalY();
                int currY = mScroller.getCurrY();
                float currVelocity = mScroller.getCurrVelocity();

                ViewCompat.postOnAnimation(view, this);
                Log.i(TAG, "run: "
                        + "  currVelocity:" + currVelocity
                        + "  currY:" + currY
                        + "  finalY:" + finalY
                        + "  >= :" + (Math.abs(currVelocity) >= mMinimumFlingVelocity)
                );
            }
        }
    }

    public void onNestedScrollInternal(@NonNull View target, int dy, int type, @NonNull int[] consumed) {
        //view.canScrollVertically
        //传正值，view内容是否可以向坐标轴负方向移动（垂直向下为正，水平向右为正），列表的滚轴向下移动，即向下滚动
        //传负值，view内容是否可以向坐标轴正方向移动（垂直向下为正，水平向右为正），列表的滚轴向上移动，即向上滚动

        if (dy == 0 || type == ViewCompat.TYPE_NON_TOUCH) return;

        float translationY = target.getTranslationY();

        if (dy < 0) {
            // dy < 0  手指向下滑动
            if (!target.canScrollVertically(-1)) {
                //target的内容不能向下移动

                //向下过度移动target
                target.setTranslationY(translationY - dy);
            } else if (translationY < 0) {
                //translationY < 0  说明target的真实位置向上移动了，target被过度向上移动了

                //向下恢复target的位置，恢复到0时NestedScrollingParent不再消耗，让NestedScrollingChild滑动
                target.setTranslationY(Math.min(0, translationY - dy));
            }
        } else {
            // dy > 0  手指向上滑动
            if (!target.canScrollVertically(1)) {
                //target的内容不能向上移动

                //向上恢复target的位置
                target.setTranslationY(translationY - dy);
            } else if (translationY > 0) {
                //translationY > 0  说明target的真实位置向下移动了，target被过度向下移动了

                //向上恢复target的位置，恢复到0时NestedScrollingParent不再消耗，让NestedScrollingChild滑动
                target.setTranslationY(Math.max(0, translationY - dy));
            }
        }

        consumed[1] += (int) (translationY - target.getTranslationY());
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
        int translationY = (int) target.getTranslationY();
        log("onStopNestedScroll:" +
                "  type:" + type +
                "  translationY:" + translationY +
                "  nestedScrollAxes:" + nestedScrollAxes +
                "  oldNestedScrollAxes:" + oldNestedScrollAxes +
                "  parentFling:" + parentFling
        );

        //回弹，触摸滚动和惯性滚动都停止的情况下，target不在原位置，则触发回弹
        if (nestedScrollAxes == ViewGroup.SCROLL_AXIS_NONE && !parentFling && mScroller.springBack(0, translationY, 0, 0, 0, 0)) {
            FlingRunnable flingRunnable = new FlingRunnable(target);
            ViewCompat.postOnAnimation(target, flingRunnable);
        }
    }

    private class FlingRunnable implements Runnable {

        private final View view;

        public FlingRunnable(View view) {
            this.view = view;
        }

        @Override
        public void run() {
            if (mScroller.computeScrollOffset()) {
                Log.i(TAG, "run: " + mScroller.getCurrY());;
                view.setTranslationY(mScroller.getCurrY());
                ViewCompat.postOnAnimation(view, this);
            } else if (parentFling) {
                parentFling = false;
            }
        }
    }

    public void abortAnimation(){
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
    }


    private void log(String text) {
        if (DEBUG) Log.i(TAG, text);
    }


    // 阻尼滑动参数
    private float mMaxDragRate = 2.5f;
    private int mMaxDragHeight = 250;
    private int mScreenHeightPixels;

    /**
     * 计算阻尼滑动距离
     * @param originTranslation 原始应该滑动的距离
     * @return Float, 计算结果
     */
    private int computeDampedSlipDistance(int originTranslation) {
        //translationY > 0  说明target的真实位置向下移动了
        if (originTranslation >= 0) {
            /**
             final double M = mMaxDragRate < 10 ? mMaxDragHeight * mMaxDragRate : mMaxDragRate;
             final double H = Math.max(mScreenHeightPixels / 2, thisView.getHeight());
             final double x = Math.max(0, spinner * mDragRate);
             final double y = Math.min(M * (1 - Math.pow(100, -x / (H == 0 ? 1 : H))), x);// 公式 y = M(1-100^(-x/H))
             */
            float dragRate = 0.5f;
            final float M = mMaxDragRate;
            final int H = Math.max(mScreenHeightPixels / 2, getHeight());
            final float x = Math.max(0, originTranslation * dragRate);
            final int y = (int) Math.min(M * (1 - Math.pow(100, -x / (H == 0 ? 1 : H))), x);// 公式 y = M(1-100^(-x/H))
            Log.i(TAG, "computeDampedSlipDistance: " + originTranslation + "   " + M + "  " + H + "  " + x + "  " + y);
            return y;
        } else {
            /**
             final float maxDragHeight = mFooterMaxDragRate < 10 ? mFooterHeight * mFooterMaxDragRate : mFooterMaxDragRate;
             final double M = maxDragHeight - mFooterHeight;
             final double H = Math.max(mScreenHeightPixels * 4 / 3, thisView.getHeight()) - mFooterHeight;
             final double x = -Math.min(0, (spinner + mFooterHeight) * mDragRate);
             final double y = -Math.min(M * (1 - Math.pow(100, -x / (H == 0 ? 1 : H))), x);// 公式 y = M(1-100^(-x/H))
             */
            float dragRate = 0.5f;
            final float M = mMaxDragRate;
            final int H = Math.max(mScreenHeightPixels / 2, getHeight());
            final float x = -Math.min(0, originTranslation * dragRate);
            final int y = (int) -Math.min(M * (1 - Math.pow(100, -x / (H == 0 ? 1 : H))), x);// 公式 y = M(1-100^(-x/H))
            Log.i(TAG, "computeDampedSlipDistance: " + originTranslation + "   " + M + "  " + H + "  " + x  + "  " + y);
            return y;
        }
    }

    Runnable mAnimationRunnable = null;
    int mSpinner = 0;                     // 当前竖直方向上 translationY 的距离

    private void moveTranslation(int translationY) {
        Log.i(TAG, "moveTranslation: " + translationY);
        int childCount = getChildCount();
        for (int i = 0; i < childCount ; i ++) {
            View child = getChildAt(i);
            if (child instanceof RecyclerView) {
                child.setTranslationY(translationY);
            }
        }
        mSpinner = translationY;
    }

    private ValueAnimator mReboundAnimator = null;
    private ReboundInterpolator mReboundInterpolator = new ReboundInterpolator();

    /**
     * 执行回弹动画
     * @param endSpinner 目标值
     * @param startDelay 延时参数
     * @param interpolator 加速器
     * @param duration 时长
     * @return ValueAnimator or null
     */
    protected ValueAnimator animSpinner(int endSpinner, int startDelay, Interpolator interpolator, int duration) {
        Log.i(TAG, "animSpinner: " + mSpinner + "  " + endSpinner);
        if (mSpinner != endSpinner) {
            if (mReboundAnimator != null) {
                mReboundAnimator.setDuration(0);//cancel会触发End调用，可以判断0来确定是否被cancel
                mReboundAnimator.cancel();//会触发 cancel 和 end 调用
                mReboundAnimator = null;
            }
            mAnimationRunnable = null;
            mReboundAnimator = ValueAnimator.ofInt(mSpinner, endSpinner);
            mReboundAnimator.setDuration(duration);
            mReboundAnimator.setInterpolator(interpolator);
            mReboundAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (animation != null && animation.getDuration() == 0) {
                        /*
                         * 2020-3-15 修复
                         * onAnimationEnd 因为 cancel 调用是, 同样触发 onAnimationEnd 导致的各种问题
                         * 在取消之前调用 reboundAnimator.setDuration(0) 来标记动画被取消
                         */
                        return;
                    }
                    mReboundAnimator = null;
                }
            });
            mReboundAnimator.addUpdateListener(animation -> {
                int value = (int) animation.getAnimatedValue();
                moveTranslation(value);
            });
            mReboundAnimator.setStartDelay(startDelay);
            mReboundAnimator.start();
            return mReboundAnimator;
        }
        return null;
    }

    /**
     * 惯性滑动后回弹动画
     * @param velocity 速度
     */
    protected void animSpinnerBounce(View view, float velocity) {
        mAnimationRunnable = new BounceRunnable(view, velocity, 0);
    }

    class BounceRunnable implements Runnable {
        int mFrame = 0;
//        long mFrameDelay = 10;
        long mLastTime = 0;
        int mOffset = 0;

        float mVelocity;
        int mSmoothDistance;

        View view;

        public BounceRunnable(View view, float mVelocity, int mSmoothDistance) {
            this.mVelocity = mVelocity;
            this.mSmoothDistance = mSmoothDistance;
            this.view = view;

            mLastTime = AnimationUtils.currentAnimationTimeMillis();
            postDelayed();
        }

        @Override
        public void run() {
            if (mAnimationRunnable == this) {
                if (Math.abs(mSpinner) >= Math.abs(mSmoothDistance)) {
                    if (mSmoothDistance != 0) {
                        mVelocity *= Math.pow(0.45f, ++mFrame * 2);//刷新、加载时回弹滚动数度衰减
                    } else {
                        mVelocity *= Math.pow(0.85f, ++mFrame * 2);//回弹滚动数度衰减
                    }
                } else {
                    mVelocity *= Math.pow(0.95f, ++mFrame * 2);//平滑滚动数度衰减
                }
                long now = AnimationUtils.currentAnimationTimeMillis();
                float t = 1f * (now - mLastTime) / 1000;
                float velocity = mVelocity * t;

                Log.i(TAG, "BounceRunnable run: " + mVelocity);

                // 还有速度时，就加剧过度滑动
                if (Math.abs(velocity) >= 1) {
                    mLastTime = now;
                    mOffset += velocity;
                    moveTranslation(computeDampedSlipDistance(mOffset));
                    postDelayed();
                } else {
                    // 没有速度后，通过 reboundAnimator，回弹至初始位置
                    mAnimationRunnable = null;
                    if (Math.abs(mSpinner) >= Math.abs(mSmoothDistance)) {
                        int duration = 10 * Math.min(Math.max((int) ReboundInterpolator.px2dp(Math.abs(mSpinner - mSmoothDistance)), 30), 100);
                        animSpinner(mSmoothDistance, 0, mReboundInterpolator, duration);
                    }
                }
            }
        }

        public void postDelayed() {
            ViewCompat.postOnAnimation(view, this);
            Log.i(TAG, "postDelayed: ");
        }
    }
}
