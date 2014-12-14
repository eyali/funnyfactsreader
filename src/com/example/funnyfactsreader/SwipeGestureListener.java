package com.example.funnyfactsreader;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * SwipeGestureListener
 * 
 * Touch listener with left and right swipe gestures detection and callbacks
 * registration
 * 
 * @author eyali
 * 
 */
public class SwipeGestureListener extends SimpleOnGestureListener implements OnTouchListener {

    private static final float HORIZONTAL_VELOCITY_THRESHOLD = 10.0f;
    private View m_view = null;
    private OnSwipeGesture m_onLeftSwipe = null;
    private OnSwipeGesture m_onRightSwipe = null;
    private GestureDetectorCompat m_detector = null;

    public SwipeGestureListener(Context context, OnSwipeGesture onLeftSwipe, OnSwipeGesture onRightSwipe) {
        m_onLeftSwipe = onLeftSwipe;
        m_onRightSwipe = onRightSwipe;
        m_detector = new GestureDetectorCompat(context, this);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (Math.abs(velocityX) > Math.abs(velocityY) + HORIZONTAL_VELOCITY_THRESHOLD) {
            if (e1.getX() < e2.getX()) {
                if (m_onLeftSwipe != null) {
                    m_onLeftSwipe.onSwipe(m_view);
                }
            } else if (e1.getX() > e2.getX()) {
                if (m_onRightSwipe != null) {
                    m_onRightSwipe.onSwipe(m_view);
                }
            }
        }
        return true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        m_view = v;
        m_detector.onTouchEvent(event);
        return true;
    }

}
