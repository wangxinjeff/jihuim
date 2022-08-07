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

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.documentfile.provider.DocumentFile;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
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
import com.hyphenate.util.UriUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * download and show original image
 * 
 */
public class EaseShowBigImageActivity extends EaseBaseActivity {
	private static final String TAG = "ShowBigImage"; 
	private EasePhotoView image;
	private int default_res = R.drawable.ease_default_image;
	private String filename;
	private Bitmap bitmap;
	private boolean isDownloaded;
	private LinearLayout loadView;
	private LinearLayout loadFailedView;
	private AppCompatImageView iconDownload;

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
		filename = getIntent().getExtras().getString("filename");
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
				boolean save = EaseCommonUtils.saveImage(getApplicationContext(), new File(filePath));
				EMLog.e(TAG, "save = " + save);
			}
		});
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
