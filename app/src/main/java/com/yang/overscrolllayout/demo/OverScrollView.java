package com.yang.overscrolllayout.demo;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild3;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.ScrollingView;
import androidx.core.view.ViewCompat;

public class OverScrollView extends FrameLayout implements NestedScrollingChild3, ScrollingView {

    private static final String TAG = "OverScrollView";

    static final float MAX_SCROLL_FACTOR = 0.5f;


    public interface OnScrollChangeListener{
        void onScrollChanged(OverScrollView v, int scrollX, int scrollY,
                             int oldScrollX, int oldScrollY);
    }

    private long mLastScroll;

    private OverScroller mScroller;

    /**
     * Position of the last motion event.
     */
    private int mLastMotionY;

    /**
     * True when the layout has changed but the traversal has not come through yet.
     * Ideally the view hierarchy would keep track of this for us.
     */
    private boolean mIsLayoutDirty = true;

    /**
     * The child to give focus to in the event that a child has requested focus while the
     * layout is dirty. This prevents the scroll from being wrong if the child has not been
     * laid out before requesting focus.
     */
    private View mChildToScrollTo = null;

    /**
     * True if the user is currently dragging this ScrollView around. This is
     * not the same as 'is being flinged', which can be checked by
     * mScroller.isFinished() (flinging begins when the user lifts their finger).
     */
    private boolean mIsBeingDragged = false;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;



    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;


    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;

    /**
     * Used during scrolling to retrieve the new offset within the window.
     */
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private int mNestedYOffset;

    private int mLastScrollerY;

    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;

    private NestedScrollingChildHelper mChildHelper;

    private OnScrollChangeListener mOnScrollChangeListener;

    public OverScrollView(@NonNull Context context) {
        this(context, null);
    }

    public OverScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initScrollView(context);
    }

    private void initScrollView(Context context) {
        mScroller = new OverScroller(context);
        setFocusable(true);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

        mChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }

    //NestedScrollingChild3

    @Override
    public void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
                                     @Nullable int[] offsetInWindow, int type, @NonNull int[] consumed) {
        mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type, consumed);
    }

    //NestedScrollingChild2

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return mChildHelper.startNestedScroll(axes, type);
    }

    @Override
    public void stopNestedScroll(int type) {
        mChildHelper.stopNestedScroll(type);
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return mChildHelper.hasNestedScrollingParent(type);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
                                        @Nullable int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed,
                                           @Nullable int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }

    //NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
                                        @Nullable int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    //NestedScrollingChild end



    //ScrollView import

    @Override
    public boolean shouldDelayChildPressedState() {
        //滚动的view返回true
        return true;
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }

        final int length = getVerticalFadingEdgeLength();
        final int scrollY = getScrollY();
        if (scrollY < length) {
            return scrollY / (float) length;
        }

        return 1.0f;
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }

        View child = getChildAt(0);
        final ViewGroup.MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        final int length = getVerticalFadingEdgeLength();
        final int bottomEdge = getHeight() - getPaddingBottom();
        final int span = child.getBottom() + lp.bottomMargin - getScrollY() - bottomEdge;
        if (span < length) {
            return span / (float) length;
        }

        return 1.0f;
    }

    public int getMaxScrollAmount(){
        return (int) (MAX_SCROLL_FACTOR * getHeight());
    }




    @Override
    public int computeHorizontalScrollRange() {
        return super.computeHorizontalScrollRange();
    }

    @Override
    public int computeHorizontalScrollOffset() {
        return super.computeHorizontalScrollOffset();
    }

    @Override
    public int computeHorizontalScrollExtent() {
        return super.computeHorizontalScrollExtent();
    }

    @Override
    public int computeVerticalScrollRange() {
        int count = getChildCount();
        int parentSpace = getHeight() - getPaddingBottom() - getPaddingTop();
        if (count == 0){
            return parentSpace;
        }

        View child = getChildAt(0);
        ViewGroup.MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        int scrollRange = child.getBottom() + lp.bottomMargin;
        int scrollY = getScrollY();
        int overScrollBottom = Math.max(0, scrollRange - parentSpace);
        if (scrollY < 0){
            scrollRange -= scrollY;
        } else if (scrollY > overScrollBottom){
            scrollRange += scrollY - overScrollBottom;
        }

        return scrollRange;
    }

    @Override
    public int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    @Override
    public int computeVerticalScrollExtent() {
        return super.computeVerticalScrollExtent();
    }



    @Override
    public void addView(View child) {
        checkChildViewCount();
        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        checkChildViewCount();
        super.addView(child, index);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        checkChildViewCount();
        super.addView(child, params);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        checkChildViewCount();
        super.addView(child, index, params);
    }

    private void checkChildViewCount() throws IllegalStateException {
        if (getChildCount() > 0) {
            throw new IllegalStateException(TAG + " can host only one direct child");
        }
    }

    public void setOnScrollChangeListener(@Nullable OnScrollChangeListener l){
        mOnScrollChangeListener = l;
    }

    private boolean canScroll(){
        if (getChildCount() > 0){
            View child = getChildAt(0);
            ViewGroup.MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            int childSize = child.getHeight() + lp.topMargin + lp.bottomMargin;
            int parentSize = getHeight() - getPaddingTop() - getPaddingBottom();
            return childSize > parentSize;
        }
        return false;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        if (mOnScrollChangeListener != null) {
            mOnScrollChangeListener.onScrollChanged(this, l, t, oldl, oldt);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, getPaddingLeft() + getPaddingRight(), lp.width);
        int verticalPadding = getPaddingTop() + getPaddingBottom();
        int childHeightSize = Math.max(0, MeasureSpec.getSize(parentHeightMeasureSpec) - verticalPadding);
        //不限制高度
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeightSize, MeasureSpec.UNSPECIFIED);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        ViewGroup.MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        int horizontalPadding = getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin + widthUsed;
        int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, horizontalPadding, lp.width);
        int verticalPadding = getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin + heightUsed;
        int childHeightSize = Math.max(0, MeasureSpec.getSize(parentHeightMeasureSpec) - verticalPadding);
        //不限制高度
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeightSize, MeasureSpec.UNSPECIFIED);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        Log.i(TAG, "onInterceptTouchEvent: 1");
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
//            Log.i(TAG, "onInterceptTouchEvent: 2");
            return true;
        }

        if (super.onInterceptTouchEvent(ev)) {
//            Log.i(TAG, "onInterceptTouchEvent: 3");
            return true;
        }

        int actionMasked = ev.getActionMasked();

