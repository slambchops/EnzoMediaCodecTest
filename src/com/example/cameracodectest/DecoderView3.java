package com.example.cameracodectest;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

@SuppressLint("NewApi")
public class DecoderView3 extends View {

    private static final String TAG = "DecoderView";

    private final Decoder decoder;
    private final Paint paint = new Paint();

    public DecoderView3(Context context) {
        super(context);
        this.decoder = new Decoder(new ViewInvalidator(this));
    }

    public DecoderView3(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.decoder = new Decoder(new ViewInvalidator(this));
    }

    public DecoderView3(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.decoder = new Decoder(new ViewInvalidator(this));
    }

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

            if (this.decoder.frameBytes == null) {
                return;
            }

//            paint.setTextSize(40);
//            paint.setColor(Color.WHITE);
//            canvas.drawText(this.AWAITING_FRAME, 0, 0, paint);
//            paint.setColor(Color.BLACK);
//            canvas.drawText(this.AWAITING_FRAME, 0, 10, paint);

            canvas.drawBitmap(this.decoder.colors, 0, this.decoder.mInWidth, 0, 0, this.decoder.mInWidth, this.decoder.mInHeight, false, paint);

            //Log.d(TAG, "end");
            this.decoder.doRender = false;

        } catch (Exception e) {
            Log.e(TAG, "Exception", e);
        }
    }

}
