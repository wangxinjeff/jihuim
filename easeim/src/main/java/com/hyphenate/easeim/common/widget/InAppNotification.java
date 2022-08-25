package com.hyphenate.easeim.common.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntRange;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.section.chat.activity.ChatActivity;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.easeui.utils.EaseUserUtils;

/**
 * Author: cpf
 * Date: 2020/4/10
 * Email: cpf4263@gmail.com
 * <p>
 * App internal banner notifications
 */
public class InAppNotification implements Runnable {

    private FrameLayout mRootView;

    private View mView;
    private ImageView iconView;
    private TextView titleView;
    private TextView nameView;
    private TextView contentView;

//    private View.OnClickListener mListener;

    private long mDuration = 3000;

    private Interpolator mInterpolator = new FastOutSlowInInterpolator();

    private ValueAnimator mValueAnimator;

    private int mHeight;

    private Handler handler = new Handler();

    private int offset = 0;

    private int touchSlop;

    private float startX, startY, lastY, maxY;

    private boolean canClick;

    private Activity activity;
    private EMMessage message;
    private static InAppNotification mInstance;
    private int marginTop;
    private int notifyIconResId;
    private String notifyName;

//    public InAppNotification(@NonNull FrameLayout frameLayout) {
//        mRootView = frameLayout;
//        touchSlop = ViewConfiguration.get(frameLayout.getContext()).getScaledTouchSlop();
//    }
//
//    public InAppNotification(@NonNull Activity activity) {
//        this((FrameLayout) activity.getWindow().getDecorView());
//        this.activity = activity;
//        offset = getStatusBarHeight(activity);
//        initView();
//    }

    public static InAppNotification getInstance(){
        if(mInstance == null){
            mInstance = new InAppNotification();
        }
        return mInstance;
    }

    public InAppNotification init(Activity activity){
        mRootView = (FrameLayout) activity.getWindow().getDecorView();
        touchSlop = ViewConfiguration.get(mRootView.getContext()).getScaledTouchSlop();
        this.activity = activity;
        offset = getStatusBarHeight(activity);
        initView();
        return mInstance;
    }

//    public InAppNotification setContentView(@NonNull View view) {
//        mView = view;
//        setTouchEvent();
//        return this;
//    }
//
//    public InAppNotification setContentView(@LayoutRes int resId) {
//        return this.setContentView(LayoutInflater.from(mRootView.getContext()).inflate(resId, mRootView, false));
//    }

    private void initView(){
        mView = LayoutInflater.from(mRootView.getContext()).inflate(R.layout.em_in_app_notify_layout, mRootView, false);
        iconView = mView.findViewById(R.id.notify_icon);
        titleView = mView.findViewById(R.id.notify_title);
        nameView = mView.findViewById(R.id.notify_name);
        contentView = mView.findViewById(R.id.notify_content);
        if(notifyIconResId > 0){
            iconView.setImageResource(notifyIconResId);
        }
        if(TextUtils.isEmpty(notifyName)){
            nameView.setText(notifyName);
        }
        setTouchEvent();
    }

    public InAppNotification setNotifyIcon(@DrawableRes int resId){
        notifyIconResId = resId;
        return mInstance;
    }

    public InAppNotification setNotifyName(String appName){
        notifyName = appName;
        return mInstance;
    }


//    public <T extends View> T findViewById(@IdRes int id) {
//        return mView.findViewById(id);
//    }
//
//    public InAppNotification setOnClickListener(@Nullable View.OnClickListener l) {
//        mListener = l;
//        return this;
//    }

    public InAppNotification setDuration(@IntRange(from = 3000, to = 10000) long duration) {
        mDuration = duration;
        return this;
    }

//    public InAppNotification setInterpolator(@NonNull Interpolator interpolator) {
//        mInterpolator = interpolator;
//        return this;
//    }

