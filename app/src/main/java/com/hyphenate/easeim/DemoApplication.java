package com.hyphenate.easeim;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.support.v7.app.AppCompatDelegate;

import com.hyphenate.easeim.common.interfaceOrImplement.UserActivityLifecycleCallbacks;
import com.hyphenate.easeim.common.widget.InAppNotification;
import com.hyphenate.util.EMLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class DemoApplication extends Application implements Thread.UncaughtExceptionHandler {
    private static DemoApplication instance;
    private UserActivityLifecycleCallbacks mLifecycleCallbacks = new UserActivityLifecycleCallbacks();

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initThrowableHandler();
        initHx();
        registerActivityLifecycleCallbacks();
        closeAndroidPDialog();
    }

    private void initThrowableHandler() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    private void initHx() {
        EaseIMHelper.getInstance().init(this);
        InAppNotification.getInstance().setNotifyName("极狐App")
                .setNotifyIcon(R.drawable.ease_chatfrom_voice_playing_f3);
        // init hx sdk
        if(EaseIMHelper.getInstance().getAutoLogin()) {
            EMLog.i("DemoApplication", "application initHx");
            EaseIMHelper.getInstance().initChat(EaseIMHelper.getInstance().getModel().getAppMode());
        }

    }

    private void registerActivityLifecycleCallbacks() {
        this.registerActivityLifecycleCallbacks(mLifecycleCallbacks);
    }

    public static DemoApplication getInstance() {
        return instance;
    }

    public UserActivityLifecycleCallbacks getLifecycleCallbacks() {
        return mLifecycleCallbacks;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    /**
     * 为了兼容5.0以下使用vector图标
     */
    static {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        }
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        EMLog.e("demoApp", e.getMessage());

    }

    /**
     * 解决androidP 第一次打开程序出现莫名弹窗
     * 弹窗内容“detected problems with api ”
     */
    private void closeAndroidPDialog(){
        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            try {
                Class aClass = Class.forName("android.content.pm.PackageParser$Package");
                Constructor declaredConstructor = aClass.getDeclaredConstructor(String.class);
                declaredConstructor.setAccessible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Class cls = Class.forName("android.app.ActivityThread");
                Method declaredMethod = cls.getDeclaredMethod("currentActivityThread");
                declaredMethod.setAccessible(true);
                Object activityThread = declaredMethod.invoke(null);
                Field mHiddenApiWarningShown = cls.getDeclaredField("mHiddenApiWarningShown");
                mHiddenApiWarningShown.setAccessible(true);
                mHiddenApiWarningShown.setBoolean(activityThread, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