//        Log.i(TAG, "onInterceptTouchEvent: 4  " + actionMasked);
        switch (actionMasked){

            case MotionEvent.ACTION_DOWN:
                //pointerIndex是0
                mActivePointerId = ev.getPointerId(0);
                mLastMotionY = (int) ev.getY();

                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);

                mScroller.computeScrollOffset();
                mIsBeingDragged = !mScroller.isFinished();

                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
                break;

            case MotionEvent.ACTION_MOVE:
                final int activePointerId = this.mActivePointerId;
                if (activePointerId == INVALID_POINTER){
                    break;
                }

                int pointerIndex = ev.findPointerIndex(activePointerId);
                if (pointerIndex == -1){
                    Log.e(TAG, "Invalid pointerId=" + activePointerId + " in onInterceptTouchEvent");
                    break;
                }

                int y = (int) ev.getY(pointerIndex);
                int yDiff = Math.abs(y - mLastMotionY);
                if (yDiff > mTouchSlop && (getNestedScrollAxes() & ViewCompat.SCROLL_AXIS_VERTICAL) == 0){
                    mIsBeingDragged = true;
                    mLastMotionY = y;
                    initVelocityTrackerIfNotExists();
                    mVelocityTracker.addMovement(ev);
                    mNestedYOffset = 0;

                    final ViewParent parent = getParent();
                    if (parent != null){
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
//                Log.i(TAG, "onInterceptTouchEvent: " + mIsBeingDragged);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                onSecondPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                recycleVelocityTracker();

                if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0, getScrollRange())){
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                stopNestedScroll(ViewCompat.TYPE_TOUCH);
                break;
        }

        return mIsBeingDragged;
    }

    private void onSecondPointerUp(MotionEvent ev){
        final int pointIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        int pointerId = ev.getPointerId(pointIndex);
        if (pointerId == mActivePointerId){
            final int newPointerIndex = pointIndex == 0 ? 1 : 0;
            mLastMotionY = (int) ev.getY(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null){
                mVelocityTracker.clear();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        initVelocityTrackerIfNotExists();

        MotionEvent vtev = MotionEvent.obtain(ev);

        final int actionMasked = ev.getActionMasked();
        if (actionMasked == MotionEvent.ACTION_DOWN){
            mNestedYOffset = 0;
        }
        vtev.offsetLocation(0, mNestedYOffset);

//        Log.i(TAG, "onTouchEvent: 1 " + actionMasked);

        switch (actionMasked){

            case MotionEvent.ACTION_DOWN:
                if (getChildCount() == 0){
                    return false;
                }

                if ((mIsBeingDragged = !mScroller.isFinished())){
                    final ViewParent parent = getParent();
                    if (parent != null){
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }

                /*
                 * If being flinged and user touches, stop the fling. isFinished
                 * will be false if being flinged.
                 */
                if (!mScroller.isFinished()){
                    mScroller.abortAnimation();
                }

                mLastMotionY = (int) ev.getY();
                mActivePointerId = ev.getPointerId(0);
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                int actionIndex = ev.getActionIndex();
                mLastMotionY = (int) ev.getY(actionIndex);
                mActivePointerId = ev.getPointerId(actionIndex);
                break;

            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1){
                    break;
                }

                final int y = (int) ev.getY(activePointerIndex);
                //触摸点位移(上一个点减去下一个点)，正值：触摸点向坐标轴负方向滑动，负值：触摸点向坐标轴正方向滑动
                //这个值可以直接传给scrollBy
                int deltaY = mLastMotionY - y;
                if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop){
                    final ViewParent parent = getParent();
                    if (parent != null){
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    mIsBeingDragged = true;

                    if (deltaY > 0){
                        deltaY -= mTouchSlop;
                    } else {
                        deltaY += mTouchSlop;
                    }
//                    Log.i(TAG, "onTouchEvent: 2 " + deltaY);
                }


                if (mIsBeingDragged){
//                    Log.i(TAG, "onTouchEvent: 3");

                    //mScrollOffset，正值：向坐标轴正方向位移
                    if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset, ViewCompat.TYPE_TOUCH)){
                        //去除调嵌套机制先消费的位移
                        deltaY -= mScrollConsumed[1];
                        //嵌套滑动时，该view在Window中位移了，mNestedYOffset正值：向坐标轴正方向位移
                        mNestedYOffset += mScrollOffset[1];

//                        //嵌套滑动时，该view在Window中位移了，触摸点在该view的正确坐标要纠正，（vtev.offsetLocation）正值代表view像坐标轴正方向位移了
//                        vtev.offsetLocation(0, mScrollOffset[1]);
                    }

                    //纠正触摸点，去掉view嵌套滑动的位移
                    mLastMotionY = y - mScrollOffset[1];

                    //ScrollY，正值代表view的内容向坐标轴负方向移动的距离
                    int oldY = getScrollY();
                    int range = getScrollRange();

                    //滚动，不在嵌套滚动层级里时
//                    Log.i(TAG, "onTouchEvent: 4  " + deltaY + "  " + getScrollY() + "  " + range);
                    if (overScrollByCompat(0, deltaY, 0, getScrollY(), 0, range, 0, 0, true)
                            && !hasNestedScrollingParent()) {
                        mVelocityTracker.clear();
//                        Log.i(TAG, "onTouchEvent: 5");
                    }

                    int scrolledDeltaY = getScrollY() - oldY;
                    int unconsumedY = deltaY - scrolledDeltaY;

                    dispatchNestedScroll(0, scrolledDeltaY, 0, unconsumedY,
                            mScrollOffset, ViewCompat.TYPE_TOUCH, mScrollConsumed);

                    mLastMotionY -= mScrollOffset[1];
                    mNestedYOffset += mScrollOffset[1];

                    deltaY -= mScrollConsumed[1];

                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged){
                    VelocityTracker velocityTracker = this.mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);

                    //速度，正值：表明手指向坐标轴负方向移动
                    int initialVelocity = (int) velocityTracker.getYVelocity(mActivePointerId);

                    Log.i(TAG, "onTouchEvent: 6   " + mActivePointerId + "   " + initialVelocity);

                    if (Math.abs(initialVelocity) > mMinimumVelocity){
                        if (!dispatchNestedPreFling(0, -initialVelocity)){
                            dispatchNestedFling(0, -initialVelocity, true);
                            fling(-initialVelocity);
                        }
                    } else if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0, getScrollRange())){
                        ViewCompat.postInvalidateOnAnimation(this);
                    }
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged && getChildCount() > 0){
                    Log.i(TAG, "onTouchEvent: 7");
                    if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0, getScrollRange())){
                        postInvalidateOnAnimation();
                    }
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;


        }

        if (mVelocityTracker != null){
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();
        return true;
    }

    private void fling(int velocityY) {
        if (getChildCount() > 0){
            mScroller.fling(getScrollX(), getScrollY() //开始位置
                    , 0, velocityY,  //速度
                    0, 0, //x
                    Integer.MIN_VALUE, Integer.MAX_VALUE, //y
                    0, 0); //overscroll

            runAnimatedScroll(true);
        }
    }

    @Override
    public void computeScroll() {
//        Log.i(TAG, "computeScroll: 1");

        if (mScroller.isFinished()){
            return;
        }

        mScroller.computeScrollOffset();
        final int y = mScroller.getCurrY();
        int unconsumed = y - mLastScrollerY;
        mLastScrollerY = y;

        mScrollConsumed[0] = 0;
        dispatchNestedPreScroll(0, unconsumed, mScrollConsumed, null, ViewCompat.TYPE_NON_TOUCH);

        unconsumed -= mScrollConsumed[1];

        int scrollRange = getScrollRange();

        if (unconsumed != 0){
            final int oldScrollY = getScrollY();
            overScrollByCompat(0, unconsumed, 0, oldScrollY, 0, scrollRange,
                    0, 0, false);

            final int scrolledByMe = getScrollY() - oldScrollY;
            unconsumed -= scrolledByMe;

            mScrollConsumed[1] = 0;
            dispatchNestedScroll(0, scrolledByMe, 0, unconsumed,
                    mScrollOffset, ViewCompat.TYPE_NON_TOUCH, mScrollConsumed);

            unconsumed -= mScrollConsumed[1];
        }

        if (unconsumed != 0){

        }

        if (mScroller.isFinished()){
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
        } else {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private void runAnimatedScroll(boolean participateInNestedScrolling){
        if (participateInNestedScrolling) {
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        } else {
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
        }
        mLastScrollerY = getScrollY();
        ViewCompat.postInvalidateOnAnimation(this);
    }

    private void endDrag(){
        mIsBeingDragged = false;
        recycleVelocityTracker();
        stopNestedScroll(ViewCompat.TYPE_TOUCH);
    }

    public int getScrollRange(){
        int scrollRange = 0;
        if (getChildCount() > 0){
            View child = getChildAt(0);
            ViewGroup.MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            int childSize = child.getHeight() + lp.topMargin + lp.bottomMargin;
            int parentSize = getHeight() - getPaddingTop() - getPaddingBottom();
//            Log.i(TAG, "getScrollRange: " + height + "  " + i);
            scrollRange = Math.max(0, childSize - parentSize);
        }
        return scrollRange;
    }


    protected boolean overScrollByCompat(int deltaX, int deltaY,
                                         int scrollX, int scrollY,
                                         int scrollRangeX, int scrollRangeY,
                                         int maxOverScrollX, int maxOverScrollY,
                                         boolean isTouchEvent) {

        int overScrollMode = getOverScrollMode();
        boolean canScrollHorizontal = computeHorizontalScrollRange() > computeHorizontalScrollExtent();
        boolean canScrollVertical = computeVerticalScrollRange() > computeVerticalScrollExtent();

        final boolean overScrollHorizontal = overScrollMode == View.OVER_SCROLL_ALWAYS
                || (overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollHorizontal);
        final boolean overScrollVertical = overScrollMode == View.OVER_SCROLL_ALWAYS
                || (overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollVertical);

        int newScrollX = scrollX + deltaX;
        if (!overScrollHorizontal){
            maxOverScrollX = 0;
        }

        int newScrollY = scrollY + deltaY;
        if (!overScrollVertical){
            maxOverScrollY = 0;
        }

        int left = -maxOverScrollX;
        int right = maxOverScrollX + scrollRangeX;
        int top = -maxOverScrollY;
        int bottom = maxOverScrollY + scrollRangeY;

        boolean clampedX = false;
        if (newScrollX > right) {
            newScrollX = right;
            clampedX = true;
        } else if (newScrollX < left) {
            newScrollX = left;
            clampedX = true;
        }

        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }

        if (clampedY && !hasNestedScrollingParent(ViewCompat.TYPE_NON_TOUCH)){
            mScroller.springBack(newScrollX, newScrollY, 0, 0, 0, getScrollRange());
        }

        onOverScrolled(newScrollX, newScrollY, clampedX, clampedY);

        return clampedX || clampedY;
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        Log.i(TAG, "onOverScrolled: " + scrollY);
        super.scrollTo(scrollX, scrollY);
    }




}
