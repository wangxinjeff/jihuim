package com.hyphenate.mediapicker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.hyphenate.mediapicker.camera.EMCameraDialogFragment;
import com.hyphenate.mediapicker.camera.EMCameraUtils;
import com.hyphenate.mediapicker.cameralibrary.util.EMMediaPathUtil;
import com.hyphenate.mediapicker.config.EMConstant;
import com.hyphenate.mediapicker.config.EMMediaPickerConfig;
import com.hyphenate.mediapicker.config.EMMediaPickerEnum;
import com.hyphenate.mediapicker.photopicker.EMPhotoPickUtils;
import com.hyphenate.mediapicker.engine.EMImageEngine;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;


/**
 * SmartMediaPicker 生成类
 *
 * @author : BaoZhou
 * @date : 2019/3/14 21:38
 */
public class EMSmartMediaPicker {
    private FragmentManager manager;
    private EMCameraDialogFragment EMCameraDialogFragment;
    private WeakReference<FragmentActivity> fragmentActivity;
    private WeakReference<Fragment> fragment;
    private EMMediaPickerConfig config;

    private EMSmartMediaPicker(Context context) {
        if (EMCameraDialogFragment == null) {
            EMCameraDialogFragment = new EMCameraDialogFragment();
            EMMediaPathUtil.getInstance().initDirs("media_picker", "files", context);
        }
    }

    public void show() {
        //只启动照片选择
        if (config.getEMMediaPickerEnum() == EMMediaPickerEnum.PHOTO_PICKER) {
            if (fragmentActivity != null) {
                EMPhotoPickUtils.getAllSelector(fragmentActivity.get(), config);
            } else if (fragment != null) {
                EMPhotoPickUtils.getAllSelector(fragment.get(), config);
            }
        }
        //只启动相机
        else if (config.getEMMediaPickerEnum() == EMMediaPickerEnum.CAMERA) {
            if (fragmentActivity != null) {
                EMCameraUtils.startCamera(fragmentActivity.get(), config);
            } else if (fragment != null) {
                EMCameraUtils.startCamera(fragment.get(), config);
            }

        }
        //启动下方弹框
        else {
            if (fragmentActivity != null) {
                EMCameraDialogFragment.setConfig(fragmentActivity.get(), config);
            } else if (fragment != null) {
                EMCameraDialogFragment.setConfig(fragment.get(), config);
            }
            EMCameraDialogFragment.show(manager, "cameraDialogFragment");
        }
    }


    /**
     * 根据路径得到视频缩略图
     *
     * @param videoPath
     * @return
     */
    public static Bitmap getVideoPhoto(String videoPath) {
        MediaMetadataRetriever media = new MediaMetadataRetriever();
        media.setDataSource(videoPath);
        Bitmap bitmap = media.getFrameAtTime();
        return bitmap;
    }

    /**
     * 获取视频总时长
     *
     * @param context
     * @param uri
     * @return
     */
    public static int getVideoDuration(Context context, Uri uri) {
        MediaMetadataRetriever media = new MediaMetadataRetriever();
        media.setDataSource(context, uri);
        String duration = media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Integer.parseInt(duration);
    }


    /**
     * 获取文件类型
     *
     * @param url
     * @return
     */
    public static String getFileType(String url) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    public static List<String> getResultData(Context context, int requestCode, int resultCode, @Nullable Intent data) {
        List<String> result = new ArrayList<>();
        if (resultCode == RESULT_OK) {
            if (requestCode == EMConstant.REQUEST_CODE_CHOOSE) {
                result = EMMatisse.obtainPathResult(data);
            } else if (requestCode == EMConstant.CAMERA_RESULT_CODE) {
                result = data.getStringArrayListExtra(EMConstant.CAMERA_PATH);
            }
        }
        if (resultCode == EMConstant.CAMERA_ERROR_CODE) {
            Toast.makeText(context, "请检查相机权限", Toast.LENGTH_SHORT).show();
        }
        return result;
    }

    public static Builder builder(FragmentActivity fragmentActivity) {
        return new Builder(fragmentActivity);
    }

    public static Builder builder(Fragment fragment) {
        return new Builder(fragment);
    }

    private void setConfig(EMMediaPickerConfig config) {
        this.config = config;
    }


