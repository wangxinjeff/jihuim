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
package com.hyphenate.easeui.utils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.RequiresApi;

import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMConversation.EMConversationType;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMNormalFileMessageBody;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.easeui.EaseIM;
import com.hyphenate.easeim.R;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.provider.EaseUserProfileProvider;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.EMLog;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class EaseCommonUtils {
	private static final String TAG = "CommonUtils";
	/**
	 * check if network avalable
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isNetWorkConnected(Context context) {
		if (context != null) {
			ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
			if (mNetworkInfo != null) {
				return mNetworkInfo.isAvailable() && mNetworkInfo.isConnected();
			}
		}

		return false;
	}

	/**
	 * check if sdcard exist
	 * 
	 * @return
	 */
	public static boolean isSdcardExist() {
		return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
	}
	
	public static EMMessage createExpressionMessage(String toChatUsername, String expressioName, String identityCode){
	    EMMessage message = EMMessage.createTxtSendMessage("["+expressioName+"]", toChatUsername);
        if(identityCode != null){
            message.setAttribute(EaseConstant.MESSAGE_ATTR_EXPRESSION_ID, identityCode);
        }
        message.setAttribute(EaseConstant.MESSAGE_ATTR_IS_BIG_EXPRESSION, true);
        return message;
	}

	/**
     * Get digest according message type and content
     * 
     * @param message
     * @param context
     * @return
     */
    public static String getMessageDigest(EMMessage message, Context context, boolean showNick) {
        String digest = "";
        String nick = "";
        if(showNick){
            try {
                JSONObject userInfo = message.getJSONObjectAttribute(EaseConstant.MESSAGE_ATTR_USER_INFO);
                nick = userInfo.optString(EaseConstant.MESSAGE_ATTR_USER_NICK) + ":";
            } catch (HyphenateException e) {
                e.printStackTrace();
                EaseUserProfileProvider userProvider = EaseIM.getInstance().getUserProvider();
                if(userProvider != null && userProvider.getUser(message.getFrom()) != null) {
                    EaseUser user = userProvider.getUser(message.getFrom());
                    if(user != null) {
                        nick = user.getNickname() + ":";
                    }
                }
            }

            if(TextUtils.equals(":",nick) || TextUtils.isEmpty(nick)){
                EaseUser user = EaseUserUtils.getUserInfo(message.getFrom());
                if(user != null) {
                    nick = user.getNickname() + ":";
                }
            }
        }
        switch (message.getType()) {
        case LOCATION:
            if (message.direct() == EMMessage.Direct.RECEIVE) {
                digest = getString(context, R.string.location_recv);
                EaseUserProfileProvider userProvider = EaseIM.getInstance().getUserProvider();
                String from = message.getFrom();
                if(userProvider != null && userProvider.getUser(from) != null) {
                    EaseUser user = userProvider.getUser(from);
                    if(user != null) {
                        from = user.getNickname();
                    }
                }
                digest = String.format(digest, from);
            } else {
                digest = getString(context, R.string.location_prefix);
            }
            break;
        case IMAGE:
            digest = getString(context, R.string.picture);
            break;
        case VOICE:
            digest = getString(context, R.string.voice_prefix);
            break;
        case VIDEO:
            digest = getString(context, R.string.video);
            break;
        case CUSTOM:
            digest = getString(context, R.string.custom);
            break;
        case TXT:
            EMTextMessageBody txtBody = (EMTextMessageBody) message.getBody();
            if(txtBody != null){
                if(message.getStringAttribute(EaseConstant.MESSAGE_ATTR_CALL_STATE, "").equals(EaseConstant.CONFERENCE_STATE_CREATE)) {
                    nick = "";
                    digest = getString(context, R.string.em_initiated_call);
                }else if(message.getStringAttribute(EaseConstant.MESSAGE_ATTR_CALL_STATE, "").equals(EaseConstant.CONFERENCE_STATE_END)) {
                    digest = getString(context, R.string.em_call_over);
                    nick = "";
                } else if(message.getBooleanAttribute(EaseConstant.MESSAGE_TYPE_RECALL, false)){
                    digest = nick + " 撤回了一条消息";
                    nick = "";
                } else if(message.getBooleanAttribute(EaseConstant.CREATE_GROUP_PROMPT, false)){
                    String groupName = message.getStringAttribute(EaseConstant.GROUP_NAME, "");
                    digest = String.format(context.getString(R.string.em_group_create_success), groupName);
                    nick = "";
                } else if(message.getBooleanAttribute(EaseConstant.JOIN_GROUP_PROMPT, false)){
                    String username = message.getStringAttribute(EaseConstant.USER_NAME, "");
                    EaseUserProfileProvider userProvider = EaseIM.getInstance().getUserProvider();
                    if(userProvider != null && userProvider.getUser(username) != null) {
                        EaseUser user = userProvider.getUser(username);
                        if(user != null) {
                            username = user.getNickname();
                        }
                    }
                    digest = username + context.getString(R.string.em_joined_group);
                    nick = "";
                } else{
                    digest = txtBody.getMessage();
                }
            }
            break;
        case FILE:
            EMNormalFileMessageBody fileBody = (EMNormalFileMessageBody) message.getBody();
            digest = "[文件]" + fileBody.getFileName();
            break;
        default:
            EMLog.e(TAG, "error, unknow type");
            return "";
        }
        Log.e("TAG", "message text = "+digest);
        return nick + digest;
    }
    
    static String getString(Context context, int resId){
        return context.getResources().getString(resId);
    }
	
	/**
	 * get top context
	 * @param context
	 * @return
	 */
	public static String getTopActivity(Context context) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);

		if (runningTaskInfos != null)
			return runningTaskInfos.get(0).topActivity.getClassName();
		else
			return "";
	}
	
	/**
     * set initial letter of according user's nickname( username if no nickname)
     * 
     * @param user
     */
    public static void setUserInitialLetter(EaseUser user) {
        final String DefaultLetter = "#";
        String letter = DefaultLetter;
        
        final class GetInitialLetter {
            String getLetter(String name) {
                if (TextUtils.isEmpty(name)) {
                    return DefaultLetter;
                }
                char char0 = name.toLowerCase().charAt(0);
                if (Character.isDigit(char0)) {
                    return DefaultLetter;
                }
                String pinyin = HanziToPinyin.getPinyin(name);
                if(!TextUtils.isEmpty(pinyin)) {
                    String letter = pinyin.substring(0, 1).toUpperCase();
                    char c = letter.charAt(0);
                    if(c < 'A' || c > 'Z') {
                        return DefaultLetter;
                    }
                    return letter;
                }
                return DefaultLetter;
            }
        }
        
        if ( !TextUtils.isEmpty(user.getNickname()) ) {
            letter = new GetInitialLetter().getLetter(user.getNickname());
            user.setInitialLetter(letter);
            return;
        } 
        if (letter.equals(DefaultLetter) && !TextUtils.isEmpty(user.getUsername())) {
            letter = new GetInitialLetter().getLetter(user.getUsername());
        }
        user.setInitialLetter(letter);
    }
    
    /**
     * change the chat type to EMConversationType
     * @param chatType
     * @return
     */
    public static EMConversationType getConversationType(int chatType) {
        if (chatType == EaseConstant.CHATTYPE_SINGLE) {
            return EMConversationType.Chat;
        } else if (chatType == EaseConstant.CHATTYPE_GROUP) {
            return EMConversationType.GroupChat;
        } else {
            return EMConversationType.ChatRoom;
        }
    }

    /**
     * get chat type by conversation type
     * @param conversation
     * @return
     */
    public static int getChatType(EMConversation conversation) {
        if(conversation.isGroup()) {
            if(conversation.getType() == EMConversationType.ChatRoom) {
                return EaseConstant.CHATTYPE_CHATROOM;
            }else {
                return EaseConstant.CHATTYPE_GROUP;
            }
        }else {
            return EaseConstant.CHATTYPE_SINGLE;
        }
    }

    /**
     * \~chinese
     * 判断是否是免打扰的消息,如果是app中应该不要给用户提示新消息
     * @param message
     * return
     *
     * \~english
     * check if the message is kind of slient message, if that's it, app should not play tone or vibrate
     *
     * @param message
     * @return
     */
    public static boolean isSilentMessage(EMMessage message){
        return message.getBooleanAttribute("em_ignore_notification", false);
    }

    /**
     * 获取屏幕的基本信息
     * @param context
     * @return
     */
    public static float[] getScreenInfo(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        float[] info = new float[5];
        if(manager != null) {
            DisplayMetrics dm = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(dm);
            info[0] = dm.widthPixels;
            info[1] = dm.heightPixels;
            info[2] = dm.densityDpi;
            info[3] = dm.density;
            info[4] = dm.scaledDensity;
        }
        return info;
    }

    /**
     * dip to px
     * @param context
     * @param value
     * @return
     */
    public static float dip2px(Context context, float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }

    /**
     * sp to px
     * @param context
     * @param value
     * @return
     */
    public static float sp2px(Context context, float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, context.getResources().getDisplayMetrics());
    }

    /**
     * 判断是否是时间戳
     * @param time
     * @return
     */
    public static boolean isTimestamp(String time) {
        if(TextUtils.isEmpty(time)) {
            return false;
        }
        long timestamp = 0L;
        try {
            timestamp = Long.parseLong(time);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
        return timestamp > 0;
    }

    /**
     * 获取首字母
     * @param name
     * @return
     */
    public static String getLetter(String name) {
        return new GetInitialLetter().getLetter(name);
    }

    private static class GetInitialLetter {
        private String defaultLetter = "#";

        /**
         * 获取首字母
         * @param name
         * @return
         */
        public String getLetter(String name) {
            if(TextUtils.isEmpty(name)) {
                return defaultLetter;
            }
            char char0 = name.toLowerCase().charAt(0);
            if(Character.isDigit(char0)) {
                return defaultLetter;
            }
            String pinyin = HanziToPinyin.getPinyin(name);
            if(!TextUtils.isEmpty(pinyin)) {
                String letter = pinyin.substring(0, 1).toUpperCase();
                char c = letter.charAt(0);
                if(c < 'A' || c > 'Z') {
                    return defaultLetter;
                }
                return letter;
            }
            return defaultLetter;
        }
    }

    public static void showSoftKeyBoard(EditText editText){
        editText.requestFocus();
        InputMethodManager inputMethodManager = (InputMethodManager)editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    public static void hideSoftKeyBoard(EditText editText){
        editText.clearFocus();
        InputMethodManager inputMethodManager = (InputMethodManager)editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public static boolean isNetConnection(Context mContext) {
        if (mContext!=null){
            ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if(networkInfo != null){
                boolean connected = networkInfo.isConnected();
                if (connected){
                    if (networkInfo.getState()== NetworkInfo.State.CONNECTED){
                        return true;
                    }else{
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 通过uri  获取文件路径
     * @param context
     * @param imageUri
     * @return
     */
    public static String getFileAbsolutePath(Context context, Uri imageUri) {
        if (context == null || imageUri == null) {
            return null;
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            return getRealFilePath(context, imageUri);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT && android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && DocumentsContract.isDocumentUri(context, imageUri)) {
            if (isExternalStorageDocument(imageUri)) {
                String docId = DocumentsContract.getDocumentId(imageUri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(imageUri)) {
                String id = DocumentsContract.getDocumentId(imageUri);
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(imageUri)) {
                String docId = DocumentsContract.getDocumentId(imageUri);
                String[] split = docId.split(":");
                String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                String selection = MediaStore.Images.Media._ID + "=?";
                String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } // MediaStore (and general)
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            return uriToFileApiQ(context,imageUri);
        }
        else if ("content".equalsIgnoreCase(imageUri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(imageUri)) {
                return imageUri.getLastPathSegment();
            }
            return getDataColumn(context, imageUri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(imageUri.getScheme())) {
            return imageUri.getPath();
        }
        return null;
    }

    //此方法 只能用于4.4以下的版本
    private static String getRealFilePath(final Context context, final Uri uri) {
        if (null == uri) {
            return null;
        }
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            String[] projection = {MediaStore.Images.ImageColumns.DATA};
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

//            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = MediaStore.Images.Media.DATA;
        String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * Android 10 以上适配
     * @param context
     * @param uri
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private static String uriToFileApiQ(Context context, Uri uri) {
        File file = null;
        //android10以上转换
        if (uri.getScheme().equals(ContentResolver.SCHEME_FILE)) {
            file = new File(uri.getPath());
        } else if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            //把文件复制到沙盒目录
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor.moveToFirst()) {
                String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                try {
                    InputStream is = contentResolver.openInputStream(uri);
                    File cache = new File(context.getExternalCacheDir().getAbsolutePath(), Math.round((Math.random() + 1) * 1000) + displayName);
                    FileOutputStream fos = new FileOutputStream(cache);
                    FileUtils.copy(is, fos);
                    file = cache;
                    fos.close();
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file.getAbsolutePath();
    }
}