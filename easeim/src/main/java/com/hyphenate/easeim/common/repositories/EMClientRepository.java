package com.hyphenate.easeim.common.repositories;

import android.text.TextUtils;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMError;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.easecallkit.base.EaseCallKitTokenCallback;
import com.hyphenate.easecallkit.base.EaseGetUserAccountCallback;
import com.hyphenate.easecallkit.base.EaseUserAccount;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.common.db.EaseDbHelper;
import com.hyphenate.easeim.common.livedatas.LiveDataBus;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.manager.EaseThreadManager;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.easeui.modules.conversation.model.EaseConversationInfo;
import com.hyphenate.util.EMLog;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 作为EMClient的repository,处理EMClient相关的逻辑
 */
public class EMClientRepository extends BaseEMRepository{

    private static final String TAG = EMClientRepository.class.getSimpleName();
    private static EMClientRepository instance;

    public static EMClientRepository getInstance() {
        if(instance == null) {
            synchronized (EMClientRepository.class) {
                if(instance == null) {
                    instance = new EMClientRepository();
                }
            }
        }
        return instance;
    }
    /**
     * 从本地数据库加载所有的对话及群组
     */
    private void loadAllConversationsAndGroups() {
        // 初始化数据库
        initDb();
        // 从本地数据库加载所有的对话及群组
        getChatManager().loadAllConversations();
        EaseThreadManager.getInstance().runOnIOThread(() -> {
            getGroupManager().loadAllGroups();
        });
    }