    /**
     * 设置各参数默认值
     */
    public static class Builder {
        private FragmentManager manager;
        private WeakReference<Fragment> fragment;
        private WeakReference<FragmentActivity> fragmentActivity;
        private boolean countable;
        private boolean originalEnable;
        private boolean isMirror;
        private int maxOriginalSize;
        private int maxImageSelectable;
        private int maxVideoSelectable;
        private int maxWidth;
        private int maxHeight;
        private int maxImageSize;
        private int maxVideoLength;
        private int maxVideoSize;
        private EMImageEngine EMImageEngine;
        private EMMediaPickerEnum mediaPickerType;

        private Builder(FragmentActivity fragmentActivity) {
            this.fragmentActivity = new WeakReference<>(fragmentActivity);
            this.manager = fragmentActivity.getSupportFragmentManager();
            setDefault();
        }

        private Builder(Fragment fragment) {
            this.fragment =  new WeakReference<>(fragment);
            this.manager = fragment.getChildFragmentManager();
            setDefault();
        }

        /**
         * 设置默认值
         */
        private void setDefault() {
            countable = true;
            isMirror = true;
            originalEnable = false;
            maxOriginalSize = 15;
            maxImageSelectable = 9;
            maxVideoSelectable = 1;
            maxWidth = 1920;
            maxHeight = 1920;
            maxImageSize = 15;
            maxVideoLength = 20000;
            maxVideoSize = 20;
            mediaPickerType = EMMediaPickerEnum.BOTH;
        }

        public Builder withIsMirror(boolean isMirror) {
            this.isMirror = isMirror;
            return this;
        }

        public Builder withCountable(boolean countable) {
            this.countable = countable;
            return this;
        }

        public Builder withOriginalEnable(boolean originalEnable) {
            this.originalEnable = originalEnable;
            return this;
        }

        public Builder withMaxOriginalSize(int maxOriginalSize) {
            this.maxOriginalSize = maxOriginalSize;
            return this;
        }

        public Builder withMaxImageSelectable(int maxImageSelectable) {
            this.maxImageSelectable = maxImageSelectable;
            return this;
        }

        public Builder withMaxVideoSelectable(int maxVideoSelectable) {
            this.maxVideoSelectable = maxVideoSelectable;
            return this;
        }

        public Builder withMaxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        public Builder withMaxHeight(int maxHeight) {
            this.maxHeight = maxHeight;
            return this;
        }

        public Builder withMaxImageSize(int maxImageSize) {
            this.maxImageSize = maxImageSize;
            return this;
        }

        public Builder withMaxVideoLength(int maxVideoLength) {
            this.maxVideoLength = maxVideoLength;
            return this;
        }

        public Builder withMaxVideoSize(int maxVideoSize) {
            this.maxVideoSize = maxVideoSize;
            return this;
        }

        public Builder withImageEngine(EMImageEngine EMImageEngine) {
            this.EMImageEngine = EMImageEngine;
            return this;
        }

        public Builder withMediaPickerType(EMMediaPickerEnum mediaPickerType) {
            this.mediaPickerType = mediaPickerType;
            return this;
        }


        public EMSmartMediaPicker build(Context context) {
            EMSmartMediaPicker EMSmartMediaPicker = new EMSmartMediaPicker(context);
            EMMediaPickerConfig config = new EMMediaPickerConfig();
            EMSmartMediaPicker.manager = manager;
            EMSmartMediaPicker.fragment = fragment;
            EMSmartMediaPicker.fragmentActivity = fragmentActivity;
            config.setCountable(countable);
            config.setMirror(isMirror);
            config.setOriginalEnable(originalEnable);
            config.setMaxOriginalSize(maxOriginalSize);
            config.setMaxImageSelectable(maxImageSelectable);
            config.setMaxVideoSelectable(maxVideoSelectable);
            config.setMaxWidth(maxWidth);
            config.setMaxHeight(maxHeight);
            config.setMaxImageSize(maxImageSize);
            config.setMaxVideoLength(maxVideoLength);
            config.setMaxVideoSize(maxVideoSize);
            config.setEMImageEngine(EMImageEngine);
            config.setEMMediaPickerEnum(mediaPickerType);
            EMSmartMediaPicker.setConfig(config);
            return EMSmartMediaPicker;
        }
    }
}
