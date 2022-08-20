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
    private EMState EMState;
    private EMCameraView view;
//    private CameraInterface.CameraOpenOverCallback cameraOpenOverCallback;

    private EMState previewEMState;       //浏览状态(空闲)
    private EMState borrowPictureEMState; //浏览图片
    private EMState borrowVideoEMState;   //浏览视频

    public EMCameraMachine(Context context, EMCameraView view, EMCameraInterface.CameraOpenOverCallback
            cameraOpenOverCallback) {
        this.context = context;
        previewEMState = new EMPreviewEMState(this);
        borrowPictureEMState = new EMBorrowPictureEMState(this);
        borrowVideoEMState = new EMBorrowVideoEMState(this);
        //默认设置为空闲状态
        this.EMState = previewEMState;
//        this.cameraOpenOverCallback = cameraOpenOverCallback;
        this.view = view;
    }

    public EMCameraView getView() {
        return view;
    }

    public Context getContext() {
        return context;
    }

    public void setEMState(EMState EMState) {
        this.EMState = EMState;
    }

    //获取浏览图片状态
    EMState getBorrowPictureEMState() {
        return borrowPictureEMState;
    }

    //获取浏览视频状态
    EMState getBorrowVideoEMState() {
        return borrowVideoEMState;
    }

    //获取空闲状态
    EMState getPreviewEMState() {
        return previewEMState;
    }

    @Override
    public void start(SurfaceHolder holder, float screenProp) {
        EMState.start(holder, screenProp);
    }

    @Override
    public void stop() {
        EMState.stop();
    }

    @Override
    public void focus(float x, float y, EMCameraInterface.FocusCallback callback) {
        EMState.focus(x, y, callback);
    }

    @Override
    public void switich(SurfaceHolder holder, float screenProp) {
        EMState.switich(holder, screenProp);
    }

    @Override
    public void restart() {
        EMState.restart();
    }

    @Override
    public void capture() {
        EMState.capture();
    }

    @Override
    public void record(Surface surface, float screenProp) {
        EMState.record(surface, screenProp);
    }

    @Override
    public void stopRecord(boolean isShort, long time) {
        EMState.stopRecord(isShort, time);
    }

    @Override
    public void cancel(SurfaceHolder holder, float screenProp) {
        EMState.cancel(holder, screenProp);
    }

    @Override
    public void confirm() {
        EMState.confirm();
    }


    @Override
    public void zoom(float zoom, int type) {
        EMState.zoom(zoom, type);
    }

    @Override
    public void flash(String mode) {
        EMState.flash(mode);
    }

    public EMState getEMState() {
        return this.EMState;
    }
}
