/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hyphenate.easeui.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.hyphenate.EMCallBack;
import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMImageMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeim.R;
import com.hyphenate.easeui.ui.base.EaseBaseActivity;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.easeui.utils.EaseFileUtils;
import com.hyphenate.easeui.widget.photoview.EasePhotoView;
import com.hyphenate.easeui.widget.photoview.PhotoViewAttacher;
import com.hyphenate.util.EMLog;

import java.io.OutputStream;

/**
 * download and show original image
 * 
 */
public class EaseShowBigImageActivity extends EaseBaseActivity {
	private static final String TAG = "ShowBigImage"; 
	private EasePhotoView image;
	private boolean isDownloaded;
	private LinearLayout loadView;
	private LinearLayout loadFailedView;
	private FrameLayout iconDownload;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.ease_activity_show_big_image);
		super.onCreate(savedInstanceState);
		setFitSystemForTheme(true, R.color.black, false);
		image = (EasePhotoView) findViewById(R.id.image);
		loadView = findViewById(R.id.load_view);
		loadFailedView = findViewById(R.id.load_failed_view);
		iconDownload = findViewById(R.id.icon_download);
		Uri uri = getIntent().getParcelableExtra("uri");
		String msgId = getIntent().getExtras().getString("messageId");
		EMLog.d(TAG, "show big msgId:" + msgId );
		EMMessage msg = EMClient.getInstance().chatManager().getMessage(msgId);
		EMImageMessageBody body = (EMImageMessageBody)msg.getBody();
		Uri thumbUri = body.thumbnailLocalUri();
		if(EaseFileUtils.isFileExistByUri(this, thumbUri)){
			Glide.with(this).load(thumbUri).into(image);
		}

		//show the image if it exist in local path
		if (EaseFileUtils.isFileExistByUri(this, uri)) {
            Glide.with(this).load(uri).into(image);
			iconDownload.setVisibility(View.VISIBLE);
		} else if(msgId != null) {
		    downloadImage(msgId);
		}

		image.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
			@Override
			public void onPhotoTap(View view, float x, float y) {
				finish();
			}
		});

		iconDownload.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Uri fileUri = ((EMImageMessageBody) msg.getBody()).getLocalUri();
				String filePath = EaseCommonUtils.getFileAbsolutePath(getApplicationContext(), fileUri);
				Bitmap bitmap = BitmapFactory.decodeFile(filePath);
				//在API29及之后是不需要申请的，默认是允许的
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(EaseShowBigImageActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
					ActivityCompat.requestPermissions(EaseShowBigImageActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
				} else {
					//保存图片到相册
					SaveImage(bitmap);
				}
			}
		});
	}

	//请求权限后的结果回调
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 0) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

			} else {
				Toast.makeText(this, "你拒绝了该权限，无法保存图片！", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void SaveImage(Bitmap bitmap) {
		//创建一个子线程，将耗时任务在子线程中完成，防止主线程被阻塞
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					//创建一个保存的Uri
					Uri saveUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
					if (TextUtils.isEmpty(saveUri.toString())) {
						Looper.prepare();
						Toast.makeText(EaseShowBigImageActivity.this, "保存失败！", Toast.LENGTH_SHORT).show();
						Looper.loop();
						return;
					}
					OutputStream outputStream = getContentResolver().openOutputStream(saveUri);
					//将位图写出到指定的位置
					//第一个参数：格式JPEG 是可以压缩的一个格式 PNG 是一个无损的格式
					//第二个参数：保留原图像90%的品质，压缩10% 这里压缩的是存储大小
					//第三个参数：具体的输出流
					if (bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)) {
						Looper.prepare();
						Toast.makeText(EaseShowBigImageActivity.this, "保存成功！", Toast.LENGTH_SHORT).show();
						Looper.loop();
					} else {
						Looper.prepare();
						Toast.makeText(EaseShowBigImageActivity.this, "保存失败！", Toast.LENGTH_SHORT).show();
						Looper.loop();
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	/**
	 * download image
	 * 
	 * @param msgId
	 */
	@SuppressLint("NewApi")
	private void downloadImage(final String msgId) {
        EMLog.e(TAG, "download with messageId: " + msgId);
		loadView.setVisibility(View.VISIBLE);
        final EMMessage msg = EMClient.getInstance().chatManager().getMessage(msgId);
        final EMCallBack callback = new EMCallBack() {
			public void onSuccess() {
			    EMLog.e(TAG, "onSuccess" );
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!isFinishing() && !isDestroyed()) {
							runOnUiThread(() -> loadView.setVisibility(View.GONE));
							isDownloaded = true;
							Uri localUrlUri = ((EMImageMessageBody) msg.getBody()).getLocalUri();
							Glide.with(EaseShowBigImageActivity.this)
									.load(localUrlUri)
									.into(image);
							iconDownload.setVisibility(View.VISIBLE);
						}
					}
				});
			}

			public void onError(final int error, String message) {
				EMLog.e(TAG, "offline file transfer error:" + message);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (EaseShowBigImageActivity.this.isFinishing() || EaseShowBigImageActivity.this.isDestroyed()) {
						    return;
						}
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								image.setVisibility(View.GONE);
								loadView.setVisibility(View.GONE);
								loadFailedView.setVisibility(View.VISIBLE);
							}
						});
                        if (error == EMError.FILE_NOT_FOUND) {
							Toast.makeText(getApplicationContext(), R.string.Image_expired, Toast.LENGTH_SHORT).show();
						}
					}
				});
			}

			public void onProgress(final int progress, String status) {
				EMLog.d(TAG, "Progress: " + progress);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
                        if (EaseShowBigImageActivity.this.isFinishing() || EaseShowBigImageActivity.this.isDestroyed()) {
                            return;
                        }
					}
				});
			}
		};
		

		msg.setMessageStatusCallback(callback);

		EMClient.getInstance().chatManager().downloadAttachment(msg);
	}

	@Override
	public void onBackPressed() {
		if (isDownloaded)
			setResult(RESULT_OK);
		finish();
	}
}
