package com.example.bt_firsttry.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class CustomView extends View {

    private Paint lPaint;
    private Rect mRect;
    private int XCross;
    private int YCross;
    final private int XStart = 950;
    final private int YStart = 500;

    public CustomView(Context context) {
        super(context);
        init(null);
    }

    public CustomView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init(attrs);
    }

    public CustomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(attrs);
    }

    public CustomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init(attrs);
    }

    public void adjustCross(int XDif, int YDif){

        XCross = XStart + (XDif - 50) * 7;
        YCross = YStart + (YDif - 50)* 7;

        postInvalidate();
    }

    private void init(@Nullable AttributeSet set){

        lPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRect = new Rect();
        XCross = XStart;
        YCross = YStart;

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        lPaint.setColor(Color.RED);
        lPaint.setStrokeWidth(5);
        canvas.drawLine(XCross - 50, YCross, XCross + 50, YCross,lPaint);
        canvas.drawLine(XCross, YCross - 50, XCross, YCross + 50,lPaint);
    }

}
