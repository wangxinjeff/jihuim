package com.hyphenate.easeim.common.manager;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMImageMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.livedatas.LiveDataBus;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.easeui.utils.EaseFileUtils;
import com.hyphenate.exceptions.HyphenateException;

import java.util.Map;

/**
 * 用于处理推送及消息相关
 */
public class PushAndMessageHelper {

    private static boolean isLock;


    /**
     * 转发消息
     * @param toChatUsername
     * @param msgId
     */
    public static void sendForwardMessage(String toChatUsername, String msgId) {
        if(TextUtils.isEmpty(msgId)) {
            return;
        }
        EMMessage message = EaseIMHelper.getInstance().getChatManager().getMessage(msgId);
        EMMessage.Type type = message.getType();
        switch (type) {
            case TXT:
                if(message.getBooleanAttribute(EaseConstant.MESSAGE_ATTR_IS_BIG_EXPRESSION, false)){
                    sendBigExpressionMessage(toChatUsername, ((EMTextMessageBody) message.getBody()).getMessage(),
                            message.getStringAttribute(EaseConstant.MESSAGE_ATTR_EXPRESSION_ID, null));
                }else{
                    // get the content and send it
                    String content = ((EMTextMessageBody) message.getBody()).getMessage();
                    sendTextMessage(toChatUsername, content);
                }
                break;
            case IMAGE:
                // send image
                Uri uri = getImageForwardUri((EMImageMessageBody) message.getBody());
                if(uri != null) {
                    sendImageMessage(toChatUsername, uri);
                }else {
                    LiveDataBus.get().with(EaseConstant.MESSAGE_FORWARD)
                            .postValue(new EaseEvent(EaseIMHelper.getInstance().getApplication().getApplicationContext().getString(R.string.no_image_resource), EaseEvent.TYPE.MESSAGE));
                }
                break;
        }
    }

    public static Uri getImageForwardUri(EMImageMessageBody body) {
        if(body == null) {
            return null;
        }
        Uri localUri = body.getLocalUri();
        Context context = EaseIMHelper.getInstance().getApplication().getApplicationContext();
        if(EaseFileUtils.isFileExistByUri(context, localUri)) {
            return localUri;
        }
        localUri = body.thumbnailLocalUri();
        if(EaseFileUtils.isFileExistByUri(context, localUri)) {
            return localUri;
        }
        return null;
    }


    /**
     * send big expression message
     * @param toChatUsername
     * @param name
     * @param identityCode
     */
    private static void sendBigExpressionMessage(String toChatUsername, String name, String identityCode){
        EMMessage message = EaseCommonUtils.createExpressionMessage(toChatUsername, name, identityCode);
        sendMessage(message);
    }

    /**
     * 发送文本消息
     * @param toChatUsername
     * @param content
     */
    private static void sendTextMessage(String toChatUsername, String content) {
        EMMessage message = EMMessage.createTxtSendMessage(content, toChatUsername);
        sendMessage(message);
    }

    /**
     * send image message
     * @param toChatUsername
     * @param imageUri
     */
    private static void sendImageMessage(String toChatUsername, Uri imageUri) {
        EMMessage message = EMMessage.createImageSendMessage(imageUri, false, toChatUsername);
        sendMessage(message);
    }

    /**
     * send image message
     * @param toChatUsername
     * @param imagePath
     */
    private static void sendImageMessage(String toChatUsername, String imagePath) {
        EMMessage message = EMMessage.createImageSendMessage(imagePath, false, toChatUsername);
        sendMessage(message);
    }


    /**
     * send message
     * @param message
     */
    private static void sendMessage(EMMessage message) {
        message.setMessageStatusCallback(new EMCallBack() {
            @Override
            public void onSuccess() {
                LiveDataBus.get().with(EaseConstant.MESSAGE_FORWARD)
                        .postValue(new EaseEvent(EaseIMHelper.getInstance().getApplication().getString(R.string.has_been_send), EaseEvent.TYPE.MESSAGE));
            }

            @Override
            public void onError(int code, String error) {

            }

            @Override
            public void onProgress(int progress, String status) {

            }
        });
        // send message
        EMClient.getInstance().chatManager().sendMessage(message);

    }
}
