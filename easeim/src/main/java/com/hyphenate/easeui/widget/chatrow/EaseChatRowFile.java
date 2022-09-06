package com.hyphenate.easeui.widget.chatrow;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMNormalFileMessageBody;
import com.hyphenate.easeim.R;
import com.hyphenate.easeui.utils.EaseEditTextUtils;
import com.hyphenate.easeui.utils.EaseFileUtils;
import com.hyphenate.util.TextFormater;

/**
 * file for row
 */
public class EaseChatRowFile extends EaseChatRow {
    private static final String TAG = EaseChatRowFile.class.getSimpleName();
    /**
     * file name
     */
    protected TextView fileNameView;
    /**
     * file's size
     */
	protected TextView fileSizeView;
    /**
     * file state
     */
    protected TextView fileStateView;
    private EMNormalFileMessageBody fileMessageBody;
    private ImageView fileTypeImg;

    public EaseChatRowFile(Context context, boolean isSender) {
        super(context, isSender);
    }

    public EaseChatRowFile(Context context, EMMessage message, int position, Object adapter) {
        super(context, message, position, adapter);
    }

    @Override
	protected void onInflateView() {
	    inflater.inflate(!showSenderType ? R.layout.ease_row_received_file
                : R.layout.ease_row_sent_file, this);
	}

	@Override
	protected void onFindViewById() {
	    fileNameView = (TextView) findViewById(R.id.tv_file_name);
        fileSizeView = (TextView) findViewById(R.id.tv_file_size);
        fileStateView = (TextView) findViewById(R.id.tv_file_state);
        percentageView = (TextView) findViewById(R.id.percentage);
        if(percentageView != null){
            percentageView.setVisibility(INVISIBLE);
        }
        fileTypeImg = findViewById(R.id.file_type);
	}

	@Override
	protected void onSetUpView() {
	    fileMessageBody = (EMNormalFileMessageBody) message.getBody();
        Uri filePath = fileMessageBody.getLocalUri();
        fileNameView.setText(fileMessageBody.getFileName());
        fileNameView.post(()-> {
            String content = EaseEditTextUtils.ellipsizeMiddleString(fileNameView,
                        fileMessageBody.getFileName(),
                        fileNameView.getMaxLines(),
                        fileNameView.getWidth() - fileNameView.getPaddingLeft() - fileNameView.getPaddingRight());
            fileNameView.setText(content);
        });

        if(fileMessageBody.getFileName().contains(".jpg") || fileMessageBody.getFileName().contains(".JPG")
                || fileMessageBody.getFileName().contains(".png") || fileMessageBody.getFileName().contains(".PNG")
                || fileMessageBody.getFileName().contains(".jpeg") || fileMessageBody.getFileName().contains(".JPEG")){
            fileTypeImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.em_icon_file_img));
        } else if (fileMessageBody.getFileName().contains(".doc")){
            fileTypeImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.em_icon_file_doc));
        } else if (fileMessageBody.getFileName().contains(".exel") || fileMessageBody.getFileName().contains(".xlsx")){
            fileTypeImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.em_icon_file_exel));
        } else if (fileMessageBody.getFileName().contains(".pdf")){
            fileTypeImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.em_icon_file_pdf));
        } else if (fileMessageBody.getFileName().contains(".txt")){
            fileTypeImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.em_icon_file_text));
        } else if (fileMessageBody.getFileName().contains(".docx")){
            fileTypeImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.em_icon_file_docx));
        } else if (fileMessageBody.getFileName().contains(".ppt")){
            fileTypeImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.em_icon_file_ppt));
        } else if (fileMessageBody.getFileName().contains(".mp4")){
            fileTypeImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.em_icon_file_mp4));
        } else if (fileMessageBody.getFileName().contains(".mov")){
            fileTypeImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.em_icon_file_mov));
        } else {
            fileTypeImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.em_icon_file_other));
        }

        fileSizeView.setText(TextFormater.getDataSize(fileMessageBody.getFileSize()));
        if (message.direct() == EMMessage.Direct.SEND){
            if (EaseFileUtils.isFileExistByUri(context, filePath)
                    && message.status() == EMMessage.Status.SUCCESS) {
                fileStateView.setText(R.string.have_uploaded);
            }else {
                fileStateView.setText("");
            }
        }
        if (message.direct() == EMMessage.Direct.RECEIVE) {
            if (EaseFileUtils.isFileExistByUri(context, filePath)) {
                fileStateView.setText(R.string.have_downloaded);
            } else {
                fileStateView.setText(R.string.did_not_download);
            }
        }
	}

    @Override
    protected void onMessageCreate() {
        super.onMessageCreate();
        progressBar.setVisibility(View.VISIBLE);
//        if (percentageView != null)
//            percentageView.setVisibility(View.INVISIBLE);
        if (statusView != null)
            statusView.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onMessageSuccess() {
        super.onMessageSuccess();
        progressBar.setVisibility(View.INVISIBLE);
//        if (percentageView != null)
//            percentageView.setVisibility(View.INVISIBLE);
        if (statusView != null)
            statusView.setVisibility(View.INVISIBLE);
        if (message.direct() == EMMessage.Direct.SEND)
            if(fileStateView != null) {
                fileStateView.setText(R.string.have_uploaded);
            }
    }

    @Override
    protected void onMessageError() {
        super.onMessageError();
        progressBar.setVisibility(View.INVISIBLE);
//        if (percentageView != null)
//            percentageView.setVisibility(View.INVISIBLE);
        if (statusView != null)
            statusView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onMessageInProgress() {
        super.onMessageInProgress();
        if(progressBar.getVisibility() != VISIBLE) {
            progressBar.setVisibility(View.VISIBLE);
        }
//        if (percentageView != null) {
//            percentageView.setVisibility(View.VISIBLE);
//            percentageView.setText(message.progress() + "%");
//        }
        if (statusView != null)
            statusView.setVisibility(View.INVISIBLE);
    }
}
