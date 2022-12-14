package com.hyphenate.mediapicker.cameralibrary.state;

import android.view.Surface;
import android.view.SurfaceHolder;

import com.hyphenate.mediapicker.cameralibrary.EMCameraInterface;

/**
 * =====================================
 * 作    者: 陈嘉桐
 * 版    本：1.1.4
 * 创建日期：2017/9/8
 * 描    述：
 * =====================================
 */
public interface EMState {

    void start(SurfaceHolder holder, float screenProp);

    void stop();

    void focus(float x, float y, EMCameraInterface.FocusCallback callback);

    void switich(SurfaceHolder holder, float screenProp);

    void restart();

    default void capture(){};

    void record(Surface surface, float screenProp);

    void stopRecord(boolean isShort, long time);

    void cancel(SurfaceHolder holder, float screenProp);

    void confirm();

    void zoom(float zoom, int type);

    void flash(String mode);
}
