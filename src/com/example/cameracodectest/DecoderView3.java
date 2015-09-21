package com.example.cameracodectest;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import java.lang.reflect.Method;

@SuppressLint("NewApi")
public class DecoderView3 extends View {

    private static final String TAG = "DecoderView";

    private final Decoder decoder;
    private final Paint paint = new Paint();
    
    //private final Object[] params;
    
    private final Bitmap bitmap;

    public DecoderView3(Context context) {
        super(context);
        this.decoder = new Decoder(new ViewInvalidator(this));
        bitmap = Bitmap.createBitmap(this.decoder.mInWidth, this.decoder.mInHeight, Config.ARGB_8888);
        //params = new Object[]{null, 0, this.decoder.mInWidth, 0, 0, this.decoder.mInWidth, this.decoder.mInHeight, false, paint};
        //this.updateMethod();
    }

    public DecoderView3(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.decoder = new Decoder(new ViewInvalidator(this));
        bitmap = Bitmap.createBitmap(this.decoder.mInWidth, this.decoder.mInHeight, Config.ARGB_8888);
        //params = new Object[]{null, 0, this.decoder.mInWidth, 0, 0, this.decoder.mInWidth, this.decoder.mInHeight, false, paint};
        //this.updateMethod();
    }

    public DecoderView3(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.decoder = new Decoder(new ViewInvalidator(this));
        bitmap = Bitmap.createBitmap(this.decoder.mInWidth, this.decoder.mInHeight, Config.ARGB_8888);
        //params = new Object[]{null, 0, this.decoder.mInWidth, 0, 0, this.decoder.mInWidth, this.decoder.mInHeight, false, paint};
        //this.updateMethod();
    }

    /*
    private Method tempMethod;
    private final String METHOD_NAME = "drawBitmap";
    
    private void updateMethod()
    {
        try {
            Method[] methodArray = Canvas.class.getMethods();
            Method method = null;
            //final String METHOD_LABEL = "Method: ";
            for (int index = 0; index < methodArray.length; index++) {
                method = methodArray[index];
                //Log.d(TAG, METHOD_LABEL + method.getName());
                if (method.getName().startsWith(METHOD_NAME)) {

                    //Log.d(TAG, "First Param: " + method.getParameterTypes()[0]);
                    if (method.getParameterTypes()[0] == byte[].class) {

                        tempMethod = method;
                        Log.d(TAG, "setting drawBitmap method");
                        //this.getVideoPosition();
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception", e);
        }
    }
    
    private void drawBitmap(Canvas canvas)
    {
        try
        {
            params[0] = this.decoder.frameBytes;
            tempMethod.invoke(canvas, params);
        }
        catch(Exception e)
        {
            Log.e(TAG, "Exception", e);
        }
    }
    */
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "onDetachedFromWindow");
        this.decoder.mRunning = false;
    }
    
    
    @Override
    public void draw(Canvas canvas) {

        try {
            //Log.d(TAG, DRAWING_FRAME);

            super.draw(canvas);

//            paint.setTextSize(40);
//            paint.setColor(Color.WHITE);
//            canvas.drawText(this.AWAITING_FRAME, 0, 0, paint);
//            paint.setColor(Color.BLACK);
//            canvas.drawText(this.AWAITING_FRAME, 0, 10, paint);

            
            bitmap.setPixels(this.decoder.frameBytes, 0, this.decoder.mInWidth, 0, 0, this.decoder.mInWidth, this.decoder.mInHeight);
            canvas.drawBitmap(bitmap, 0, 0, paint);
            //this.drawBitmap(canvas);
            //canvas.drawBitmap(this.decoder.frameBytes, 0, this.decoder.mInWidth, 0, 0, this.decoder.mInWidth, this.decoder.mInHeight, false, paint);
            //canvas.drawBitmap(this.decoder.colors, 0, this.decoder.mInWidth, 0, 0, this.decoder.mInWidth, this.decoder.mInHeight, false, paint);
            

            //Log.d(TAG, "end");
            this.decoder.doRender = false;

        } catch (Exception e) {
            Log.e(TAG, "Exception", e);
        }
    }

}
