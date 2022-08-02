package com.hyphenate.mediapicker.camera;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.hyphenate.easeim.R;
import com.hyphenate.mediapicker.config.MediaPickerConfig;
import com.hyphenate.mediapicker.photopicker.PhotoPickUtils;

import java.lang.ref.WeakReference;



/**
 * 选择拍照
 *
 * @author : BaoZhou
 * @date : 2018/3/19 21:14
 */
public class CameraDialogFragment extends DialogFragment {

    private ImageView ivTakePhoto;
    private ImageView ivPickPhoto;
    private ImageView ivCancel;
    private MediaPickerConfig config;
    private WeakReference<Fragment> fragment;
    private WeakReference<FragmentActivity> fragmentActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(true);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View view = inflater.inflate(R.layout.camera_select_photo_popout, null);
        initView(view);
        return view;
    }


    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    @Override
    public void onActivityCreated(Bundle arg0) {
        getDialog().getWindow().getAttributes().windowAnimations = R.style.PhotoDialog;
        super.onActivityCreated(arg0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        getDialog().getWindow().setLayout(dm.widthPixels, getDialog().getWindow().getAttributes().height);
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        final WindowManager.LayoutParams layoutParams = getDialog().getWindow().getAttributes();
        layoutParams.width = dm.widthPixels;
        layoutParams.gravity = Gravity.BOTTOM;
        getDialog().getWindow().setAttributes(layoutParams);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void initView(View view) {
        ivTakePhoto = view.findViewById(R.id.iv_take_photo);
        ivTakePhoto.setOnClickListener(v -> {
            if (fragment != null) {
                CameraUtils.startCamera(fragment.get(), config);
            }
            if (fragmentActivity != null) {
                CameraUtils.startCamera(fragmentActivity.get(), config);
            }
            dismiss();
        });
        ivPickPhoto = view.findViewById(R.id.iv_pick_photo);
        ivPickPhoto.setOnClickListener(v -> {
            if (fragment != null) {
                PhotoPickUtils.getAllSelector(fragment.get(), config);
            }
            if (fragmentActivity != null) {
                PhotoPickUtils.getAllSelector(fragmentActivity.get(), config);
            }

            dismiss();
        });
        ivCancel = view.findViewById(R.id.iv_cancel);
        ivCancel.setOnClickListener(v -> dismiss());
    }


    public void setConfig(Fragment fragment, MediaPickerConfig config) {
        this.fragment = new WeakReference<>(fragment);
        this.config = config;
        this.fragmentActivity = null;
    }

    public void setConfig(FragmentActivity fragmentActivity, MediaPickerConfig config) {
        this.fragment = null;
        this.config = config;
        this.fragmentActivity = new WeakReference<>(fragmentActivity);
    }
}
