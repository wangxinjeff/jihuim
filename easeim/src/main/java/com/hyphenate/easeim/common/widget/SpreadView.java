package com.hyphenate.easeim.common.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;


import com.hyphenate.easeim.R;
import com.hyphenate.util.DensityUtil;

import java.util.ArrayList;
import java.util.List;

public class SpreadView extends View {

    private Paint centerPaint; //中心圆paint
    private float radius = 77f; //中心圆半径
    private Paint spreadPaint; //扩散圆paint
    private float centerX;//圆心x
    private float centerY;//圆心y
    private float distance = 23; //每次圆递增间距
    private int delayMilliseconds = 350;//扩散延迟间隔，越大扩散越慢
    private List<Integer> spreadRadius = new ArrayList<>();//扩散圆层级数，元素为扩散的距离
    private List<Integer> alphas = new ArrayList<> ();//对应每层圆的透明度

    public SpreadView(Context context) {
        this(context, null);
    }

    public SpreadView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpreadView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SpreadView, defStyleAttr, 0);
        radius = a.getDimension(R.styleable.SpreadView_spread_radius, DensityUtil.dip2px(getContext(), radius));
        int centerColor = a.getColor(R.styleable.SpreadView_spread_center_color, ContextCompat.getColor(context, R.color.colorAccent));
        int spreadColor = a.getColor(R.styleable.SpreadView_spread_spread_color, ContextCompat.getColor(context, R.color.colorAccent));
        distance = a.getDimension(R.styleable.SpreadView_spread_distance, DensityUtil.dip2px(getContext(), distance));
        delayMilliseconds = a.getInt(R.styleable.SpreadView_spread_delay_milliseconds, delayMilliseconds);
        a.recycle();

        centerPaint = new Paint();
        centerPaint.setColor(centerColor);
        centerPaint.setAntiAlias(true);
        //最开始不透明且扩散距离为0
        alphas.add(255);
        spreadRadius.add(0);
        spreadPaint = new Paint();
        spreadPaint.setAntiAlias(true);
        spreadPaint.setAlpha(255);
        spreadPaint.setColor(spreadColor);

//        initData();
    }

    private void refreshData() {
        if(alphas.isEmpty()) {
            alphas.add(255);
            spreadRadius.add(0);
        } else if (alphas.size() == 1) {
            alphas.add(78);
            spreadRadius.add(DensityUtil.dip2px(getContext(), 23));
        } else if (alphas.size() == 2) {
            alphas.add(51);
            spreadRadius.add(DensityUtil.dip2px(getContext(), 47));
        } else if (alphas.size() == 3) {
            alphas.add(25);
            spreadRadius.add(DensityUtil.dip2px(getContext(), 75));
        } else {
            alphas.clear();
            spreadRadius.clear();
            alphas.add(255);
            spreadRadius.add(0);
        }
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 圆心位置
        centerX = w / 2;
        centerY = h / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < spreadRadius.size(); i++) {
            int alpha = alphas.get(i);
            spreadPaint.setAlpha(alpha);
            int width = spreadRadius.get(i);
            // 绘制扩散的圆
            canvas.drawCircle(centerX, centerY, radius + width, spreadPaint);
        }
        refreshData();
        // 中间的圆
        canvas.drawCircle(centerX, centerY, radius, centerPaint);
        // TODO 可以在中间圆绘制文字或者图片
        // 延迟更新，达到扩散视觉差效果
        postInvalidateDelayed(delayMilliseconds);
    }
}
