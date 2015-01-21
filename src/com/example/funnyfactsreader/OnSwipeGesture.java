package com.example.funnyfactsreader;

import android.view.View;

/**
 * OnSwipeGesture
 * 
 * Provides callback to register in SwipeGestureListener
 * 
 * @author eyali
 * 
 */
public interface OnSwipeGesture {

    /**
     * onSwipe - called when the passed View was swiped
     * 
     * @param swipedView
     *            - the swiped view
     */
    void onSwipe(View swipedView);

}
