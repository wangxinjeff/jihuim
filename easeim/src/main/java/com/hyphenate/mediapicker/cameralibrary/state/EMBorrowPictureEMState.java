package com.hyphenate.mediapicker.cameralibrary.state;

import android.view.Surface;
import android.view.SurfaceHolder;

import com.hyphenate.mediapicker.cameralibrary.EMCameraInterface;
import com.hyphenate.mediapicker.cameralibrary.JEMCameraView;
import com.hyphenate.mediapicker.cameralibrary.util.EMLogUtil;


/**
 * =====================================
 * 作    者: 陈嘉桐
 * 版    本：1.1.4
 * 创建日期：2017/9/8
 * 描    述：
 * =====================================
 */
public class EMBorrowPictureEMState implements EMState {
    private final String TAG = "BorrowPictureState";
    private EMCameraMachine machine;

    public EMBorrowPictureEMState(EMCameraMachine machine) {
        this.machine = machine;
    }

    @Override
    public void start(SurfaceHolder holder, float screenProp) {
        EMCameraInterface.getInstance().doStartPreview(holder, screenProp);
        machine.setEMState(machine.getPreviewEMState());
    }

    @Override
    public void stop() {

    }


    @Override
    public void focus(float x, float y, EMCameraInterface.FocusCallback callback) {
    }

    @Override
    public void switich(SurfaceHolder holder, float screenProp) {

    }

    @Override
    public void restart() {

    }

    @Override
    public void capture() {

    }

    @Override
    public void record(Surface surface, float screenProp) {

    }

    @Override
    public void stopRecord(boolean isShort, long time) {
    }

    @Override
    public void cancel(SurfaceHolder holder, float screenProp) {
        EMCameraInterface.getInstance().doStartPreview(holder, screenProp);
        machine.getView().resetState(JEMCameraView.TYPE_PICTURE);
        machine.setEMState(machine.getPreviewEMState());
    }

    @Override
    public void confirm() {
        machine.getView().confirmState(JEMCameraView.TYPE_PICTURE);
        machine.setEMState(machine.getPreviewEMState());
    }

    @Override
    public void zoom(float zoom, int type) {
        EMLogUtil.i(TAG, "zoom");
    }

    @Override
    public void flash(String mode) {

    }

}