package com.yang.overscrolllayout.demo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MyScrollView extends FrameLayout {

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

    private int mOverscrollDistance;
    private int mOverflingDistance;

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

    public MyScrollView(@NonNull Context context) {
        super(context);
    }

    public MyScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MyScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MyScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mScroller = new OverScroller(context);
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
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
            return true;
        }

        if (super.onInterceptTouchEvent(ev)) {
            return true;
        }

        int actionMasked = ev.getActionMasked();

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
                int deltaY = mLastMotionY - y;
                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)){
                    deltaY -= mScrollConsumed[1];
                    vtev.offsetLocation(0, mScrollOffset[1]);
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
                }

                if (mIsBeingDragged){
                    mLastMotionY = y - mScrollOffset[1];
                    int oldY = getScrollY();
                    int range = getScrollRange();
                    int overScrollMode = getOverScrollMode();
                    boolean canOverScroll = overScrollMode == OVER_SCROLL_ALWAYS ||
                            (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);

                    if (overScrollBy(0, deltaY, 0, getScrollY(), 0, range, 0, mOverscrollDistance, true)
                            && !hasNestedScrollingParent()) {
                        mVelocityTracker.clear();
                    }

                    int scrolledDeltaY = getScrollY() - oldY;
                    int unconsumedY = deltaY - scrolledDeltaY;
                    if (dispatchNestedScroll(0, scrolledDeltaY, 0, unconsumedY, mScrollOffset)){
                        mLastMotionY -= mScrollOffset[1];
                        vtev.offsetLocation(0, mScrollOffset[1]);
                        mNestedYOffset += mScrollOffset[1];
                    } else if (canOverScroll){
                        int pulledY = oldY + deltaY;
                        if (pulledY < 0){

                        } else if (pulledY > range){

                        }
                    }
                }
                break;


        }

        return super.onTouchEvent(ev);
    }


    private int getScrollRange(){
        int scrollRange = 0;
        if (getChildCount() > 0){
            View child = getChildAt(0);
            scrollRange = Math.max(0, child.getHeight() - (getHeight() - getPaddingTop() - getPaddingBottom()));
        }
        return scrollRange;
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        if (!mScroller.isFinished()){
            int oldX = getScrollX();
            int oldY = getScrollY();
            setScrollX(scrollX);
            setScrollY(scrollY);
            onScrollChanged(scrollX, scrollY, oldX, oldY);
            if (clampedY){
                mScroller.springBack(scrollX, scrollY, 0, 0, 0, getScrollRange());
            }
        } else {
            super.scrollTo(scrollX, scrollY);
        }

        awakenScrollBars();
    }
}
