package com.hyphenate.easeim.common.repositories;

import android.text.TextUtils;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMError;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMCursorResult;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeim.common.model.GroupApplyBean;
import com.hyphenate.easeim.common.model.SearchResult;
import com.hyphenate.easeim.common.net.ErrorCode;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.manager.EaseThreadManager;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.exceptions.HyphenateException;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

public class EMGroupManagerRepository extends BaseEMRepository{

    private static EMGroupManagerRepository instance;

    public static EMGroupManagerRepository getInstance() {
        if(instance == null) {
            synchronized (EMGroupManagerRepository.class) {
                if(instance == null) {
                    instance = new EMGroupManagerRepository();
                }
            }
        }
        return instance;
    }

    /**
     * 获取所有群组列表
     * @param callBack
     */
    public void getAllGroups(ResultCallBack<List<EMGroup>> callBack) {
        if(!isLoggedIn()) {
            callBack.onError(ErrorCode.EM_NOT_LOGIN);
            return;
        }
        getGroupManager().asyncGetJoinedGroupsFromServer(new EMValueCallBack<List<EMGroup>>() {
            @Override
            public void onSuccess(List<EMGroup> value) {
                if(callBack != null) {
                    callBack.onSuccess(value);
                }
            }

            @Override
            public void onError(int error, String errorMsg) {
                if(callBack != null) {
                    callBack.onError(error, errorMsg);
                }
            }
        });
    }

    /**
     * 获取群组成员列表(包含管理员和群主)
     * @param groupId
     * @return
     */
    public void getGroupAllMembers(String groupId, ResultCallBack<List<EaseUser>> callBack) {
                EaseIMHelper.getInstance().getGroupManager().asyncGetGroupFromServer(groupId, new EMValueCallBack<EMGroup>() {
                    @Override
                    public void onSuccess(EMGroup value) {
                        List<String> members = value.getMembers();
                        if(members.size() < (value.getMemberCount() - value.getAdminList().size() - 1)) {
                            members = getAllGroupMemberByServer(groupId);
                        }
                        members.addAll(value.getAdminList());
                        members.add(value.getOwner());
                        if(!members.isEmpty()) {
                            List<EaseUser> users = EaseUser.parse(members);
                            sortUserData(users);
                            for(EaseUser item : users){
                                if(TextUtils.equals(value.getOwner(), item.getUsername())){
                                    item.setOwner(true);
                                }
                            }
                            callBack.onSuccess(users);
                        }else {
                            callBack.onError(ErrorCode.EM_ERR_GROUP_NO_MEMBERS);
                        }
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        callBack.onError(error, errorMsg);
                    }
                });
    }

    /**
     * 获取禁言列表
     * @param groupId
     * @return
     */
    public void getGroupMuteMap(String groupId, ResultCallBack<Map<String, Long>> callBack) {
                EaseThreadManager.getInstance().runOnIOThread(() -> {
                    Map<String, Long> map = null;
                    Map<String, Long> result = new HashMap<>();
                    int pageSize = 200;
                    do{
                        try {
                            map = getGroupManager().fetchGroupMuteList(groupId, 0, pageSize);
                        } catch (HyphenateException e) {
                            e.printStackTrace();
                            callBack.onError(e.getErrorCode(), e.getMessage());
                            break;
                        }
                        if(map != null) {
                            result.putAll(map);
                        }
                    }while (map != null && map.size() >= 200);
                    callBack.onSuccess(result);
                });
    }

    /**
     * 获取群公告
     * @param groupId
     * @return
     */
    public void getGroupAnnouncement(String groupId, ResultCallBack<String> callBack) {
                getGroupManager().asyncFetchGroupAnnouncement(groupId, new EMValueCallBack<String>() {
                    @Override
                    public void onSuccess(String value) {
                        callBack.onSuccess(value);
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        callBack.onError(error, errorMsg);
                    }
                });
    }

    /**
     * 获取所有成员
     * @param groupId
     * @return
     */
    public List<String> getAllGroupMemberByServer(String groupId) {
        // 根据groupId获取群组中所有成员
        List<String> contactList = new ArrayList<>();
        EMCursorResult<String> result = null;
        do {
            try {
                result = getGroupManager().fetchGroupMembers(groupId, result != null ? result.getCursor() : "", 20);
            } catch (HyphenateException e) {
                e.printStackTrace();
            }
            if(result != null) {
                contactList.addAll(result.getData());
            }
        } while (result != null && !TextUtils.isEmpty(result.getCursor()));
        return contactList;
    }

