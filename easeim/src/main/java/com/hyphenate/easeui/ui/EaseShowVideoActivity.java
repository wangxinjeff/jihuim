package com.hyphenate.easeui.ui;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMVideoMessageBody;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeui.ui.base.EaseBaseActivity;
import com.hyphenate.easeui.utils.EaseFileUtils;
import com.hyphenate.util.EMLog;

import java.io.File;

/**
 * show the video
 * 
 */
public class EaseShowVideoActivity extends EaseBaseActivity {
	private static final String TAG = "ShowVideoActivity";
	private LinearLayout loadFailedView;
	private ProgressBar progressBar;
	private Uri localFilePath;
	private RelativeLayout backView;
	private AppCompatImageView iconBack;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.ease_showvideo_activity);
		progressBar = (ProgressBar) findViewById(R.id.progress_bar);
		loadFailedView = findViewById(R.id.load_failed_view);

		backView = findViewById(R.id.back_view);
		iconBack = findViewById(R.id.icon_back);
		if(EaseIMHelper.getInstance().isAdmin()){
			iconBack.setImageResource(R.drawable.em_icon_back_normal);
		}
		backView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		final EMMessage message = getIntent().getParcelableExtra("msg");
		if (!(message.getBody() instanceof EMVideoMessageBody)) {
			Toast.makeText(EaseShowVideoActivity.this, getApplicationContext().getString(R.string.unsupported_message_body), Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		EMVideoMessageBody messageBody = (EMVideoMessageBody)message.getBody();

		localFilePath = messageBody.getLocalUri();
		EMLog.d(TAG, "localFilePath = "+localFilePath);
		EMLog.d(TAG, "local filename = "+messageBody.getFileName());

		//检查Uri读权限
		EaseFileUtils.takePersistableUriPermission(this, localFilePath);

		if(EaseFileUtils.isFileExistByUri(this, localFilePath)) {
		    showLocalVideo(localFilePath);
		} else {
			EMLog.d(TAG, "download remote video file");
			downloadVideo(message);
		}
	}

	private void showLocalVideo(Uri videoUri) {
		EaseShowLocalVideoActivity.actionStart(this, videoUri.toString());
//		String filePath = UriUtils.getFilePath(this, videoUri);
//		if(!TextUtils.isEmpty(filePath) && new File(filePath).exists()) {
//		    videoUri = EaseCompat.getUriForFile(this, new File(filePath));
//		}
//		try {
//			Intent intent = new Intent(Intent.ACTION_VIEW);
//			String mimeType = EaseCompat.getMimeType(this, UriUtils.getFileNameByUri(this, videoUri));
//			EMLog.d(TAG, "video uri = "+videoUri);
//			EMLog.d(TAG, "video mimeType = "+mimeType);
//			intent.setDataAndType(videoUri, mimeType);
//			// 注意添加该flag,用于Android7.0以上设备获取相册文件权限.
//			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//			startActivity(intent);
//		} catch (Exception e) {
//			e.printStackTrace();
//			EMLog.e(TAG, e.getMessage());
//		}
		finish();
	}

	/**
	 * download video file
	 */
	private void downloadVideo(final EMMessage message) {
		progressBar.setVisibility(View.VISIBLE);
		message.setMessageStatusCallback(new EMCallBack() {
			@Override
			public void onSuccess() {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if(isFinishing() || isDestroyed()){
							return;
						}
						runOnUiThread(() -> progressBar.setVisibility(View.GONE));
						showLocalVideo(((EMVideoMessageBody)message.getBody()).getLocalUri());
					}
				});
			}

			@Override
			public void onProgress(final int progress,String status) {
				Log.d("ease", "video progress:" + progress);
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						progressBar.setProgress(progress);
					}
				});

			}

			@Override
			public void onError(final int error, String msg) {
				Log.e("###", "offline file transfer error:" + msg);
				Uri localFilePath = ((EMVideoMessageBody) message.getBody()).getLocalUri();
				String filePath = EaseFileUtils.getFilePath(EaseShowVideoActivity.this, localFilePath);
				if(TextUtils.isEmpty(filePath)) {
				    EaseShowVideoActivity.this.getContentResolver().delete(localFilePath, null, null);
				}else {
					File file = new File(filePath);
					if (file.exists()) {
						file.delete();
					}
				}

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						progressBar.setVisibility(View.GONE);
						loadFailedView.setVisibility(View.VISIBLE);
						if (error == EMError.FILE_NOT_FOUND) {
							Toast.makeText(getApplicationContext(), R.string.Video_expired, Toast.LENGTH_SHORT).show();
						}
					}
				});
			}
		});
		EMClient.getInstance().chatManager().downloadAttachment(message);
	}

	@Override
	public void onBackPressed() {
		finish();
	}
 

}
