package com.example.wangpengyun.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

/**
 * Created by wangpengyun on 2017/10/15.
 */

public class RulerView extends View {

    private static final String TAG = "RulerView";
    private VelocityTracker mVelocityTracker;
    private ViewConfiguration mViewConfiguration;

    //文字大小
    private int weightTextSize = 110;
    private int kgTextSize = 70;
    private int scaleTextSize = 80;

    //游标（矩形）的宽高
    private int cursorWidth = 16;
    private int cursorHeight = 140;

    //尺横线的宽度（粗细）
    private int horizontalLineWidth = 6;
    //长刻度的宽度
    private int rulerThickLineWidth = 12;
    //短刻度的宽度
    private int rulerThinLineWidth = 8;
    //长刻度（刻度线）的长度
    private int rulerThickLineHeight = 140;
    //短刻度的长度
    private int rulerThinLineHeight = 80;

    //刻度间的距离
    private int deltaScale = 45;
    //初始移动距离
    private int defaultScroll = deltaScale * 100;
    //尺横线的总长度
    private int maxLength = deltaScale * 1600;

    //文字到尺边缘的距离
    private int padding = 80;

    //初始体重值
    private String weight = "50.0";

    private Bitmap stillBitmap;
    private Canvas stillCanvas;

    private int lastX;
    private int deltaX;
    private int totalX;
    private int scrollX;

    private boolean afterFling = false;

    private Scroller mScroller;

    //拿到的值
    private Paint weightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    //单位"kg"
    private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    //尺的游标
    private Paint cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    //移动的尺
    private Paint rulerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    //显示刻度的文字
    private Paint scalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RulerView(Context context) {
        super(context);
        init(context);
    }