    private void sortUserData(List<EaseUser> users) {
        Collections.sort(users, new Comparator<EaseUser>() {

            @Override
            public int compare(EaseUser lhs, EaseUser rhs) {
                if(lhs.getInitialLetter().equals(rhs.getInitialLetter())){
                    return lhs.getNickname().compareTo(rhs.getNickname());
                }else{
                    if("#".equals(lhs.getInitialLetter())){
                        return 1;
                    }else if("#".equals(rhs.getInitialLetter())){
                        return -1;
                    }
                    return lhs.getInitialLetter().compareTo(rhs.getInitialLetter());
                }

            }
        });
    }

    /**
     * 设置群组名称
     * @param groupId
     * @param groupName
     * @return
     */
    public void setGroupName(String groupId, String groupName, ResultCallBack<String> callBack) {
                try{
                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    json.put("groupName", groupName);

                    RequestBody body = RequestBody.create(JSON, json.toString());

                    Headers headers = new Headers.Builder()
                            .add("Authorization", EaseIMHelper.getInstance().getModel().getAppToken())
                            .add("username", EaseIMHelper.getInstance().getCurrentUser())
                            .build();
                    Request request = new Request.Builder()
                            .url(EaseIMHelper.getInstance().getServerHost()+"/v4/users/"+EaseIMHelper.getInstance().getCurrentUser()+"/group/"+groupId+"/modGroup")
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
                                    if (TextUtils.equals("OK", status) || TextUtils.equals("SUCCEED", status)) {
                                        callBack.onSuccess(groupName);
                                    } else {
                                        callBack.onError(EMError.GENERAL_ERROR, "modify groupName failed");
                                    }
                                }catch (JSONException e){
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "modify groupName failed");
                            }
                        }
                    });
                }catch(JSONException e){
                    e.printStackTrace();
                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                }
    }

    /**
     * 设置群公告
     * @param groupId
     * @param announcement
     * @return
     */
    public void setGroupAnnouncement(String groupId, String announcement, ResultCallBack<String> callBack) {
                getGroupManager().asyncUpdateGroupAnnouncement(groupId, announcement, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        callBack.onSuccess(announcement);
                    }

                    @Override
                    public void onError(int code, String error) {
                        callBack.onError(code, error);
                    }

                    @Override
                    public void onProgress(int progress, String status) {

                    }
                });
    }

    /**
     * 设置群描述
     * @param groupId
     * @param description
     * @return
     */
    public void setGroupDescription(String groupId, String description, ResultCallBack<String> callBack) {
                getGroupManager().asyncChangeGroupDescription(groupId, description, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        EMMessage message = EMMessage.createSendMessage(EMMessage.Type.CMD);
                        EMCmdMessageBody body = new EMCmdMessageBody("event");
                        body.deliverOnlineOnly(true);
                        message.addBody(body);
                        message.setTo(groupId);
                        message.setChatType(EMMessage.ChatType.GroupChat);
                        message.setAttribute(EaseConstant.MESSAGE_ATTR_EVENT_TYPE, EaseConstant.EVENT_TYPE_GROUP_INTRO);
                        message.setAttribute(EaseConstant.MESSAGE_ATTR_GROUP_ID, groupId);
                        message.setAttribute(EaseConstant.MESSAGE_ATTR_GROUP_INTRO, description);
                        EMClient.getInstance().chatManager().sendMessage(message);
                        callBack.onSuccess(description);
                    }

                    @Override
                    public void onError(int code, String error) {
                        callBack.onError(code, error);
                    }

                    @Override
                    public void onProgress(int progress, String status) {

                    }
                });
    }

    /**
     * 运管端邀请群成员
     */
    public void addMembersWithAdmin(String groupId, List<String> customerList, List<String> waiterList, EMCallBack callBack) {
                try{
                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    JSONArray customers = new JSONArray();
                    for(String customer : customerList){
                        customers.put(customer);
                    }
                    json.put("customerAids", customers);

                    JSONArray waiters = new JSONArray();
                    for(String waiter : waiterList){
                        waiters.put(waiter);
                    }
                    json.put("waiterAids", waiters);

                    RequestBody body = RequestBody.create(JSON, json.toString());

                    Headers headers = new Headers.Builder()
                            .add("Authorization", EaseIMHelper.getInstance().getModel().getAppToken())
                            .add("username", EaseIMHelper.getInstance().getCurrentUser())
                            .build();
                    Request request = new Request.Builder()
                            .url(EaseIMHelper.getInstance().getServerHost()+"/v4/users/" + EaseIMHelper.getInstance().getCurrentUser() + "/group/"+groupId+"/addUsers/inviter/"+EaseIMHelper.getInstance().getCurrentUser())
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
                                        callBack.onSuccess();
                                    } else {
                                        callBack.onError(EMError.GENERAL_ERROR, "add member failed");
                                    }
                                } catch (JSONException e) {
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "add member failed");
                            }
                        }
                    });
                }catch(JSONException e){
                    e.printStackTrace();
                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                }
    }

    /**
     * 极狐app邀请群成员
     */
    public void addMembersWithCustomer(String groupId, List<String> customerList, List<String> waiterList, EMCallBack callBack) {
                try{
                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    JSONArray customers = new JSONArray();
                    for(String customer : customerList){
                        customers.put(customer);
                    }
                    json.put("customerAids", customers);

                    JSONArray waiters = new JSONArray();
                    for(String waiter : waiterList){
                        waiters.put(waiter);
                    }
                    json.put("waiterAids", waiters);

                    RequestBody body = RequestBody.create(JSON, json.toString());

                    Headers headers = new Headers.Builder()
                            .add("Authorization", EMClient.getInstance().getAccessToken())
                            .add("username", EaseIMHelper.getInstance().getCurrentUser())
                            .build();
                    Request request = new Request.Builder()
                            .url(EaseIMHelper.getInstance().getServerHost()+"/v4/users/" + EaseIMHelper.getInstance().getCurrentUser()
                                    + "/group/"+groupId+"/addUsers/inviter/"+EaseIMHelper.getInstance().getCurrentUser() + "/APP")
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
                                        callBack.onSuccess();
                                    } else {
                                        callBack.onError(EMError.GENERAL_ERROR, "add member failed");
                                    }
                                } catch (JSONException e) {
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "add member failed");
                            }
                        }
                    });
                }catch(JSONException e){
                    e.printStackTrace();
                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                }
    }

    /**
     * 获取群组申请
     * @param page
     * @param size
     * @return
     */
    public void fetchGroupApply(int page, int size, ResultCallBack<List<GroupApplyBean>> callBack){
                try{
                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    json.put("username", EaseIMHelper.getInstance().getCurrentUser());
                    json.put("page", page);
                    json.put("size", size);
                    RequestBody body = RequestBody.create(JSON, json.toString());

                    Headers headers = new Headers.Builder()
                            .add("Authorization", EaseIMHelper.getInstance().getModel().getAppToken())
                            .add("username", EaseIMHelper.getInstance().getCurrentUser())
                            .build();
                    Request request = new Request.Builder()
                            .url(EaseIMHelper.getInstance().getServerHost()+"/v2/group/chatgroups/users/state")
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
                                    List<GroupApplyBean> list = new ArrayList<>();
                                    JSONObject result = new JSONObject(responseBody);
                                    String status = result.optString("status");
                                    if(TextUtils.equals("OK", status) || TextUtils.equals("SUCCEED", status)){
                                        JSONObject entity = result.getJSONObject("entity");
                                        JSONArray data = entity.getJSONArray("data");
                                        if(data.length() > 0){
                                            for(int i = 0; i < data.length(); i ++){
                                                JSONObject item = data.getJSONObject(i);
                                                GroupApplyBean bean = new GroupApplyBean();
                                                bean.setUserName(item.optString("userName"));
                                                bean.setGroupId(item.optString("groupId"));
                                                bean.setGroupName(item.optString("groupName"));
                                                if(TextUtils.equals(item.optString("state"), "wait")){
                                                    bean.setOperated(false);
                                                } else {
                                                    bean.setOperated(true);
                                                    bean.setOperatedResult(item.optString("state"));
                                                }
                                                bean.setRole(item.optString("role"));
                                                bean.setInviterName(item.optString("inviter"));
                                                list.add(bean);
                                            }
                                        }

                                        callBack.onSuccess(list);
                                    } else {
                                        callBack.onError(EMError.GENERAL_ERROR, "get group apply failed");
                                    }
                                } catch (JSONException e) {
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "get group apply failed");
                            }
                        }
                    });
                }catch(JSONException e){
                    e.printStackTrace();
                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                }
    }

    /**
     * 处理群组申请
     * @param bean
     * @param state
     * @return
     */
    public void operationGroupApply(GroupApplyBean bean, String state, ResultCallBack<GroupApplyBean> callBack){
                try{
                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    json.put("groupId", bean.getGroupId());
                    json.put("username", bean.getUserName());
                    json.put("role", bean.getRole());
                    json.put("option", state);
                    RequestBody body = RequestBody.create(JSON, json.toString());

                    Headers headers = new Headers.Builder()
                            .add("Authorization", EaseIMHelper.getInstance().getModel().getAppToken())
                            .add("username", EaseIMHelper.getInstance().getCurrentUser())
                            .build();
                    Request request = new Request.Builder()
                            .url(EaseIMHelper.getInstance().getServerHost()+"/v2/group/chatgroups/users/"+EaseIMHelper.getInstance().getCurrentUser()+"/state")
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
                                        bean.setOperated(true);
                                        bean.setOperatedResult(state);
                                        callBack.onSuccess(bean);
                                    } else {
                                        callBack.onError(EMError.GENERAL_ERROR, "operation group apply failed");
                                    }
                                } catch (JSONException e) {
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "operation group apply failed");
                            }
                        }
                    });
                }catch(JSONException e){
                    e.printStackTrace();
                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                }
    }

    /**
     * 禁言
     * @param groupId
     * @param usernames
     * @return
     */
    public void muteGroupMembers(String groupId, List<String> usernames, long duration, ResultCallBack<String> callBack) {
                getGroupManager().aysncMuteGroupMembers(groupId, usernames, duration, new EMValueCallBack<EMGroup>() {
                    @Override
                    public void onSuccess(EMGroup value) {
                        callBack.onSuccess(getContext().getString(R.string.demo_group_member_mute, usernames.get(0)));
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        callBack.onError(error, errorMsg);
                    }
                });
    }

    /**
     * 解除禁言
     * @param groupId
     * @param usernames
     * @return
     */
    public void unMuteGroupMembers(String groupId, List<String> usernames, ResultCallBack<String> callBack) {
                getGroupManager().asyncUnMuteGroupMembers(groupId, usernames, new EMValueCallBack<EMGroup>() {
                    @Override
                    public void onSuccess(EMGroup value) {
                        callBack.onSuccess(getContext().getString(R.string.demo_group_member_remove_mute, usernames.get(0)));
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        callBack.onError(error, errorMsg);
                    }
                });
    }

    /**
     * 创建群组
     */
    public void createGroup(String groupName, String desc, List<String> customerList, List<String> waiterList, ResultCallBack<String> callBack) {

                try{
                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    json.put("groupName", groupName);
                    json.put("desc", desc);
                    json.put("ownerAid", EaseIMHelper.getInstance().getCurrentUser());
                    JSONArray customers = new JSONArray();
                    for(String customer : customerList){
                        customers.put(customer);
                    }
                    json.put("customerAids", customers);
                    JSONArray waiters = new JSONArray();
                    for(String waiter : waiterList){
                        waiters.put(waiter);
                    }
                    json.put("waiterAids", waiters);
                    json.put("groupType", "MANUAL");
                    json.put("action", true);
                    RequestBody body = RequestBody.create(JSON, json.toString());

                    Headers headers = new Headers.Builder()
                            .add("Authorization", EaseIMHelper.getInstance().getModel().getAppToken())
                            .add("username", EaseIMHelper.getInstance().getCurrentUser())
                            .build();
                    Request request = new Request.Builder()
                            .url(EaseIMHelper.getInstance().getServerHost()+"/v4/users/" + EaseIMHelper.getInstance().getCurrentUser() + "/group/createGroup")
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
                                        JSONObject data = result.getJSONObject("data");
                                        String groupId = data.getString("groupId");
                                        callBack.onSuccess(groupId);
                                    }else {
                                        callBack.onError(EMError.GENERAL_ERROR, "create group failed");
                                    }
                                } catch (JSONException e) {
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "create group failed");
                            }
                        }
                    });
                }catch(JSONException e){
                    e.printStackTrace();
                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                }
    }

    /**
     * 获取服务备注
     */
    public void getServiceNote(String groupId, ResultCallBack<List<String>> callBack){
                    OkHttpClient client = new OkHttpClient();
                    Headers headers = new Headers.Builder()
                            .add("Authorization", EaseIMHelper.getInstance().getModel().getAppToken())
                            .add("username", EaseIMHelper.getInstance().getCurrentUser())
                            .build();
                    Request request = new Request.Builder()
                            .url(EaseIMHelper.getInstance().getServerHost()+"/v4/users/" + EaseIMHelper.getInstance().getCurrentUser() + "/group/"+groupId+"/getGroupInfo")
                            .headers(headers)
                            .get()
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
                                        JSONObject entity = result.getJSONObject("data");
                                        String businessRemark = entity.optString("businessRemark");
                                        String sysDesc = entity.optString("sysDesc");
                                        List<String> list = new ArrayList<>();
                                        list.add(sysDesc);
                                        list.add(businessRemark);
                                        callBack.onSuccess(list);
                                    } else {
                                        callBack.onError(EMError.GENERAL_ERROR, "get service note failed");
                                    }
                                } catch (JSONException e) {
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "get service note failed");
                            }
                        }
                    });
    }

    /**
     * 编辑服务备注
     */
    public void changeServiceNote(String groupId, String note, ResultCallBack callBack){
                try{
                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    json.put("businessRemark", note);
                    RequestBody body = RequestBody.create(JSON, json.toString());

                    Headers headers = new Headers.Builder()
                            .add("Authorization", EaseIMHelper.getInstance().getModel().getAppToken())
                            .add("username", EaseIMHelper.getInstance().getCurrentUser())
                            .build();
                    Request request = new Request.Builder()
                            .url(EaseIMHelper.getInstance().getServerHost()+"/v4/users/"+EaseIMHelper.getInstance().getCurrentUser()+"/group/"+groupId+"/modGroup")
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
                                    if (TextUtils.equals("OK", status) || TextUtils.equals("SUCCEED", status)) {
                                        callBack.onSuccess(note);
                                    } else {
                                        callBack.onError(EMError.GENERAL_ERROR, "modify service note failed");
                                    }
                                }catch (JSONException e){
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "modify service note failed");
                            }
                        }
                    });
                }catch(JSONException e){
                    e.printStackTrace();
                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                }
    }

    /**
     * 查询群列表
     * @return
     */
    public void searchGroupChat(String aid, String mobile, String orderId, String vin, String groupName, String source, ResultCallBack<List<SearchResult>> callBack){
                try{
                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    json.put("aid", aid);
                    json.put("mobile", mobile);
                    json.put("orderId", orderId);
                    json.put("vin", vin);
                    json.put("groupName", groupName);
                    json.put("source", source);
                    RequestBody body = RequestBody.create(JSON, json.toString());

                    Headers headers = new Headers.Builder()
                            .add("Authorization", EaseIMHelper.getInstance().getModel().getAppToken())
                            .add("username", EaseIMHelper.getInstance().getCurrentUser())
                            .build();
                    Request request = new Request.Builder()
                            .url(EaseIMHelper.getInstance().getServerHost()+"/v4/users/"+ EaseIMHelper.getInstance().getCurrentUser() + "/group/listGroup")
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
                                try{
                                    JSONObject result = new JSONObject(responseBody);
                                    String status = result.optString("status");
                                    if(TextUtils.equals("OK", status) || TextUtils.equals("SUCCEED", status)){
                                        JSONArray data = result.getJSONArray("data");
                                        List<SearchResult> list = new ArrayList<>();
                                        if(data.length() > 0){
                                            for(int i = 0; i < data.length(); i++){
                                                JSONObject item = data.getJSONObject(i);
                                                SearchResult searchResult = new SearchResult();
                                                searchResult.setName(item.optString("groupName"));
                                                searchResult.setId(item.optString("groupId"));
                                                list.add(searchResult);
                                            }
                                        }
                                        callBack.onSuccess(list);
                                    } else {
                                        callBack.onError(EMError.GENERAL_ERROR, "search group list failed");
                                    }
                                }catch (JSONException e){
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "search group list failed");
                            }
                        }
                    });
                }catch(JSONException e){
                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                }
    }

    /**
     * 极狐app搜索用户
     * @param keyword
     * @return
     */
    public void searchUserWithCustomer(String keyword, ResultCallBack<List<String>> callBack) {
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
                    .url(EaseIMHelper.getInstance().getServerHost()+"/v1/gov/arcfox/user/"+EaseIMHelper.getInstance().getCurrentUser())
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
                                List<String> list = new ArrayList<>();
                                if(entities.length() > 0){
                                    JSONObject item = entities.getJSONObject(0);
                                    list.add(item.optString("userName"));
                                }
                                callBack.onSuccess(list);
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

    /**
     * 运管端搜索用户
     * @param keyword
     * @return
     */
    public void searchUserWithAdmin(String keyword, ResultCallBack<List<String>> callBack) {
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
                    .url(EaseIMHelper.getInstance().getServerHost()+"/v2/gov/arcfox/user/"+EaseIMHelper.getInstance().getCurrentUser())
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
                                List<String> list = new ArrayList<>();
                                if(entities.length() > 0){
                                    JSONObject item = entities.getJSONObject(0);
                                    list.add(item.optString("userName"));
                                }
                                callBack.onSuccess(list);
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
}
