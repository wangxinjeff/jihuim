package com.hyphenate.mediapicker.cameralibrary.state;

import android.graphics.Bitmap;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.daasuu.mp4compose.FillMode;
import com.daasuu.mp4compose.composer.Mp4Composer;
import com.hyphenate.mediapicker.cameralibrary.EMCameraInterface;
import com.hyphenate.mediapicker.cameralibrary.JEMCameraView;
import com.hyphenate.mediapicker.cameralibrary.util.EMFileUtil;
import com.hyphenate.mediapicker.cameralibrary.util.EMLogUtil;

/**
 * =====================================
 * 作    者: 陈嘉桐
 * 版    本：1.1.4
 * 创建日期：2017/9/8
 * 描    述：空闲状态
 * =====================================
 */
class EMPreviewEMState implements EMState {
    public static final String TAG = "PreviewState";

    private EMCameraMachine machine;

    EMPreviewEMState(EMCameraMachine machine) {
        this.machine = machine;
    }

    @Override
    public void start(SurfaceHolder holder, float screenProp) {
        EMCameraInterface.getInstance().doStartPreview(holder, screenProp);
    }

    @Override
    public void stop() {
        EMCameraInterface.getInstance().doStopPreview();
    }


    @Override
    public void focus(float x, float y, EMCameraInterface.FocusCallback callback) {
        EMLogUtil.i("preview state focus");
        if (machine.getView().handlerFocus(x, y)) {
            EMCameraInterface.getInstance().handleFocus(machine.getContext(), x, y, callback);
        }
    }

    @Override
    public void switich(SurfaceHolder holder, float screenProp) {
        EMCameraInterface.getInstance().switchCamera(holder, screenProp);
    }

    @Override
    public void restart() {

    }

    @Override
    public void capture() {
        EMCameraInterface.getInstance().takePicture((bitmap, isVertical) -> {
            machine.getView().showPicture(bitmap, isVertical);
            machine.setEMState(machine.getBorrowPictureEMState());
            EMLogUtil.i("capture");
        });
    }

    @Override
    public void record(Surface surface, float screenProp) {
        EMCameraInterface.getInstance().startRecord(surface, screenProp, null);
    }

    @Override
    public void stopRecord(final boolean isShort, long time) {
        EMCameraInterface.getInstance().stopRecord(isShort, (url, firstFrame) -> {
            if (isShort) {
                machine.getView().resetState(JEMCameraView.TYPE_SHORT);
            }
            else if (EMCameraInterface.getInstance().isMirror()  ) {
                flipHorizontalVideo(url, firstFrame);
            } else {
                machine.getView().playVideo(firstFrame, url);
                machine.setEMState(machine.getBorrowVideoEMState());
            }
        });
    }


    @Override
    public void cancel(SurfaceHolder holder, float screenProp) {
        EMLogUtil.i("浏览状态下,没有 cancel 事件");
    }

    @Override
    public void confirm() {
        EMLogUtil.i("浏览状态下,没有 confirm 事件");
    }

    @Override
    public void zoom(float zoom, int type) {
        EMLogUtil.i(TAG, "zoom");
        EMCameraInterface.getInstance().setZoom(zoom, type);
    }

    @Override
    public void flash(String mode) {
        EMCameraInterface.getInstance().setFlashMode(mode);
    }


    private void flipHorizontalVideo(String url, Bitmap firstFrame) {
        String convert_url = url.replace("video", "covert_video");
        new Mp4Composer(url, convert_url)
                .flipHorizontal(true)
                .fillMode(FillMode.PRESERVE_ASPECT_FIT)
                .listener(new Mp4Composer.Listener() {
                    @Override
                    public void onProgress(double progress) {
                    }

                    @Override
                    public void onCompleted() {
                        EMFileUtil.deleteFile(url);
                        machine.getView().playVideo(firstFrame, convert_url);
                        machine.setEMState(machine.getBorrowVideoEMState());
                    }

                    @Override
                    public void onCanceled() {
                    }

                    @Override
                    public void onFailed(Exception exception) {
                    }
                })
                .start();
    }
}
