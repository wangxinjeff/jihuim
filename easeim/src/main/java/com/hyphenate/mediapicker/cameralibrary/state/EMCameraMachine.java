package com.hyphenate.mediapicker.cameralibrary.state;

import android.content.Context;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.hyphenate.mediapicker.cameralibrary.EMCameraInterface;
import com.hyphenate.mediapicker.cameralibrary.view.EMCameraView;


/**
 * =====================================
 * 作    者: 陈嘉桐
 * 版    本：1.1.4
 * 创建日期：2017/9/8
 * 描    述：
 * =====================================
 */
public class EMCameraMachine implements EMState {


    private Context context;
    private EMState state;
    private EMCameraView view;
//    private CameraInterface.CameraOpenOverCallback cameraOpenOverCallback;

    private EMState previewState;       //浏览状态(空闲)
    private EMState borrowPictureState; //浏览图片
    private EMState borrowVideoState;   //浏览视频

    public EMCameraMachine(Context context, EMCameraView view, EMCameraInterface.CameraOpenOverCallback
            cameraOpenOverCallback) {
        this.context = context;
        previewState = new EMPreviewState(this);
        borrowPictureState = new EMBorrowPictureState(this);
        borrowVideoState = new EMBorrowVideoState(this);
        //默认设置为空闲状态
        this.state = previewState;
//        this.cameraOpenOverCallback = cameraOpenOverCallback;
        this.view = view;
    }

    public EMCameraView getView() {
        return view;
    }

    public Context getContext() {
        return context;
    }

    public void setState(EMState state) {
        this.state = state;
    }

    //获取浏览图片状态
    EMState getBorrowPictureState() {
        return borrowPictureState;
    }

    //获取浏览视频状态
    EMState getBorrowVideoState() {
        return borrowVideoState;
    }

    //获取空闲状态
    EMState getPreviewState() {
        return previewState;
    }

    @Override
    public void start(SurfaceHolder holder, float screenProp) {
        state.start(holder, screenProp);
    }

    @Override
    public void stop() {
        state.stop();
    }

    @Override
    public void focus(float x, float y, EMCameraInterface.FocusCallback callback) {
        state.focus(x, y, callback);
    }

    @Override
    public void switich(SurfaceHolder holder, float screenProp) {
        state.switich(holder, screenProp);
    }

    @Override
    public void restart() {
        state.restart();
    }

    @Override
    public void capture() {
        state.capture();
    }

    @Override
    public void record(Surface surface, float screenProp) {
        state.record(surface, screenProp);
    }

    @Override
    public void stopRecord(boolean isShort, long time) {
        state.stopRecord(isShort, time);
    }

    @Override
    public void cancel(SurfaceHolder holder, float screenProp) {
        state.cancel(holder, screenProp);
    }

    @Override
    public void confirm() {
        state.confirm();
    }


    @Override
    public void zoom(float zoom, int type) {
        state.zoom(zoom, type);
    }

    @Override
    public void flash(String mode) {
        state.flash(mode);
    }

    public EMState getState() {
        return this.state;
    }
}
