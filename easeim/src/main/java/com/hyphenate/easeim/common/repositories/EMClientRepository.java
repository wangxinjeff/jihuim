package com.hyphenate.easeim.common.repositories;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMError;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMUserInfo;
import com.hyphenate.easecallkit.base.EaseCallKitTokenCallback;
import com.hyphenate.easecallkit.base.EaseGetUserAccountCallback;
import com.hyphenate.easecallkit.base.EaseUserAccount;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.common.db.EaseDbHelper;
import com.hyphenate.easeim.common.db.entity.EmUserEntity;
import com.hyphenate.easeim.common.livedatas.LiveDataBus;
import com.hyphenate.easeim.common.net.ErrorCode;
import com.hyphenate.easeim.common.net.Resource;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeim.common.utils.PreferenceManager;
import com.hyphenate.easeui.EaseIM;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.manager.EaseThreadManager;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.util.EMLog;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public void fetchSelfInfo(){
        String[] userId = new String[1];
        userId[0] = EMClient.getInstance().getCurrentUser();
        EMUserInfo.EMUserInfoType[] userInfoTypes = new EMUserInfo.EMUserInfoType[2];
        userInfoTypes[0] = EMUserInfo.EMUserInfoType.NICKNAME;
        userInfoTypes[1] = EMUserInfo.EMUserInfoType.AVATAR_URL;
        EMClient.getInstance().userInfoManager().fetchUserInfoByAttribute(userId, userInfoTypes,new EMValueCallBack<Map<String, EMUserInfo>>() {
            @Override
            public void onSuccess(Map<String, EMUserInfo> userInfos) {
                runOnMainThread(new Runnable() {
                    public void run() {
                        EMUserInfo userInfo = userInfos.get(EMClient.getInstance().getCurrentUser());
                        //昵称
                        if(userInfo != null && userInfo.getNickName() != null &&
                                userInfo.getNickName().length() > 0){
                            EaseEvent event = EaseEvent.create(EaseConstant.NICK_NAME_CHANGE, EaseEvent.TYPE.CONTACT);
                            event.message = userInfo.getNickname();
                            LiveDataBus.get().with(EaseConstant.NICK_NAME_CHANGE).postValue(event);
                            PreferenceManager.getInstance().setCurrentUserNick(userInfo.getNickname());
                        }
                        //头像
                        if(userInfo != null && userInfo.getAvatarUrl() != null && userInfo.getAvatarUrl().length() > 0){

                            EaseEvent event = EaseEvent.create(EaseConstant.AVATAR_CHANGE, EaseEvent.TYPE.CONTACT);
                            event.message = userInfo.getAvatarUrl();
                            LiveDataBus.get().with(EaseConstant.AVATAR_CHANGE).postValue(event);
                            PreferenceManager.getInstance().setCurrentUserAvatar(userInfo.getAvatarUrl());
                        }
                    }
                });
            }

            @Override
            public void onError(int error, String errorMsg) {
                EMLog.e(TAG,"fetchUserInfoByIds error:" + error + " errorMsg:" + errorMsg);
            }
        });
    }

    /**
     * 登录过后需要加载的数据
     * @return
     */
    public LiveData<Resource<Boolean>> loadAllInfoFromHX() {
        return new NetworkOnlyResource<Boolean>() {

            @Override
            protected void createCall(ResultCallBack<LiveData<Boolean>> callBack) {
                if(isAutoLogin()) {
                    runOnIOThread(() -> {
                        if(isLoggedIn()) {
                            loadAllConversationsAndGroups();
                            callBack.onSuccess(createLiveData(true));
                        }else {
                            callBack.onError(ErrorCode.EM_NOT_LOGIN);
                        }

                    });
                }else {
                    callBack.onError(ErrorCode.EM_NOT_LOGIN);
                }

            }
        }.asLiveData();
    }

    /**
     * 从本地数据库加载所有的对话及群组
     */
    private void loadAllConversationsAndGroups() {
        // 初始化数据库
        initDb();
        // 从本地数据库加载所有的对话及群组
        getChatManager().loadAllConversations();
        getGroupManager().loadAllGroups();
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
     * 极狐APP登录
     * @param username
     * @param password
     * @param callBack
     */
    public void loginWithCustomer(String username, String password, EMCallBack callBack){
        loginToServer(username, password, new EMCallBack() {
            @Override
            public void onSuccess() {
                getServiceGroups(callBack);
            }

            @Override
            public void onError(int i, String s) {
                callBack.onError(i, s);
            }
        });
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
                setAutoLogin(true);
                new EMChatManagerRepository().fetchConversationsFromServer();
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
                EMLog.e("EaseIM", "logout success");
                EaseIMHelper.getInstance().logoutSuccess();
                callBack.onSuccess();
            }

            @Override
            public void onError(int i, String s) {
                EMClient.getInstance().logout(false, new EMCallBack() {
                    @Override
                    public void onSuccess() {
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

    private void successForCallBack(@NonNull ResultCallBack<LiveData<EaseUser>> callBack) {
        // ** manually load all local groups and conversation
        loadAllConversationsAndGroups();
        //从服务器拉取加入的群，防止进入会话页面只显示id
        getAllJoinGroup();
        // get contacts from server
        getContactsFromServer();
        // get current user id
        String currentUser = EMClient.getInstance().getCurrentUser();
        EaseUser user = new EaseUser(currentUser);
        callBack.onSuccess(new MutableLiveData<>(user));
    }

    public void loginSuccess(){
        // ** manually load all local groups and conversation
        loadAllConversationsAndGroups();
        //从服务器拉取加入的群，防止进入会话页面只显示id
        getAllJoinGroup();
    }

    private void getContactsFromServer() {
        new EMContactManagerRepository().getContactList(new ResultCallBack<List<EaseUser>>() {
            @Override
            public void onSuccess(List<EaseUser> value) {
                if(getUserDao() != null) {
                    getUserDao().clearUsers();
                    getUserDao().insert(EmUserEntity.parseList(value));
                }
            }

            @Override
            public void onError(int error, String errorMsg) {

            }
        });
    }

    private void getAllJoinGroup() {
        new EMGroupManagerRepository().getAllGroups(new ResultCallBack<List<EMGroup>>() {
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
     * 极狐app获取专属服务群列表
     * @param callBack
     */
    public void getServiceGroups(EMCallBack callBack){
        OkHttpClient client = new OkHttpClient();
        Headers headers = new Headers.Builder()
                .add("Authorization", EMClient.getInstance().getAccessToken())
                .add("username", EaseIMHelper.getInstance().getCurrentUser())
                .build();
        Request request = new Request.Builder()
                .url(EaseIMHelper.getInstance().getServerHost()+"v2/group/chatgroups/users/"+ EaseIMHelper.getInstance().getCurrentUser() + "/action")
                .headers(headers)
                .get()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callBack.onSuccess();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String responseBody = response.body().string();
                if(response.code() == 200 && !TextUtils.isEmpty(responseBody)){
                    try {
                        JSONObject result = new JSONObject(responseBody);
                        String status = result.optString("status");
                        if(TextUtils.equals("OK", status) || TextUtils.equals("SUCCEED", status)){
                            JSONArray entity = result.getJSONArray("entity");
                            if(entity.length() > 0){
                                JSONObject json = new JSONObject();
                                for(int i = 0; i < entity.length(); i ++){
                                    JSONObject item = entity.getJSONObject(i);
                                    String groupId = item.optString("groupId");
                                    String groupName = item.optString("groupName");
                                    json.put(groupId, groupName);
                                }
                                EaseIMHelper.getInstance().getModel().setServiceGroup(json.toString());
                            }
                            callBack.onSuccess();
                        } else {
                            callBack.onError(EMError.GENERAL_ERROR, "fetch service groups failed");
                        }
                    } catch (JSONException e) {
                        callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                    }
                } else {
                    callBack.onError(EMError.GENERAL_ERROR, "fetch service groups failed");
                }
            }
        });
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

    /**
     * 极狐app端获取通话rtc token
     * @param userName
     * @param channelName
     * @param callBack
     */
    public void getRtcTokenWithCustomer(String userName, String channelName, EaseCallKitTokenCallback callBack){
        try {
            OkHttpClient client = new OkHttpClient();
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            JSONObject json = new JSONObject();
            json.put("username", userName);
            json.put("channelName", channelName);

            RequestBody body = RequestBody.create(JSON, json.toString());
            Headers headers = new Headers.Builder()
                    .add("Authorization", EMClient.getInstance().getAccessToken())
                    .add("username", EaseIMHelper.getInstance().getCurrentUser())
                    .build();
            Request request = new Request.Builder()
                    .url(EaseIMHelper.getInstance().getServerHost() + "v1/rtc/token")
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
     * 极狐app端获取通话channel中的所有人
     * @param uid
     * @param channelName
     * @param callBack
     */
    public void getAgoraUidWithCustomer(int uid, String channelName, EaseGetUserAccountCallback callBack){
        OkHttpClient client = new OkHttpClient();

        Headers headers = new Headers.Builder()
                .add("Authorization", EMClient.getInstance().getAccessToken())
                .add("username", EaseIMHelper.getInstance().getCurrentUser())
                .build();
        Request request = new Request.Builder()
                .url(EaseIMHelper.getInstance().getServerHost() + "v1/rtc/channle/" + channelName + "/show")
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
                    EMLog.e("getUid:", responseBody);
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
                                EMLog.e("getUid:", "uid = " + id + ", name = " + name);
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
