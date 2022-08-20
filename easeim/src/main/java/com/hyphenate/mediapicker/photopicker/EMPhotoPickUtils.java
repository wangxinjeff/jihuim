package com.hyphenate.mediapicker.photopicker;

import android.Manifest;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import android.support.v4.app.Fragment;

import com.hyphenate.mediapicker.config.EMConstant;
import com.hyphenate.mediapicker.config.EMMediaPickerConfig;
import com.hyphenate.mediapicker.filter.EMFilter;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.hyphenate.mediapicker.EMMatisse;

import com.hyphenate.easeim.R;


/**
 * 照片选择器
 *
 * @author : BaoZhou
 * @date : 2018/7/12 10:02
 */
public class EMPhotoPickUtils {
    static public void getAllSelector(final Fragment fragment, final EMMediaPickerConfig config) {

        /**
         * RxPermissions 参考：https://blog.csdn.net/qq_43546258/article/details/107712028
         */
        RxPermissions rxPermissions = new RxPermissions(fragment);
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE).subscribe(granted ->{
                    if(granted){
                        startMatisse(true, config, EMMatisse.from(fragment));
                    } else {
                        Toast.makeText(fragment.getContext(), "请确认开启读写存储权限", Toast.LENGTH_LONG)
                        .show();
                    }
            }
        );
    }


    static public void getAllSelector(final FragmentActivity fragmentActivity, final EMMediaPickerConfig config) {

        startMatisse(true, config, EMMatisse.from(fragmentActivity));

        RxPermissions rxPermissions = new RxPermissions(fragmentActivity);
        rxPermissions.request(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE).subscribe(granted ->{
                    if(granted){
                        startMatisse(true, config, EMMatisse.from(fragmentActivity));
                    } else {
                        Toast.makeText(fragmentActivity, "请确认开启读写存储权限", Toast.LENGTH_LONG)
                        .show();
                    }
                }

        );
    }

    private static void startMatisse(Boolean aBoolean, EMMediaPickerConfig config, EMMatisse EMMatisse) {
        if (aBoolean) {
            if (config.getEMImageEngine() == null) {
                throw new IllegalArgumentException("ImageEngine cannot be null");
            }
            EMMatisse.choose(config.getPhotoPickerMediaType())
                    .showSingleMediaType(config.getMaxVideoSelectable() == 0 || config.getMaxImageSelectable() == 0)
                    .theme(R.style.EMMatisse_Dracula)
                    .countable(config.isCountable())
                    .addFilter(new EMFileSizeFilter(config.getMaxWidth(), config.getMaxHeight(), config.getMaxVideoSize() * EMFilter.K * EMFilter.K, config.getMaxImageSize() * EMFilter.K * EMFilter.K, config.getMaxVideoLength()))
                    .maxSelectablePerMediaType(config.getMaxImageSelectable() == 0 ? 1 : config.getMaxImageSelectable()
                            , config.getMaxVideoSelectable() == 0 ? 1 : config.getMaxVideoSelectable())
                    .originalEnable(config.isOriginalEnable())
                    .maxOriginalSize(config.getMaxOriginalSize())
                    .imageEngine(config.getEMImageEngine())
                    .forResult(EMConstant.REQUEST_CODE_CHOOSE);

        }
    }

}


