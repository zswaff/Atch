package com.auriferous.atch;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;

import com.auriferous.atch.Callbacks.VariableCallback;

public class BannerTouchView extends RelativeLayout {
    private InputMethodManager imm;

    public int titleBarHeight = 70;
    private int windowHeight;
    public boolean isHeightInitialized = false;
    private float slop;

    private ViewGroup.MarginLayoutParams layoutParams;
    public boolean allTheWayUp = false;
    public boolean partiallyUp = false;

    private float lastY = 0;
    private int activePointerId = -1;
    private boolean panned = false;


    public BannerTouchView(Context context) {
        this(context, null, 0);
    }
    public BannerTouchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public BannerTouchView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        titleBarHeight = GeneralUtils.convertDpToPixel(titleBarHeight, context);

        imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setHeight(int height) {
        windowHeight = height;
        isHeightInitialized = true;
    }

    public void setupBanner(VariableCallback<Integer> callback){
        if(partiallyUp) return;
        partiallyUp = true;
        setVisibility(View.VISIBLE);

        layoutParams = (ViewGroup.MarginLayoutParams)getLayoutParams();
        layoutParams.setMargins(0, getBottomHeight() + titleBarHeight, 0, 0);
        animate(layoutParams.topMargin, getBottomHeight(), 300, callback);
    }
    public void putAllTheWayUp(){
        animate(layoutParams.topMargin, 0, 300, null);
    }
    public void takeAllTheWayDown(){
        animate(layoutParams.topMargin, getBottomHeight(), 300, null);
    }
    public void removeBanner(VariableCallback<Integer> callback){
        partiallyUp = false;
        animate(layoutParams.topMargin, getBottomHeight() + titleBarHeight, 300, callback);
    }

    private void animate(int start, int finish, int speed, final VariableCallback<Integer> callback) {
        ValueAnimator animator = ValueAnimator.ofInt(start, finish);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int paddingAmount = (Integer) valueAnimator.getAnimatedValue();

                if (paddingAmount == getBottomHeight())
                    allTheWayUp = false;
                if (paddingAmount == getBottomHeight() + titleBarHeight && !partiallyUp)
                    setVisibility(View.GONE);
                if (paddingAmount == 0)
                    allTheWayUp = true;
                layoutParams.topMargin = paddingAmount;

                layoutParams.bottomMargin = -layoutParams.topMargin;

                setLayoutParams(layoutParams);
                invalidate();

                if (callback != null)
                    callback.done(windowHeight - paddingAmount);
            }
        });
        animator.setDuration(Math.abs((speed*Math.abs(finish-start))/getBottomHeight()));
        animator.start();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
        layoutParams = (ViewGroup.MarginLayoutParams) getLayoutParams();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                panned = false;
                lastY = event.getY();

                activePointerId = event.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = event.findPointerIndex(activePointerId);
                final float currentY = event.getY(pointerIndex);
                final float dy = currentY - lastY;

                layoutParams.topMargin += dy;
                if(layoutParams.topMargin <= 0) {
                    layoutParams.topMargin = 0;
                    allTheWayUp = true;
                }
                if(layoutParams.topMargin >= getBottomHeight()) {
                    layoutParams.topMargin = getBottomHeight();
                    allTheWayUp = false;
                }

                layoutParams.bottomMargin = - layoutParams.topMargin;

                setLayoutParams(layoutParams);
                invalidate();

                if(Math.abs(layoutParams.topMargin - getBottomHeight()) > slop)
                    panned = true;

                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                activePointerId = -1;

                if(layoutParams.topMargin == getBottomHeight() && panned) return true;
                if(layoutParams.topMargin == 0 && panned) return true;

                if(!allTheWayUp)
                    animate(layoutParams.topMargin, 0, 300, null);
                else
                    animate(layoutParams.topMargin, getBottomHeight(), 300, null);
            }
            case MotionEvent.ACTION_POINTER_UP:
            {
                final int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = event.getPointerId(pointerIndex);

                if(pointerId == activePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    lastY = event.getY(newPointerIndex);
                    activePointerId = event.getPointerId(newPointerIndex);
                }
                break;
            }
        }
        return true;
    }


    public int getBottomHeight() {
        return (windowHeight - titleBarHeight);
    }
}