    public RulerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RulerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mViewConfiguration = ViewConfiguration.get(context);
        mScroller = new Scroller(context);
        scrollTo(defaultScroll, 0);
        totalX = -defaultScroll;
        invalidate();
    }

    {
        weightPaint.setColor(Color.parseColor("#4CAF50"));
        weightPaint.setTextSize(weightTextSize);

        textPaint.setColor(Color.parseColor("#4CAF50"));
        textPaint.setTextSize(kgTextSize);

        cursorPaint.setColor(Color.parseColor("#4CAF50"));
        cursorPaint.setStrokeWidth(cursorWidth);

        rulerPaint.setColor(Color.parseColor("#BDBDBD"));
        rulerPaint.setStrokeWidth(horizontalLineWidth);

        scalePaint.setColor(Color.parseColor("#212121"));
        scalePaint.setTextSize(scaleTextSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int textLength = (int) weightPaint.measureText(weight);
        int scaleTextY = rulerThickLineHeight + padding / 2 + scaleTextSize;

        initStillCanvas();

        canvas.save();
        canvas.translate(getMeasuredWidth() / 2, getMeasuredHeight() / 2 - 60);
        //绘制刻度线及刻度值
        canvas.drawLine(0, 0, deltaScale * 1600, 0, rulerPaint);
        for (int i = 0; i < 1601; i++) {
            int rulerLineHeight;
            if (i % 10 == 0) {
                rulerPaint.setStrokeWidth(rulerThickLineWidth);
                rulerLineHeight = rulerThickLineHeight;
                int minWeight = 40;
                String weight = String.valueOf(minWeight + i / 10);
                canvas.drawText(weight, i * deltaScale - scalePaint.measureText(weight) / 2,
                        scaleTextY, scalePaint);
            } else {
                rulerPaint.setStrokeWidth(rulerThinLineWidth);
                rulerLineHeight = rulerThinLineHeight;
            }
            canvas.drawLine(deltaScale * i, -3, deltaScale * i, rulerLineHeight, rulerPaint);

        }
        rulerPaint.setStrokeWidth(horizontalLineWidth);
        canvas.restore();

        //绘制直尺的横线及尺上方的文字
        setText();
        stillCanvas.translate(getMeasuredWidth() / 2, getMeasuredHeight() / 2 - 60);
        stillCanvas.drawText(weight, -textLength / 2, -padding, weightPaint);
        stillCanvas.drawText("kg", textLength / 2 + padding / 4, -130, textPaint);
        stillCanvas.drawLine(0, -horizontalLineWidth / 2, 0, cursorHeight, cursorPaint);
        stillCanvas.drawCircle(0, cursorHeight, cursorWidth / 2, cursorPaint);

        canvas.drawBitmap(stillBitmap, -totalX, 0, null);
    }

    private void initStillCanvas() {
        stillBitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        stillCanvas = new Canvas(stillBitmap);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        int x = (int) event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                scrollX = getScrollX();
                Log.i(TAG, "onTouchEvent: " + scrollX);

                deltaX = x - lastX;
                //一次滑动（手指抬起一次）的位移
                totalX += deltaX;
                scrollBy(-deltaX, 0);
                setText();
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                mVelocityTracker.computeCurrentVelocity(1000);
                int xVelocity = (int) mVelocityTracker.getXVelocity();
                if (Math.abs(xVelocity) > mViewConfiguration.getScaledMinimumFlingVelocity()) {
                    int duration;
                    int flingX = deltaScale * 40;
                    int toLimit = deltaScale * 5;
                    int smallFlingX = deltaScale * 6;
                    int offsetX;
                    if ((scrollX <= toLimit && deltaX > 0)
                            || (scrollX >= maxLength - toLimit && deltaX < 0)) {
                        offsetX = deltaX > 0 ? -smallFlingX : smallFlingX;
                        duration = 300;
                    } else {
                        offsetX = deltaX > 0 ? -flingX : flingX;
                        duration = 1000;
                    }

                    mScroller.startScroll(getScrollX(), 0, offsetX, 0, duration);
                    invalidate();
                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mScroller.isFinished()) {
//                                afterFling = true;
                                makeGood();
                            }
                        }
                    }, duration + 100);
                } else {
                    makeGood();
                }

                break;
        }
        lastX = x;
        return true;
    }

    private void setText() {
        int scrollX = getScrollX();
        int i = scrollX % deltaScale;
        int j = scrollX / deltaScale;
        int add = (i >= deltaScale / 2 ? 1 : 0);
        int beforePoint = 40 + j / 10;
        int afterPoint = j % 10 + add;
        if (afterPoint == 10) {
            beforePoint += 1;
            afterPoint = 0;
        }
        String weight = beforePoint + "." + afterPoint;
        if (getScrollX() <= 0) {
            weight = "40.0";
        }
        if (getScrollX() >= maxLength) {
            weight = "200.0";
        }
        setWeight(weight);
    }

    //移动刻度尺，保持刻度和游标在一条线上（把靠近游标的刻度线移到游标处）
    private void makeGood() {
        int duration;
        int offsetX;
        if (getScrollX() < 0) {
            offsetX = -getScrollX();
            duration = 800;
        } else if (getScrollX() > maxLength) {
            offsetX = -(getScrollX() - maxLength);
            duration = 800;
        } else {
            int a = getScrollX() % deltaScale;

            /*
            * 原计划抛动后，移动尺的方向和抛动方向一致，像因为惯性移动一样。
            * 能实现但是效果不是很理想，先挖个坑
            * 后来决定在抛动结束后停顿100ms后来调整尺的刻度
            * */
//            if (afterFling){
//                if (deltaX>0){
//                    offsetX=-a;
//                }else {
//                    offsetX=deltaScale-a;
//                }
//                duration=1500;
//            }else {
            offsetX = a >= deltaScale / 2 ? deltaScale - a : -a;
            duration = 300;
//            }
        }
        mScroller.startScroll(getScrollX(), 0, offsetX, 0, duration);
        invalidate();
        afterFling = false;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            totalX = -getScrollX();
            invalidate();
        }
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public void setWeightTextSize(int weightTextSize) {
        this.weightTextSize = weightTextSize;
        invalidate();
    }

    public void setKgTextSize(int kgTextSize) {
        this.kgTextSize = kgTextSize;
        invalidate();
    }

    public void setScaleTextSize(int scaleTextSize) {
        this.scaleTextSize = scaleTextSize;
        invalidate();
    }

    public void setCursorWidth(int cursorWidth) {
        this.cursorWidth = cursorWidth;
        invalidate();
    }

    public void setCursorHeight(int cursorHeight) {
        this.cursorHeight = cursorHeight;
        invalidate();
    }

    public void setHorizontalLineWidth(int horizontalLineWidth) {
        this.horizontalLineWidth = horizontalLineWidth;
        invalidate();
    }

    public void setRulerThickLineWidth(int rulerThickLineWidth) {
        this.rulerThickLineWidth = rulerThickLineWidth;
        invalidate();
    }

    public void setRulerThinLineWidth(int rulerThinLineWidth) {
        this.rulerThinLineWidth = rulerThinLineWidth;
        invalidate();
    }

    public void setRulerThickLineHeight(int rulerThickLineHeight) {
        this.rulerThickLineHeight = rulerThickLineHeight;
        invalidate();
    }

    public void setRulerThinLineHeight(int rulerThinLineHeight) {
        this.rulerThinLineHeight = rulerThinLineHeight;
        invalidate();
    }

    public void setDeltaScale(int deltaScale) {
        this.deltaScale = deltaScale;
        invalidate();
    }

    public void setPadding(int padding) {
        this.padding = padding;
        invalidate();
    }

}
