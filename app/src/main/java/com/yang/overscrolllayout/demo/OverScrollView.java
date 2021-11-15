package com.yang.overscrolllayout.demo;

import android.content.Context;
import android.os.Build;
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

public class OverScrollView extends FrameLayout {

    private static final String TAG = "======";

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

    private int mOverScrollDistance;
    private int mOverFlingDistance;

    private float mVerticalScrollFactor;

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

    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;

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
        mOverScrollDistance = configuration.getScaledOverscrollDistance();
        mOverFlingDistance = configuration.getScaledOverflingDistance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mVerticalScrollFactor = configuration.getScaledVerticalScrollFactor();
        } else {
            //TODO
            mVerticalScrollFactor = 64;
        }
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

                startNestedScroll(SCROLL_AXIS_VERTICAL);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:

                break;

            case MotionEvent.ACTION_MOVE:
                final int activePointerId = this.mActivePointerId;
                if (activePointerId == INVALID_POINTER){
                    break;
                }

                int pointerIndex = ev.findPointerIndex(activePointerId);
                if (pointerIndex == -1){
                    break;
                }

                int y = (int) ev.getY(pointerIndex);
                int yDiff = Math.abs(y - mLastMotionY);
                if (yDiff > mTouchSlop && (getNestedScrollAxes() & SCROLL_AXIS_VERTICAL) == 0){
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

//                if (mScroller.springBack())
                stopNestedScroll();
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

        int actionMasked = ev.getActionMasked();
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
                startNestedScroll(SCROLL_AXIS_VERTICAL);
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
                //触摸点位移，正值：触摸向坐标轴负方向滑动，负值：触摸向坐标轴正方向滑动
                int deltaY = mLastMotionY - y;
                //mScrollOffset，正值：向坐标轴正方向位移
                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)){
                    //去除调嵌套机制先消费的位移
                    deltaY -= mScrollConsumed[1];
                    //嵌套滑动时，该view在Window中位移了，触摸点在该view的正确坐标要纠正，（vtev.offsetLocation）正值代表view像坐标轴正方向位移了
                    vtev.offsetLocation(0, mScrollOffset[1]);
                    //嵌套滑动时，该view在Window中位移了，mNestedYOffset正值：向坐标轴正方向位移
                    mNestedYOffset += mScrollOffset[1];
                }

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

                    //纠正触摸点，去掉view嵌套滑动的位移
                    mLastMotionY = y - mScrollOffset[1];
                    //ScrollY，正值代表view的内容向坐标轴负方向移动的距离
                    int oldY = getScrollY();
                    int range = getScrollRange();
                    int height = getHeight() - getPaddingTop() - getPaddingBottom();

                    //滚动，不在嵌套滚动层级里时
//                    Log.i(TAG, "onTouchEvent: 4  " + deltaY + "  " + getScrollY() + "  " + range);
                    if (overScrollBy(0, deltaY, 0, getScrollY(), 0, range, 0, height / 2, true)
                            && !hasNestedScrollingParent()) {
                        mVelocityTracker.clear();
//                        Log.i(TAG, "onTouchEvent: 5");
                    }

                    int scrolledDeltaY = getScrollY() - oldY;
                    int unconsumedY = deltaY - scrolledDeltaY;
                    if (dispatchNestedScroll(0, scrolledDeltaY, 0, unconsumedY, mScrollOffset)){
                        mLastMotionY -= mScrollOffset[1];
                        vtev.offsetLocation(0, mScrollOffset[1]);
                        mNestedYOffset += mScrollOffset[1];
                    }
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
                        flingWithNestedDispatch(-initialVelocity);
                    } else if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0, getScrollRange())){
                        postInvalidateOnAnimation();
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

    private void flingWithNestedDispatch(int velocityY){
        int scrollY = getScrollY();
        Log.i(TAG, "flingWithNestedDispatch: 1  " + scrollY + "   " + velocityY);

        boolean canFling = (scrollY > 0 || velocityY > 0) && (scrollY < getScrollRange() || velocityY < 0);

        if (!dispatchNestedPreFling(0, velocityY)) {
            Log.i(TAG, "flingWithNestedDispatch: 2  " + canFling);
            dispatchNestedFling(0, velocityY, canFling);
            Log.i(TAG, "flingWithNestedDispatch: 3");

//            if (canFling) {
                fling(velocityY);
//            }
        }
    }

    private void fling(int velocityY) {
        if (getChildCount() > 0){
            int height = getHeight() - getPaddingTop() - getPaddingBottom();
            int bottom = getChildAt(0).getHeight();

            Log.i(TAG, "fling: " + velocityY);

            mScroller.fling(getScrollX(), getScrollY(), 0, velocityY, 0, 0, 0,
                    Math.max(0, bottom - height), 0, height / 2);

            postInvalidateOnAnimation();
        }
    }

    @Override
    public void computeScroll() {
//        Log.i(TAG, "computeScroll: 1");
        if (mScroller.computeScrollOffset()) {
            Log.i(TAG, "computeScroll: 2");

            int oldX = getScrollX();
            int oldY = getScrollY();

            int currX = mScroller.getCurrX();
            int currY = mScroller.getCurrY();

            if (oldX != currX || oldY != currY){
//                Log.i(TAG, "computeScroll: 3");
                
                int height = getHeight() - getPaddingTop() - getPaddingBottom();

                overScrollBy(currX - oldX, currY - oldY, oldX, oldY, 0, getScrollRange(),
                        0, height / 2, false);
            }

            postInvalidateOnAnimation();
        } else {
            Log.i(TAG, "computeScroll: 3");
        }
    }

    private void endDrag(){
        mIsBeingDragged = false;
        recycleVelocityTracker();
    }

    public int getScrollRange(){
        int scrollRange = 0;
        if (getChildCount() > 0){
            View child = getChildAt(0);
            int height = child.getHeight();
            int i = getHeight() - getPaddingTop() - getPaddingBottom();
//            Log.i(TAG, "getScrollRange: " + height + "  " + i);
            scrollRange = Math.max(0, height - i);
        }
        return scrollRange;
    }

    @Override
    protected int computeVerticalScrollRange() {
        int count = getChildCount();
        int contentLength = getHeight() - getPaddingBottom() - getPaddingTop();
        if (count == 0){
            return contentLength;
        }

        int scrollRange = getChildAt(0).getBottom();
        int scrollY = getScrollY();
        int overScrollBottom = Math.max(0, scrollRange - contentLength);
        if (scrollY < 0){
            scrollRange -= scrollY;
        } else if (scrollY > overScrollBottom){
            scrollRange += scrollY - overScrollBottom;
        }

        return scrollRange;
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return super.computeVerticalScrollOffset();
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        Log.i(TAG, "onOverScrolled: " + scrollY);
        if (!mScroller.isFinished()){
            scrollTo(scrollX, scrollY);
            Log.i(TAG, "onOverScrolled: 1");
        } else {
            scrollTo(scrollX, scrollY);
            Log.i(TAG, "onOverScrolled: 2  " + scrollY);
        }
    }
}
