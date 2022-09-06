package com.hyphenate.easeim.common.repositories;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMError;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMUserInfo;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.common.db.entity.EmUserEntity;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeim.common.model.EaseModel;
import com.hyphenate.easeim.common.net.ErrorCode;
import com.hyphenate.easeim.common.net.Resource;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.manager.EaseThreadManager;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.EMLog;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EMContactManagerRepository extends BaseEMRepository{

    /**
     * 极狐app搜索用户
     * @param keyword
     * @return
     */
    public LiveData<Resource<List<EaseUser>>> searchUserWithCustomer(String keyword) {
        return new NetworkOnlyResource<List<EaseUser>>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<List<EaseUser>>> callBack) {
                try{
                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    json.put("username", keyword);
                    RequestBody body = RequestBody.create(JSON, json.toString());

                    Headers headers = new Headers.Builder()
                            .add("Authorization", EMClient.getInstance().getAccessToken())
                            .add("username", EaseIMHelper.getInstance().getCurrentUser())
                            .build();
                    Request request = new Request.Builder()
                            .url(EaseIMHelper.getInstance().getServerHost()+"v1/gov/arcfox/user/"+EaseIMHelper.getInstance().getCurrentUser())
                            .headers(headers)
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
                                        JSONArray entities = result.getJSONArray("entities");
                                        List<EaseUser> list = new ArrayList<>();
                                        if(entities.length() > 0){
                                            JSONObject item = entities.getJSONObject(0);
                                            EaseUser user = new EaseUser();
                                            user.setUsername(item.optString("userName"));
                                            if(!TextUtils.equals("null", item.optString("nickName"))){
                                                user.setNickname(item.optString("nickName"));
                                            }
                                            user.setAvatar(item.optString("avatar"));
                                            list.add(user);
                                        }
                                        callBack.onSuccess(createLiveData(list));
                                    } else {
                                        callBack.onError(EMError.GENERAL_ERROR, "search user failed");
                                    }
                                } catch (JSONException e) {
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "search user failed");
                            }
                        }
                    });
                }catch(JSONException e){
                    e.printStackTrace();
                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                }
            }
        }.asLiveData();
    }

    /**
     * 运管端搜索用户
     * @param keyword
     * @return
     */
    public LiveData<Resource<List<EaseUser>>> searchUserWithAdmin(String keyword) {
        return new NetworkOnlyResource<List<EaseUser>>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<List<EaseUser>>> callBack) {
                try{
                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    json.put("username", keyword);
                    RequestBody body = RequestBody.create(JSON, json.toString());

                    Headers headers = new Headers.Builder()
                            .add("Authorization", EaseIMHelper.getInstance().getModel().getAppToken())
                            .add("username", EaseIMHelper.getInstance().getCurrentUser())
                            .build();
                    Request request = new Request.Builder()
                            .url(EaseIMHelper.getInstance().getServerHost()+"v2/gov/arcfox/user/"+EaseIMHelper.getInstance().getCurrentUser())
                            .headers(headers)
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
                                        JSONArray entities = result.getJSONArray("entities");
                                        List<EaseUser> list = new ArrayList<>();
                                        if(entities.length() > 0){
                                            JSONObject item = entities.getJSONObject(0);
                                            EaseUser user = new EaseUser();
                                            user.setUsername(item.optString("userName"));
                                            if(!TextUtils.equals("null", item.optString("nickName"))){
                                                user.setNickname(item.optString("nickName"));
                                            }
                                            user.setAvatar(item.optString("avatar"));
                                            list.add(user);
                                        }
                                        callBack.onSuccess(createLiveData(list));
                                    } else {
                                        callBack.onError(EMError.GENERAL_ERROR, "search user failed");
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "search user failed");
                            }
                        }
                    });
                }catch(JSONException e){
                    e.printStackTrace();
                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                }
            }
        }.asLiveData();
    }
}