    /**
     * 运管端登录获取token/im账号密码
     * @param phone
     * @param password
     * @param callBack
     */
    public void loginWithAdmin(String phone, String password, EMCallBack callBack){
        try {
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            OkHttpClient client = new OkHttpClient();
            JSONObject json = new JSONObject();
            json.put("phone", phone);
            json.put("password", password);
            RequestBody body = RequestBody.create(JSON, json.toString());
            Request request = new Request.Builder()
                    .url(EaseIMHelper.getInstance().getServerHost()+"v2/gov/arcfox/login")
                    .post(body)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    String responseBody = response.body().string();
                    if(response.code() == 200 && !TextUtils.isEmpty(responseBody)){
                        try {
                            JSONObject result = new JSONObject(responseBody);
                            String status = result.optString("status");
                            if(TextUtils.equals("OK", status) || TextUtils.equals("SUCCEED", status)){
                                JSONObject entity = result.getJSONObject("entity");
                                String token = entity.getString("token");
                                EaseIMHelper.getInstance().getModel().setAppToken(token);
                                JSONObject imUser = entity.getJSONObject("imUserToken");
                                String username = imUser.getString("username");
                                String password = imUser.getString("password");
                                loginToServer(username, password, callBack);
                            } else {
                                callBack.onError(EMError.USER_AUTHENTICATION_FAILED, "user login failed,username or password error.");
                            }

                        } catch (JSONException e) {
                            callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                        }
                    } else {
                        callBack.onError(EMError.USER_AUTHENTICATION_FAILED, "user login failed,username or password error.");
                    }
                }
            });
        } catch (JSONException e) {
            callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
        }
    }

    /**
     * 登录环信
     * @param userName
     * @param pwd
     * @param callBack
     */
    private void loginToServer(String userName, String pwd, EMCallBack callBack) {
        EMClient.getInstance().login(userName, pwd, new EMCallBack() {
            @Override
            public void onSuccess() {
                EaseIMHelper.getInstance().getModel().setCurrentUserName(userName);
                loginSuccess();
                //从服务器拉取加入的群，防止进入会话页面只显示id
                getAllJoinGroup();
                setAutoLogin(true);
                EMChatManagerRepository.getInstance().fetchConversationsFromServer(new ResultCallBack<List<EaseConversationInfo>>() {
                    @Override
                    public void onSuccess(List<EaseConversationInfo> data) {
                        // 拉取服务器的会话列表之后设置为不是初次登录
                        EaseIMHelper.getInstance().makeNotFirstInstall();
                        // 获取会话对应的群组信息
                        for(EaseConversationInfo info : data){
                            if(info.getInfo() instanceof EMConversation){
                                EMConversation conversation = (EMConversation) info.getInfo();
                                if(conversation.getType() == EMConversation.EMConversationType.GroupChat){
                                    getGroupManager().asyncGetGroupFromServer(conversation.conversationId(), new EMValueCallBack<EMGroup>() {
                                        @Override
                                        public void onSuccess(EMGroup emGroup) {

                                        }

                                        @Override
                                        public void onError(int i, String s) {

                                        }
                                    });
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(int i, String s) {

                    }
                });
                callBack.onSuccess();
            }

            @Override
            public void onError(int i, String s) {
                callBack.onError(i, s);
                closeDb();
            }
        });
    }


    /**
     * 运管端退出登录
     * @param callBack
     */
    public void logoutWithAdmin(EMCallBack callBack){
        OkHttpClient client = new OkHttpClient();
        Headers headers = new Headers.Builder()
                .add("Authorization", EaseIMHelper.getInstance().getModel().getAppToken())
                .add("username", EaseIMHelper.getInstance().getCurrentUser())
                .build();
        Request request = new Request.Builder()
                .url(EaseIMHelper.getInstance().getServerHost()+"v2/gov/arcfox/transport/"+ EaseIMHelper.getInstance().getCurrentUser() + "/logout")
                .headers(headers)
                .get()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                EaseIMHelper.getInstance().logoutSuccess();
                callBack.onSuccess();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                EaseIMHelper.getInstance().logoutSuccess();
                callBack.onSuccess();
            }
        });
    }

    /**
     * sdk 退出登录
     * @param callBack
     */
    public void logout(EMCallBack callBack){
        EMClient.getInstance().logout(true, new EMCallBack() {
            @Override
            public void onSuccess() {
//                logoutWithAdmin(callBack);
                EMLog.e("EaseIM", "logout success");
                EaseIMHelper.getInstance().logoutSuccess();
                callBack.onSuccess();
            }

            @Override
            public void onError(int i, String s) {
                EMClient.getInstance().logout(false, new EMCallBack() {
                    @Override
                    public void onSuccess() {
//                        logoutWithAdmin(callBack);
                        EMLog.e("EaseIM", "logout success");
                        EaseIMHelper.getInstance().logoutSuccess();
                        callBack.onSuccess();
                    }

                    @Override
                    public void onError(int i, String s) {
                        EMLog.e("EaseIM", "logout failed: " + i + ", " +s);
                        callBack.onError(i, s);
                    }
                });

            }
        });
    }

    /**
     * 设置本地标记，是否自动登录
     * @param autoLogin
     */
    public void setAutoLogin(boolean autoLogin) {
        EaseIMHelper.getInstance().setAutoLogin(autoLogin);
    }

    public void loginSuccess(){
        // ** manually load all local groups and conversation
        loadAllConversationsAndGroups();
    }

    private void getAllJoinGroup() {
        EMGroupManagerRepository.getInstance().getAllGroups(new ResultCallBack<List<EMGroup>>() {
            @Override
            public void onSuccess(List<EMGroup> value) {
                //加载完群组信息后，刷新会话列表页面，保证展示群组名称
                EMLog.i("ChatPresenter", "login isGroupsSyncedWithServer success");
                EaseEvent event = EaseEvent.create(EaseConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP);
                LiveDataBus.get().with(EaseConstant.GROUP_CHANGE).postValue(event);
            }

            @Override
            public void onError(int error, String errorMsg) {

            }
        });
    }

    private void closeDb() {
        EaseDbHelper.getInstance(EaseIMHelper.getInstance().getApplication()).closeDb();
    }

    /**
     * 运管端获取通话rtc token
     * @param userName
     * @param channelName
     * @param callBack
     */
    public void getRtcTokenWithAdmin(String userName, String channelName, EaseCallKitTokenCallback callBack){
        try {
            OkHttpClient client = new OkHttpClient();
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            JSONObject json = new JSONObject();
            json.put("username", userName);
            json.put("channelName", channelName);

            RequestBody body = RequestBody.create(JSON, json.toString());
            Headers headers = new Headers.Builder()
                    .add("Authorization", EaseIMHelper.getInstance().getModel().getAppToken())
                    .add("username", EaseIMHelper.getInstance().getCurrentUser())
                    .build();
            Request request = new Request.Builder()
                    .url(EaseIMHelper.getInstance().getServerHost() + "v2/rtc/token")
                    .headers(headers)
                    .post(body)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    callBack.onGetTokenError(EMError.GENERAL_ERROR, e.getMessage());
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    String responseBody = response.body().string();
                    if (response.code() == 200 && !TextUtils.isEmpty(responseBody)) {
                        try {
                            JSONObject result = new JSONObject(responseBody);
                            String status = result.optString("status");
                            if(TextUtils.equals("OK", status) || TextUtils.equals("SUCCEED", status)){
                                JSONObject entity = result.getJSONObject("entity");
                                String token = entity.optString("token");
                                int uid = entity.optInt("uid");
                                callBack.onSetToken(token, uid);
                            } else {
                                callBack.onGetTokenError(EMError.GENERAL_ERROR, "fetch rtc token failed");
                            }

                        } catch (JSONException e) {
                            callBack.onGetTokenError(EMError.GENERAL_ERROR, e.getMessage());
                        }
                    } else {
                        callBack.onGetTokenError(EMError.GENERAL_ERROR, "fetch rtc token failed");
                    }
                }
            });
        }catch(JSONException e){
            callBack.onGetTokenError(EMError.GENERAL_ERROR, e.getMessage());
        }
    }

    /**
     * 运管端获取通话channel中的所有人
     * @param uid
     * @param channelName
     * @param callBack
     */
    public void getAgoraUidWithAdmin(int uid, String channelName, EaseGetUserAccountCallback callBack){
        OkHttpClient client = new OkHttpClient();

        Headers headers = new Headers.Builder()
                .add("Authorization", EaseIMHelper.getInstance().getModel().getAppToken())
                .add("username", EaseIMHelper.getInstance().getCurrentUser())
                .build();
        Request request = new Request.Builder()
                .url(EaseIMHelper.getInstance().getServerHost() + "v2/rtc/channle/" + channelName + "/show")
                .headers(headers)
                .get()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callBack.onSetUserAccountError(EMError.GENERAL_ERROR, e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String responseBody = response.body().string();
                if (response.code() == 200 && !TextUtils.isEmpty(responseBody)) {
                    try {
                        JSONObject result = new JSONObject(responseBody);
                        String status = result.optString("status");
                        if(TextUtils.equals("OK", status) || TextUtils.equals("SUCCEED", status)){
                            JSONObject entity = result.getJSONObject("entity");
                            JSONObject rtcChannels = entity.getJSONObject("rtcChannels");
                            List<EaseUserAccount> userAccounts = new ArrayList<>();
                            for (Iterator<String> it = rtcChannels.keys(); it.hasNext(); ) {
                                String name = it.next();
                                String id = rtcChannels.optString(name);
                                if(TextUtils.equals(String.valueOf(uid), id)){
                                    EaseIMHelper.getInstance().setEaseCallKitUserInfo(name);
                                }
                                userAccounts.add(new EaseUserAccount(Integer.parseInt(id), name));
                            }
                            EaseThreadManager.getInstance().runOnMainThread(() -> callBack.onUserAccount(userAccounts));
                        } else {
                            callBack.onSetUserAccountError(EMError.GENERAL_ERROR, "fetch agora uid failed");
                        }

                    } catch (JSONException e) {
                        callBack.onSetUserAccountError(EMError.GENERAL_ERROR, e.getMessage());
                    }
                } else {
                    callBack.onSetUserAccountError(EMError.GENERAL_ERROR, "fetch agora uid failed");
                }
            }
        });
    }
}
