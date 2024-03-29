package com.example.android.bluetoothlegatt;



import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import static java.lang.Math.abs;
import static java.lang.Math.min;

public class JoystickView extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener {
    private float centerX;
    private float centerY;
    private float baseRadius;
    private JoystickListener joystickCallback;
    private float hatRadius;

    private void setupDimensions() {
        centerX = getWidth() / 2;
        centerY = getHeight() / 2;
        baseRadius = min(getWidth(), getHeight()) / 3;
        hatRadius = min(getWidth(), getHeight()) / 5;
    }

    //Draws joystick on screen.
    private void drawJoystick(float x, float y) {
        if (getHolder().getSurface().isValid()) {
            Canvas newCanvas = this.getHolder().lockCanvas();
            Paint colours = new Paint();
            newCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            colours.setARGB(255, 50, 50, 50);
            newCanvas.drawCircle(centerX, centerY, baseRadius, colours);

            colours.setARGB(255, 43, 116, 113);
            newCanvas.drawCircle(x, y, hatRadius, colours);

            getHolder().unlockCanvasAndPost(newCanvas);
        }
    }

    public JoystickView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setOnTouchListener(this);
        if (context instanceof JoystickListener) {
            joystickCallback = (JoystickListener) context;
        }
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setOnTouchListener(this);
        if (context instanceof JoystickListener) {
            joystickCallback = (JoystickListener) context;
        }
    }

    public JoystickView(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
        getHolder().addCallback(this);
        setOnTouchListener(this);
        if (context instanceof JoystickListener) {
            joystickCallback = (JoystickListener) context;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        setupDimensions();
        drawJoystick(centerX, centerY);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    //Called everytime the screen is touched. Uses simple trig to control joystick movement.
    public boolean onTouch(View v, MotionEvent e) {
        float scalar = 3.58f;
        float x;
        float y;
        if (v.equals(this)) {
            if (e.getAction() != e.ACTION_UP) {
                float displacement = (float) Math.sqrt(Math.pow(e.getX() - centerX, 2) + Math.pow(e.getY() - centerY, 2));
                if (displacement < baseRadius) {
                    drawJoystick(e.getX(), e.getY());
                    x = min(100, abs((e.getX() - centerX) / scalar));
                    if((e.getX() - centerX) / scalar < 0){
                        x = -x;
                    }
                    y = min(100, abs((centerY - e.getY()) / scalar));
                    if((centerY - e.getY()) / scalar < 0){
                        y = -y;
                    }
                    joystickCallback.onJoystickMoved(x, y);
                } else {
                    float ratio = baseRadius / displacement;
                    float constrainedX = centerX + (e.getX() - centerX) * ratio;
                    float constrainedY = centerY + (e.getY() - centerY) * ratio;
                    drawJoystick(constrainedX, constrainedY);
                    x = min(100, abs((constrainedX - centerX) / scalar));
                    if((constrainedX - centerX) / scalar < 0){
                        x = -x;
                    }
                    y = min(100, abs((centerY - constrainedY) / scalar));
                    if((centerY - constrainedY) / scalar < 0){
                        y = -y;
                    }
                    joystickCallback.onJoystickMoved(x, y);
                }
            } else {
                drawJoystick(centerX, centerY);
                joystickCallback.onJoystickMoved(0, 0);
            }
        }
        return true;
    }

    //callback function to be overwritten in actual implementation (currently in DeviceControlActivity)
    public interface JoystickListener {
        void onJoystickMoved(float x, float y);
    }

}
