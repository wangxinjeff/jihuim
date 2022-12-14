package com.hyphenate.mediapicker.camera;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.mediapicker.cameralibrary.JEMCameraView;
import com.hyphenate.mediapicker.cameralibrary.listener.EMClickListener;
import com.hyphenate.mediapicker.cameralibrary.listener.EMErrorListener;
import com.hyphenate.mediapicker.cameralibrary.listener.JEMCameraListener;
import com.hyphenate.mediapicker.cameralibrary.util.EMFileUtil;
import com.hyphenate.mediapicker.config.EMConstant;

import java.util.ArrayList;



/**
 * 录像照相
 *
 * @author : BaoZhou
 * @date : 2018/7/12 21:40
 */
public class EMCameraActivity extends BaseInitActivity {
    private JEMCameraView jCameraView;
    /**
     * BUTTON_STATE_ONLY_CAPTURE = 0x101;      //只能拍照
     * BUTTON_STATE_ONLY_RECORDER = 0x102;     //只能录像
     * BUTTON_STATE_BOTH = 0x103;              //两者都可以
     */
    public int buttonState;


    public int duration;

    public boolean isMirror;
    
    @Override
    protected int getLayoutId() {
        return R.layout.em_camera_layout;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        buttonState = getIntent().getIntExtra(EMConstant.BUTTON_STATE, JEMCameraView.BUTTON_STATE_BOTH);
        duration = getIntent().getIntExtra(EMConstant.DURATION, 10 * 1000);
        isMirror = getIntent().getBooleanExtra(EMConstant.IS_MIRROR, true);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        jCameraView = findViewById(R.id.jcameraview);
        //设置视频保存路径
        jCameraView.setFeatures(JEMCameraView.BUTTON_STATE_BOTH);
        jCameraView.setMediaQuality(JEMCameraView.MEDIA_QUALITY_MIDDLE);
        jCameraView.setDuration(duration);
        jCameraView.setMirror(isMirror);
        jCameraView.setErrorListener(new EMErrorListener() {
            @Override
            public void onError() {
                //错误监听
                Intent intent = new Intent();
                setResult(103, intent);
                finishActivityWithAnim();
            }

            @Override
            public void AudioPermissionError() {
                Toast.makeText(EMCameraActivity.this, "需要打开录音权限?", Toast.LENGTH_SHORT).show();
            }
        });
        if (buttonState != 0)
            jCameraView.setFeatures(buttonState);
        else {
            jCameraView.setFeatures(JEMCameraView.BUTTON_STATE_BOTH);
        }
        //JCameraView监听
        if (JEMCameraView.BUTTON_STATE_ONLY_CAPTURE == buttonState) {//只拍照
            jCameraView.setTip("轻触拍照");
        } else if (JEMCameraView.BUTTON_STATE_ONLY_RECORDER == buttonState) {//只拍摄
            jCameraView.setTip("长按拍摄");
        } else {
            jCameraView.setTip("轻触拍照，长按录制视频");
        }
        jCameraView.setJCameraListener(new JEMCameraListener() {
            @Override
            public void captureSuccess(Bitmap bitmap) {
                //获取图片bitmap
                String path = EMFileUtil.saveBitmap("capture_photo", bitmap);
                ArrayList<String> paths = new ArrayList<>(1);
                paths.add(path);
                Intent intent = new Intent();
                intent.putExtra(EMConstant.CAMERA_PATH, paths);
                setResult(RESULT_OK, intent);
                finishActivityWithAnim();
            }

            @Override
            public void recordSuccess(String url, Bitmap firstFrame) {
                //获取视频路径
                ArrayList<String> paths = new ArrayList<>(1);
                paths.add(url);
                Intent intent = new Intent();
                intent.putExtra(EMConstant.CAMERA_PATH, paths);
                setResult(RESULT_OK, intent);
                finishActivityWithAnim();
            }

        });

        jCameraView.setLeftClickListener(new EMClickListener() {
            @Override
            public void onClick() {
                EMCameraActivity.this.finishActivityWithAnim();
            }
        });
        jCameraView.setRightClickListener(new EMClickListener() {
            @Override
            public void onClick() {
                Toast.makeText(EMCameraActivity.this, "Right", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        //全屏显示
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(option);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        jCameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        jCameraView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void finishActivityWithAnim() {
        finish();
        overridePendingTransition(0, R.anim.em_camera_push_bottom_out);
    }

    @Override
    public void onBackPressed() {
        finishActivityWithAnim();
    }
}