    private int measureHeight() {
        ViewGroup.LayoutParams lp = mView.getLayoutParams();
        if (lp == null) {
            lp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) lp).topMargin += offset;
            marginTop = ((ViewGroup.MarginLayoutParams) lp).topMargin;
        }
        int widthSpec;
        if (lp.width > 0) {
            widthSpec = View.MeasureSpec.makeMeasureSpec(lp.width, View.MeasureSpec.EXACTLY);
        } else {
            widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.AT_MOST);
        }
        int heightSpec;
        if (lp.height > 0) {
            heightSpec = View.MeasureSpec.makeMeasureSpec(lp.height, View.MeasureSpec.EXACTLY);
        } else {
            heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        }
        mView.measure(widthSpec, heightSpec);
        return mView.getMeasuredHeight();
    }

    public void show(EMMessage message) {
        // 默认传进来的是群消息
        this.message = message;
        if(mView != null){
            if(message.getChatType() == EMMessage.ChatType.Chat){
                EaseUser user = EaseUserUtils.getUserInfo(message.getFrom());
                titleView.setText(String.format("[%s]", user == null ? message.getFrom() : user.getNickname()));
                contentView.setText(EaseCommonUtils.getMessageDigest(message, activity.getApplicationContext(), false));
            } else if (message.getChatType() == EMMessage.ChatType.GroupChat){
                EMGroup group = EaseIMHelper.getInstance().getGroupManager().getGroup(message.getTo());
                titleView.setText(String.format("[%s]", group != null ? group.getGroupName() : message.getTo()));
                contentView.setText(EaseCommonUtils.getMessageDigest(message, activity.getApplicationContext(), true));
            }
            if (mHeight == 0) {
                mHeight = measureHeight();
            } else {
                ViewGroup.LayoutParams lp = mView.getLayoutParams();
                if (lp == null) {
                    lp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ((ViewGroup.MarginLayoutParams) lp).topMargin = marginTop;
                }
            }
            if (mView.getParent() == null) {
                mRootView.addView(mView);
                mView.setTranslationY(getMaxTranslationY());
            }
            enterAnim(mView.getTranslationY(), 0);
            handler.postDelayed(this, mDuration);
        }
    }

    private void hide() {
        if (mValueAnimator != null) {
            mValueAnimator.cancel();
            mValueAnimator = null;
        }
        handler.removeCallbacks(this);
        if(mView != null){
            exitAnim(mView.getTranslationY(), getMaxTranslationY());
        }
    }

    private float getMaxTranslationY() {
        return -(mHeight + offset);
    }

    public void hideNotification(){
        if (mView != null && mRootView != null) {
            mRootView.removeView(mView);
        }
        hide();
        mView = null;
    }

    @Override
    public void run() {
        hide();
    }

    private void enterAnim(float start, float end) {
        mValueAnimator = ValueAnimator.ofFloat(start, end);
        mValueAnimator.setInterpolator(mInterpolator);
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mView == null || mRootView == null) {
                    mValueAnimator.cancel();
                    mValueAnimator = null;
                    return;
                }
                float value = (Float) animation.getAnimatedValue();
                mView.setTranslationY(value);
                maxY = mView.getY();
            }
        });
        mValueAnimator.start();
    }

    private void exitAnim(float start, float end) {
        mValueAnimator = ValueAnimator.ofFloat(start, end);
        mValueAnimator.setInterpolator(mInterpolator);
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mView == null || mRootView == null) {
                    mValueAnimator.cancel();
                    mValueAnimator = null;
                    return;
                }
                float value = (Float) animation.getAnimatedValue();
                mView.setTranslationY(value);
            }
        });
        mValueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (mView != null && mRootView != null) {
                    mRootView.removeView(mView);
                }
            }
        });
        mValueAnimator.start();
    }

    private void setTouchEvent() {
        mView.setOnClickListener(null);
        mView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        canClick = true;
                        startX = event.getRawX();
                        startY = event.getRawY();
                        lastY = startY;
                        handler.removeCallbacks(InAppNotification.this);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float x = event.getRawX();
                        float y = event.getRawY();
                        updateY(y);
                        lastY = y;
                        if (!isClick(x, y)) {
                            canClick = false;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
//                        if (mListener != null && canClick) {
//                            mListener.onClick(v);
                        if(canClick){
                            hideNotification();
                            ChatActivity.actionStart(activity, message.getTo(), EaseConstant.CHATTYPE_GROUP);
                        } else {
                            if (maxY - v.getY() > offset) {
                                hide();
                            }else {
                                show(message);
                            }
                        }
                        break;
                }
                return true;
            }
        });
    }


    private void updateY(float y) {
        float newY = mView.getY() + (y - lastY);
        if (newY < maxY) {
            mView.setY(newY);
        }
    }

    private boolean isClick(float endX, float endY) {
        return Math.abs(endX - startX) < touchSlop && Math.abs(endY - startY) < touchSlop;
    }

    /**
     * Gets the height of the status bar
     */
    private int getStatusBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        return resources.getDimensionPixelSize(resourceId);
    }
}