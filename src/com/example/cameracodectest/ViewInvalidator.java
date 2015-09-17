package com.example.cameracodectest;

import android.view.View;

/**
 *
 * @author tberthelot
 */
public class ViewInvalidator extends Invalidator {
    
    private final View view;
    
    public ViewInvalidator(View view)
    {
        this.view = view;
    }
    
    public void post()
    {
        this.view.postInvalidate();
    }
}
