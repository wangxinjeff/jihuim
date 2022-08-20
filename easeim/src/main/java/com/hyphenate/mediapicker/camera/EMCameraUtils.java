package com.hyphenate.mediapicker.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.hyphenate.mediapicker.cameralibrary.JEMCameraView;
import com.hyphenate.mediapicker.config.EMConstant;
import com.hyphenate.mediapicker.config.EMMediaPickerConfig;
import com.tbruyelle.rxpermissions2.RxPermissions;

/**
 * 拍照相机工具类
 *
 * @author : BaoZhou
 * @date : 2018/7/12 21:48
 */
public class EMCameraUtils {
    //权限申请自定义码
    private static int buttonState = JEMCameraView.BUTTON_STATE_BOTH;
    private static int mDuration;
    private static boolean mIsMirror;

    public static void startCamera(final Fragment fragment, final EMMediaPickerConfig config) {
        buttonState = config.getCameraMediaType();
        mDuration = config.getMaxVideoLength();
        mIsMirror = config.isMirror();
        startCameraActivity(fragment);
    }

    public static void startCamera(final FragmentActivity fragmentActivity, final EMMediaPickerConfig config) {
        buttonState = config.getCameraMediaType();
        mDuration = config.getMaxVideoLength();
        mIsMirror = config.isMirror();
        startCameraActivity(fragmentActivity);
    }

    private static void startCameraActivity(final FragmentActivity fragmentActivity) {
        RxPermissions rxPermissions = new RxPermissions(fragmentActivity);
        rxPermissions.request(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE).subscribe(granted -> {
                    if(granted){
                        startActivity(fragmentActivity);
                    }else{
                        Toast.makeText(fragmentActivity, "请确认开启录音，相机，读写存储权限", Toast.LENGTH_SHORT).show();
                    }
        });
    }


    private static void startCameraActivity(final Fragment fragment) {
        RxPermissions rxPermissions = new RxPermissions(fragment);
        rxPermissions.request(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE).subscribe(granted -> {
                    if(granted){
                        startActivity(fragment);
                    }else {
                        Toast.makeText(fragment.getActivity(), "请确认开启录音，相机，读写存储权限", Toast.LENGTH_SHORT).show();
                    }
        });
    }


    private static void startActivity(Activity activity) {
        Intent intent = new Intent();
        intent.setClass(activity, EMCameraActivity.class);
        intent.putExtra(EMConstant.BUTTON_STATE, buttonState);
        intent.putExtra(EMConstant.DURATION, mDuration);
        intent.putExtra(EMConstant.IS_MIRROR, mIsMirror);
        activity.startActivityForResult(intent, EMConstant.CAMERA_RESULT_CODE);
    }

    private static void startActivity(Fragment fragment) {
        Intent intent = new Intent();
        intent.setClass(fragment.getActivity(), EMCameraActivity.class);
        intent.putExtra(EMConstant.BUTTON_STATE, buttonState);
        intent.putExtra(EMConstant.DURATION, mDuration);
        intent.putExtra(EMConstant.IS_MIRROR, mIsMirror);
        fragment.startActivityForResult(intent, EMConstant.CAMERA_RESULT_CODE);
    }


}
