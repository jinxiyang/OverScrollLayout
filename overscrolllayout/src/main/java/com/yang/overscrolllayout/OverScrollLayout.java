package com.yang.overscrolllayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class OverScrollLayout extends FrameLayout implements NestedScrollingParent3 {

    public static boolean DEBUG = true;

    private static final String TAG = "OverScrollLayout";

    private final NestedScrollingParentHelper mNestedScrollingParentHelper;

    private final int[] mNestedScrollingV2ConsumedCompat = new int[2];

    private final OverScroller mScroller;

    private final int mMinimumFlingVelocity;

    private MockFlingRunnable mMockFlingRunnable;
    private Runnable mOverScrollRunnable = null;
    private Runnable mSpringBackRunnable = null;

    //滚动view的id
    private int mScrollViewId = View.NO_ID;

    //滚动的方向，默认垂直方向
    private Axes mAxis = Axes.VERTICAL;

    //最大过度滚动的距离，默认1，scrollview的height
    private float mOverScrollDistanceFactor = 1.0f;

    //滚动子view的包装类
    private ScrollViewWrapper mScrollView = null;

    //过度滚动监听器集合
    private List<OnOverScrollListener> onOverScrollListeners = null;

    public OverScrollLayout(@NonNull Context context) {
        this(context, null);
    }

    public OverScrollLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverScrollLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OverScrollLayout);
        mScrollViewId = a.getResourceId(R.styleable.OverScrollLayout_oslScrollView, mScrollViewId);
        mAxis = Axes.values[a.getInt(R.styleable.OverScrollLayout_oslAxis, mAxis.getArrayIndex())];
        mOverScrollDistanceFactor = a.getFloat(R.styleable.OverScrollLayout_oslOverScrollDistanceFactor, mOverScrollDistanceFactor);
        a.recycle();

        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mScroller = new OverScroller(context);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mScrollViewId != View.NO_ID) {
            View view = findViewById(mScrollViewId);
            if (view == null) {
                throw new RuntimeException("xml中app:oslScrollView配置的view找不到");
            } else if (!isScrollView(view)) {
                throw new RuntimeException("xml中app:oslScrollView配置的view 不符合滚动view");
            }
            setScrollView(view);
        } else {
            //如果xml中没有配置app:oslScrollVie，默认从子view中查找第一个可滚动的view，不找孙子辈的view
            View view = findScrollView();
            if (view != null) {
                setScrollView(view);
            }
        }
    }

    @Nullable
    public View findScrollView() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (isScrollView(child)) {
                return child;
            }
        }
        return null;
    }

    public boolean isScrollView(View view) {
        return view instanceof RecyclerView;
    }

    public void setAxes(Axes axes) {
        mAxis = axes;
        setMaxOverScrollDistance();
    }

    public void setScrollView(@NonNull View view) {
        View contentView = view;
        while (contentView.getParent() != this) {
            ViewParent parent = contentView.getParent();
            if (parent instanceof View) {
                contentView = (View) parent;
            }
        }
        mScrollView = new ScrollViewWrapper(contentView, view);
        mScrollView.setListeners(onOverScrollListeners);
        setMaxOverScrollDistance();
    }

    public void setMaxOverScrollDistance() {
        if (mScrollView != null) {
            int maxOverScrollDistance = 0;

            if (mAxis == Axes.HORIZONTAL) {
                int width = mScrollView.getContentView().getWidth();
                maxOverScrollDistance = (int) (width * mOverScrollDistanceFactor);
            } else if (mAxis == Axes.VERTICAL) {
                int height = mScrollView.getContentView().getHeight();
                maxOverScrollDistance = (int) (height * mOverScrollDistanceFactor);
            }

            log("setMaxOverScrollDistance: " + maxOverScrollDistance);
            mScrollView.setMaxOverScrollDistance(maxOverScrollDistance);
        }
    }

    public void setOverScrollDistanceFactor(float factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("factor cannot be lower than 0");
        }
        if (factor != mOverScrollDistanceFactor) {
            mOverScrollDistanceFactor = factor;
            setMaxOverScrollDistance();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        log("onLayout");
        //重新布局后，layout变化了，重新设置最大过度滚动距离
        setMaxOverScrollDistance();
    }

    public void addOnOverScrollListener(@NonNull OnOverScrollListener listener) {
        if (onOverScrollListeners == null) {
            onOverScrollListeners = new ArrayList<>();
        }
        onOverScrollListeners.add(listener);
        if (mScrollView != null) {
            mScrollView.setListeners(onOverScrollListeners);
        }
    }

    public void removeOnOverScrollListener(@NonNull OnOverScrollListener listener) {
        if (onOverScrollListeners != null) {
            onOverScrollListeners.remove(listener);
        }
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
        return mAxis.compareNestedScrollAxes(axes) & mScrollView != null && mScrollView.getScrollView() == target;
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
        onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        onNestedScrollInternal(dx, dy, type, consumed);
        log("onNestedPreScroll: dx:" + dx + "  dy:" + dy + "  consumed:" + consumed[0] + "-" + consumed[1]);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, mNestedScrollingV2ConsumedCompat);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
        onNestedScrollInternal(dxUnconsumed, dyUnconsumed, type, consumed);
        log("onNestedScroll: "
                + "dxyConsumed:" + dxConsumed + "-" + dyConsumed
                + "  dxyUnconsumed:" + dxUnconsumed + "-" + dyUnconsumed
                + "  consumed:" + consumed[0] + "-" + consumed[1]);

        if (type == ViewCompat.TYPE_NON_TOUCH && (dxUnconsumed != 0 || dyUnconsumed != 0) && !mScroller.isFinished() && mMockFlingRunnable != null) {
            //NestedScrollingChild fling到了边界，不能再消耗滚动距离，开启过度滚动并回弹

            float currVelocity = mMockFlingRunnable.getCurrVelocity();
            abortAnimation();
            if (Math.abs(currVelocity) >= mMinimumFlingVelocity) {
                overScroll(target, (int) currVelocity);
            }
        }
    }

    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        log("onNestedPreFling : velocityX:"  + velocityX + "   velocityY:" + velocityY + "   translationY:" + target.getTranslationY());

        //如果发生了过度滚动，则springBack返回true，消耗PreFling，子view不fling。
        //   没有发生过度滚动，则springBack返回false，不消耗PreFling，子view fling。
        return springBack();
    }

    @Override
    public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
        log("onNestedFling : velocityX:"  + velocityX + "   velocityY:" + velocityY + "  consumed:" + consumed);
        if (consumed) {
            if (mAxis == Axes.HORIZONTAL) {
                mockNestedScrollingChildFling(target, -(int) velocityX);
            } else {
                mockNestedScrollingChildFling(target, -(int) velocityY);
            }
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
        float translationY = target.getTranslationY();
        log("onStopNestedScroll:" +
                "  type:" + type +
                "  translationY:" + translationY +
                "  nestedScrollAxes:" + nestedScrollAxes +
                "  oldNestedScrollAxes:" + oldNestedScrollAxes
        );

        if (nestedScrollAxes == ViewGroup.SCROLL_AXIS_NONE && mScrollView != null) {
            //回弹，触摸滚动和惯性滚动都停止的情况下，target不在原位置，则触发回弹
            springBack();
        }
    }

    private void onNestedScrollInternal(int dx, int dy, int type, @NonNull int[] consumed) {
        ScrollViewWrapper wrapper = mScrollView;
        if (type == ViewCompat.TYPE_NON_TOUCH || wrapper == null) return;

        if (mAxis == Axes.HORIZONTAL && dx != 0) {
            onNestedScrollHorizontal(wrapper, dx, consumed);
        } else if (mAxis == Axes.VERTICAL && dy != 0) {
            onNestedScrollVertical(wrapper, dy, consumed);
        }
    }

    private void onNestedScrollHorizontal(ScrollViewWrapper scrollView, int dx, @NonNull int[] consumed) {
        int translationX = scrollView.getTranslationX();

        int newTranslationX = translationX;

        if (dx < 0) {
            // dx < 0  手指向右滑动
            if (!scrollView.canScrollRight()) {
                //target的内容不能向右移动

                //向右过度移动target
                newTranslationX = translationX - dx;
            } else if (translationX < 0) {
                //translationX < 0  说明target的真实位置向左移动了，target被过度向左移动了

                //向右恢复target的位置，恢复到0时NestedScrollingParent不再消耗，让NestedScrollingChild滑动
                newTranslationX = Math.min(0, translationX - dx);
            }
        } else {
            // dx > 0  手指向左滑动
            if (!scrollView.canScrollLeft()) {
                //target的内容不能向左移动

                //向左恢复target的位置
                newTranslationX = translationX - dx;
            } else if (translationX > 0) {
                //translationX > 0  说明target的真实位置向右移动了，target被过度向右移动了

                //向左恢复target的位置，恢复到0时NestedScrollingParent不再消耗，让NestedScrollingChild滑动
                newTranslationX = Math.max(0, translationX - dx);
            }
        }

        if (translationX != newTranslationX) {
            scrollView.translateX(newTranslationX);
            consumed[0] += (int) (translationX - newTranslationX);
        }
    }

    private void onNestedScrollVertical(ScrollViewWrapper scrollView, int dy, @NonNull int[] consumed) {
        int translationY = scrollView.getTranslationY();

        int newTranslationY = translationY;

        if (dy < 0) {
            // dy < 0  手指向下滑动
            if (!scrollView.canScrollDown()) {
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
            if (!scrollView.canScrollUp()) {
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
            scrollView.translateY(newTranslationY);
            consumed[1] += (int) (translationY - newTranslationY);
        }
    }

    public void abortAnimation(){
        log("abortAnimation: ");
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
        if (mOverScrollRunnable != null) {
            mOverScrollRunnable = null;
        }
        if (mMockFlingRunnable != null) {
            mMockFlingRunnable = null;
        }

        if (mSpringBackRunnable != null) {
            mSpringBackRunnable = null;
        }
    }

    private void mockNestedScrollingChildFling(View target, int velocity) {
        log("mockNestedScrollingChildFling: velocity:  " + velocity);
        //模拟滚动view的fling事件，统一fling在Y轴上，我们只是要一个模拟的滚动速度，和坐标轴没关系。
        mScroller.fling(0, 0, 0, velocity, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        mMockFlingRunnable = new MockFlingRunnable(target, velocity);
        ViewCompat.postOnAnimation(target, mMockFlingRunnable);
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
                log("MockFlingRunnable run: "
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
    private void overScroll(@NonNull View target, int velocity) {
        log("overScroll: "
                + "  velocity:" + velocity
                + "  translationX:" + target.getTranslationX()
                + "  translationY:" + target.getTranslationY()
        );
        mOverScrollRunnable = new OverScrollRunnable(target, velocity);
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

                log("OverScrollRunnable run: " + mVelocity + "  " + velocity);

                // 还有速度时，就加剧过度滑动
                if (Math.abs(velocity) >= 1) {
                    mLastTime = now;
                    mOffset += velocity;
                    if (mAxis == Axes.HORIZONTAL) {
                        mScrollView.translateX(mOffset);
                    } else if (mAxis == Axes.VERTICAL) {
                        mScrollView.translateY(mOffset);
                    }
                    ViewCompat.postOnAnimation(view, this);
                } else {
                    // 没有速度后，通过 reboundAnimator，回弹至初始位置
                    mOverScrollRunnable = null;
                    springBack();
                }
            }
        }
    }

    private boolean springBack(){
        int translationX = mScrollView.getTranslationX();
        int translationY = mScrollView.getTranslationY();
        if ((translationX != 0 || translationY != 0) && mScroller.springBack(translationX, translationY, 0, 0, 0, 0)) {
            log("springBack: translationX: " + translationX + "  translationY:" + translationY);
            mSpringBackRunnable = new SpringBackRunnable(mScrollView);
            ViewCompat.postOnAnimation(mScrollView.getContentView(), mSpringBackRunnable);
            return true;
        }
        return false;
    }

    private class SpringBackRunnable implements Runnable {

        private final ScrollViewWrapper scrollView;

        public SpringBackRunnable(ScrollViewWrapper wrapper) {
            this.scrollView = wrapper;
        }

        @Override
        public void run() {
            if (mScroller.computeScrollOffset()) {
                log("SpringBack run:" + "  currX: " + mScroller.getCurrX() + "  currY: " + mScroller.getCurrY());
                scrollView.translate(mScroller.getCurrX(), mScroller.getCurrY());
                ViewCompat.postOnAnimation(scrollView.getContentView(), this);
            }
        }
    }

    protected static void log(String text) {
        if (DEBUG) Log.i(TAG, text);
    }


    public static class ScrollViewWrapper {
        private final View mContentView;
        private final View mScrollView;

        //滚动view虚拟的位移距离，因为有阻尼，实际没有消耗这么多
        private int mVirtualTranslationX = 0;
        private int mVirtualTranslationY = 0;

        private int mMaxOverScrollDistance;

        private List<OnOverScrollListener> listeners = null;

        public ScrollViewWrapper(@NonNull View contentView, @NonNull View scrollView) {
            this.mContentView = contentView;
            this.mScrollView = scrollView;
            //不显示滚动到头的阴影
            mScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        public View getScrollView() {
            return mScrollView;
        }

        public View getContentView(){
            return mContentView;
        }

        public void setMaxOverScrollDistance(int maxOverScrollDistance) {
            this.mMaxOverScrollDistance = maxOverScrollDistance;
        }

        public void setListeners(List<OnOverScrollListener> listeners) {
            this.listeners = listeners;
        }

        public int getTranslationY() {
            return mVirtualTranslationY;
        }

        public int getTranslationX() {
            return mVirtualTranslationX;
        }

        public void translate(int translationX, int translationY) {
            translateX(translationX);
            translateY(translationY);
        }

        public void translateX(int translationX) {
            if (mVirtualTranslationX != translationX) {
                mVirtualTranslationX = translationX;
                int actualTranslationX = computeDampedSlipDistance(translationX);
                mContentView.setTranslationX(actualTranslationX);
                log("translateX: Virtual:" + translationX + "  Actual:" + actualTranslationX);
                onOverScroll(Axes.HORIZONTAL, translationX, actualTranslationX);
            }
        }

        public void translateY(int translationY) {
            if (mVirtualTranslationY != translationY) {
                mVirtualTranslationY = translationY;
                int actualTranslationY = computeDampedSlipDistance(translationY);
                mContentView.setTranslationY(actualTranslationY);
                log("translateY: Virtual:" + translationY + "  Actual:" + actualTranslationY);
                onOverScroll(Axes.VERTICAL, translationY, actualTranslationY);
            }
        }

        private void onOverScroll(Axes axes, int translation, int actualTranslation) {
            if (listeners != null) {
                for (OnOverScrollListener listener : listeners) {
                    listener.onOverScroll(axes, translation, actualTranslation);
                }
            }
        }

        public boolean canScrollUp(){
            //view.canScrollVertically
            //传正值，view内容是否可以向坐标轴负方向移动（垂直向下为正，水平向右为正）
            //传负值，view内容是否可以向坐标轴正方向移动（垂直向下为正，水平向右为正）
            return mScrollView.canScrollVertically(1);
        }

        public boolean canScrollDown(){
            //view.canScrollVertically
            //传正值，view内容是否可以向坐标轴负方向移动（垂直向下为正，水平向右为正）
            //传负值，view内容是否可以向坐标轴正方向移动（垂直向下为正，水平向右为正）
            return mScrollView.canScrollVertically(-1);
        }

        public boolean canScrollLeft(){
            //view.canScrollHorizontally
            //传正值，view内容是否可以向坐标轴负方向移动（垂直向下为正，水平向右为正）
            //传负值，view内容是否可以向坐标轴正方向移动（垂直向下为正，水平向右为正）
            return mScrollView.canScrollHorizontally(1);
        }

        public boolean canScrollRight(){
            //view.canScrollHorizontally
            //传正值，view内容是否可以向坐标轴负方向移动（垂直向下为正，水平向右为正）
            //传负值，view内容是否可以向坐标轴正方向移动（垂直向下为正，水平向右为正）
            return mScrollView.canScrollHorizontally(-1);
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
            int M = mMaxOverScrollDistance;
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
            double M = mMaxOverScrollDistance;
            double H = M * 8.75;

            double x = (Math.log(1 - y / M) / Math.log(100) * (-H));
            return (int) (x * (distance / y));
        }
    }


    /**
     * 过度滚动的坐标轴方向
     */
    public static class Axes {

        /**
         * 无方向，过度滚动的坐标轴方向
         */
        public static final Axes NONE = new Axes(0, ViewCompat.SCROLL_AXIS_NONE);

        /**
         * 水平方向，过度滚动的坐标轴方向
         */
        public static final Axes HORIZONTAL = new Axes(1, ViewCompat.SCROLL_AXIS_HORIZONTAL);

        /**
         * 垂直方向，过度滚动的坐标轴方向
         */
        public static final Axes VERTICAL = new Axes(2, ViewCompat.SCROLL_AXIS_VERTICAL);

        public static final Axes[] values = new Axes[]{
                NONE,
                HORIZONTAL,
                VERTICAL
        };

        /**
         * 对应attrs.xml中oslAxis的value值
         */
        private final int arrayIndex;

        /**
         * 对应NestedScroll体系的坐标轴，ViewCompat.SCROLL_AXIS_NONE、ViewCompat.SCROLL_AXIS_HORIZONTAL、ViewCompat.SCROLL_AXIS_VERTICAL
         */
        private final int nestedScrollAxes;

        public Axes(int arrayIndex, int nestedScrollAxes) {
            this.arrayIndex = arrayIndex;
            this.nestedScrollAxes = nestedScrollAxes;
        }

        public int getNestedScrollAxes() {
            return nestedScrollAxes;
        }

        public int getArrayIndex() {
            return arrayIndex;
        }

        public boolean compareNestedScrollAxes(int nestedScrollAxes) {
            return (this.nestedScrollAxes & nestedScrollAxes) != 0;
        }
    }

    /**
     * 过度滚动监听器
     */
    public interface OnOverScrollListener {
        /**
         * 发生过度滚动时
         * @param axes 滚动的坐标轴方向
         * @param translation 过度滚动的距离，无阻尼
         * @param actualTranslation 过度滚动的距离经过阻尼后，实际滚动的距离
         */
        void onOverScroll(Axes axes, int translation, int actualTranslation);
    }
}
