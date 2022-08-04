package com.hyphenate.easeim.common.repositories;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMError;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCursorResult;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMGroupInfo;
import com.hyphenate.chat.EMGroupOptions;
import com.hyphenate.chat.EMMucSharedFile;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.db.entity.EmUserEntity;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeim.common.model.GroupApplyBean;
import com.hyphenate.easeim.common.model.SearchResult;
import com.hyphenate.easeim.common.net.ErrorCode;
import com.hyphenate.easeim.common.net.Resource;
import com.hyphenate.easeui.manager.EaseThreadManager;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.exceptions.HyphenateException;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
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

    /**
     * 获取所有的群组列表
     * @return
     */
    public LiveData<Resource<List<EMGroup>>> getAllGroups() {
        return new NetworkBoundResource<List<EMGroup>, List<EMGroup>>() {
            @Override
            protected boolean shouldFetch(List<EMGroup> data) {
                return true;
            }

            @Override
            protected LiveData<List<EMGroup>> loadFromDb() {
                List<EMGroup> allGroups = getGroupManager().getAllGroups();
                return new MutableLiveData<>(allGroups);
            }

            @Override
            protected void createCall(ResultCallBack<LiveData<List<EMGroup>>> callBack) {
                if(!isLoggedIn()) {
                    callBack.onError(ErrorCode.EM_NOT_LOGIN);
                    return;
                }
                getGroupManager().asyncGetJoinedGroupsFromServer(new EMValueCallBack<List<EMGroup>>() {
                    @Override
                    public void onSuccess(List<EMGroup> value) {
                        callBack.onSuccess(new MutableLiveData<>(value));
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        callBack.onError(error, errorMsg);
                    }
                });
            }

            @Override
            protected void saveCallResult(List<EMGroup> item) {

            }

        }.asLiveData();
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
     * 从服务器分页获取加入的群组
     * @param pageIndex
     * @param pageSize
     * @return
     */
    public LiveData<Resource<List<EMGroup>>> getGroupListFromServer(int pageIndex, int pageSize) {
        return new NetworkOnlyResource<List<EMGroup>>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<List<EMGroup>>> callBack) {
                getGroupManager().asyncGetJoinedGroupsFromServer(pageIndex, pageSize, new EMValueCallBack<List<EMGroup>>() {
                    @Override
                    public void onSuccess(List<EMGroup> value) {
                        callBack.onSuccess(createLiveData(value));
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        callBack.onError(error, errorMsg);
                    }
                });
            }
        }.asLiveData();
    }

    /**
     * 获取公开群
     * @param pageSize
     * @param cursor
     * @return
     */
    public LiveData<Resource<EMCursorResult<EMGroupInfo>>> getPublicGroupFromServer(int pageSize, String cursor) {
        return new NetworkOnlyResource<EMCursorResult<EMGroupInfo>>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<EMCursorResult<EMGroupInfo>>> callBack) {
                EaseIMHelper.getInstance().getGroupManager().asyncGetPublicGroupsFromServer(pageSize, cursor, new EMValueCallBack<EMCursorResult<EMGroupInfo>>() {
                    @Override
                    public void onSuccess(EMCursorResult<EMGroupInfo> value) {
                        callBack.onSuccess(createLiveData(value));
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        callBack.onError(error, errorMsg);
                    }
                });
            }
        }.asLiveData();
    }

    /**
     * 获取群组信息
     * @param groupId
     * @return
     */
    public LiveData<Resource<EMGroup>> getGroupFromServer(String groupId) {
        return new NetworkOnlyResource<EMGroup>() {

            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<EMGroup>> callBack) {
                if(!isLoggedIn()) {
                    callBack.onError(ErrorCode.EM_NOT_LOGIN);
                    return;
                }
                EaseIMHelper.getInstance().getGroupManager().asyncGetGroupFromServer(groupId, new EMValueCallBack<EMGroup>() {
                    @Override
                    public void onSuccess(EMGroup value) {
                        callBack.onSuccess(createLiveData(value));
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        callBack.onError(error, errorMsg);
                    }
                });
            }

        }.asLiveData();
    }

    /**
     * 加入群组
     * @param group
     * @param reason
     * @return
     */
    public LiveData<Resource<Boolean>> joinGroup(EMGroup group, String reason) {
        return new NetworkOnlyResource<Boolean>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<Boolean>> callBack) {
                if(group.isMemberOnly()) {
                    getGroupManager().asyncApplyJoinToGroup(group.getGroupId(), reason, new EMCallBack() {
                        @Override
                        public void onSuccess() {
                            callBack.onSuccess(createLiveData(true));
                        }

                        @Override
                        public void onError(int code, String error) {
                            callBack.onError(code,error);
                        }

                        @Override
                        public void onProgress(int progress, String status) {

                        }
                    });
                }else {
                    getGroupManager().asyncJoinGroup(group.getGroupId(), new EMCallBack() {
                        @Override
                        public void onSuccess() {
                            callBack.onSuccess(createLiveData(true));
                        }

                        @Override
                        public void onError(int code, String error) {
                            callBack.onError(code,error);
                        }

                        @Override
                        public void onProgress(int progress, String status) {

                        }
                    });
                }

            }
        }.asLiveData();
    }

    public LiveData<Resource<List<String>>> getGroupMembersByName(String groupId) {
        return new NetworkOnlyResource<List<String>>() {

            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<List<String>>> callBack) {
                if(!isLoggedIn()) {
                    callBack.onError(ErrorCode.EM_NOT_LOGIN);
                    return;
                }
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
                            callBack.onSuccess(createLiveData(members));
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

        }.asLiveData();
    }

    /**
     * 获取群组成员列表(包含管理员和群主)
     * @param groupId
     * @return
     */
    public LiveData<Resource<List<EaseUser>>> getGroupAllMembers(String groupId) {
        return new NetworkOnlyResource<List<EaseUser>>() {

            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<List<EaseUser>>> callBack) {
                if(!isLoggedIn()) {
                    callBack.onError(ErrorCode.EM_NOT_LOGIN);
                    return;
                }
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
                            List<EaseUser> users = EmUserEntity.parse(members);
                            sortUserData(users);
                            for(EaseUser item : users){
                                if(TextUtils.equals(value.getOwner(), item.getUsername())){
                                    item.setOwner(true);
                                }
                            }
                            callBack.onSuccess(createLiveData(users));
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

        }.asLiveData();
    }

    /**
     * 获取群组成员列表(不包含管理员和群主)
     * @param groupId
     * @return
     */
    public LiveData<Resource<List<EaseUser>>> getGroupMembers(String groupId) {
        return new NetworkOnlyResource<List<EaseUser>>() {

            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<List<EaseUser>>> callBack) {
                if(!isLoggedIn()) {
                    callBack.onError(ErrorCode.EM_NOT_LOGIN);
                    return;
                }
                runOnIOThread(()-> {
                    List<String> members = getAllGroupMemberByServer(groupId);
                    List<EaseUser> users = new ArrayList<>();
                    if(members != null && !members.isEmpty()){
                        for(int i = 0; i < members.size(); i++){
                            EaseUser user = EaseIMHelper.getInstance().getUserInfo(members.get(i));
                            if(user != null){
                                users.add(user);
                            }else{
                                EaseUser m_user = new EaseUser(members.get(i));
                                users.add(m_user);
                            }
                        }
                    }
                    sortUserData(users);
                    callBack.onSuccess(createLiveData(users));
                });
            }

        }.asLiveData();
    }

    /**
     * 获取禁言列表
     * @param groupId
     * @return
     */
    public LiveData<Resource<Map<String, Long>>> getGroupMuteMap(String groupId) {
        return new NetworkOnlyResource<Map<String, Long>>() {

            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<Map<String, Long>>> callBack) {
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
                    callBack.onSuccess(createLiveData(result));
                });

            }

        }.asLiveData();
    }

    /**
     * 获取群组黑名单列表
     * @param groupId
     * @return
     */
    public LiveData<Resource<List<String>>> getGroupBlackList(String groupId) {
        return new NetworkOnlyResource<List<String>>() {

            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<List<String>>> callBack) {
                EaseThreadManager.getInstance().runOnIOThread(() -> {
                    List<String> list = null;
                    try {
                        list = fetchGroupBlacklistFromServer(groupId);
                    } catch (HyphenateException e) {
                        e.printStackTrace();
                        callBack.onError(e.getErrorCode(), e.getMessage());
                        return;
                    }
                    if(list == null) {
                        list = new ArrayList<>();
                    }
                    callBack.onSuccess(createLiveData(list));
                });

            }

        }.asLiveData();
    }

    private List<String> fetchGroupBlacklistFromServer(String groupId) throws HyphenateException {
        int pageSize = 200;
        List<String> list = null;
        List<String> result = new ArrayList<>();
        do{
            list = getGroupManager().fetchGroupBlackList(groupId, 0, pageSize);
            if(list != null) {
                result.addAll(list);
            }
        }while (list != null && list.size() >= pageSize);
        return result;
    }

    /**
     * 获取群公告
     * @param groupId
     * @return
     */
    public LiveData<Resource<String>> getGroupAnnouncement(String groupId) {
        return new NetworkBoundResource<String, String>() {

            @Override
            protected boolean shouldFetch(String data) {
                return true;
            }

            @Override
            protected LiveData<String> loadFromDb() {
                String announcement = EaseIMHelper.getInstance().getGroupManager().getGroup(groupId).getAnnouncement();
                return createLiveData(announcement);
            }

            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<String>> callBack) {
                getGroupManager().asyncFetchGroupAnnouncement(groupId, new EMValueCallBack<String>() {
                    @Override
                    public void onSuccess(String value) {
                        callBack.onSuccess(createLiveData(value));
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        callBack.onError(error, errorMsg);
                    }
                });
            }

            @Override
            protected void saveCallResult(String item) {

            }

        }.asLiveData();
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

    public List<EMGroup> getAllManageGroups(List<EMGroup> allGroups) {
        if(allGroups != null && allGroups.size() > 0) {
            List<EMGroup> manageGroups = new ArrayList<>();
            for (EMGroup group : allGroups) {
                if(TextUtils.equals(group.getOwner(), getCurrentUser()) || group.getAdminList().contains(getCurrentUser())) {
                    manageGroups.add(group);
                }
            }
            // 对数据进行排序
            sortData(manageGroups);
            return manageGroups;
        }
        return new ArrayList<>();
    }

    /**
     * get all join groups, not contain manage groups
     * @return
     */
    public List<EMGroup> getAllJoinGroups(List<EMGroup> allGroups) {
        if(allGroups != null && allGroups.size() > 0) {
            List<EMGroup> joinGroups = new ArrayList<>();
            for (EMGroup group : allGroups) {
                if(!TextUtils.equals(group.getOwner(), getCurrentUser()) && !group.getAdminList().contains(getCurrentUser())) {
                    joinGroups.add(group);
                }
            }
            // 对数据进行排序
            sortData(joinGroups);
            return joinGroups;
        }
        return new ArrayList<>();
    }

    /**
     * 对数据进行排序
     * @param groups
     */
    private void sortData(List<EMGroup> groups) {
        Collections.sort(groups, new Comparator<EMGroup>() {
            @Override
            public int compare(EMGroup o1, EMGroup o2) {
                String name1 = EaseCommonUtils.getLetter(o1.getGroupName());
                String name2 = EaseCommonUtils.getLetter(o2.getGroupName());
                if(name1.equals(name2)){
                    return o1.getGroupId().compareTo(o2.getGroupId());
                }else{
                    if("#".equals(name1)){
                        return 1;
                    }else if("#".equals(name2)){
                        return -1;
                    }
                    return name1.compareTo(name2);
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
    public LiveData<Resource<String>> setGroupName(String groupId, String groupName) {
        return new NetworkOnlyResource<String>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<String>> callBack) {
//                getGroupManager().asyncChangeGroupName(groupId, groupName, new EMCallBack() {
//                    @Override
//                    public void onSuccess() {
//                        callBack.onSuccess(createLiveData(groupName));
//                    }
//
//                    @Override
//                    public void onError(int code, String error) {
//                        callBack.onError(code,  error);
//                    }
//
//                    @Override
//                    public void onProgress(int progress, String status) {
//
//                    }
//                });

                try{
                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    json.put("groupName", groupName);

                    RequestBody body = RequestBody.create(json.toString(), JSON);

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
                                callBack.onSuccess(createLiveData(groupName));
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "");
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
     * 设置群公告
     * @param groupId
     * @param announcement
     * @return
     */
    public LiveData<Resource<String>> setGroupAnnouncement(String groupId, String announcement) {
        return new NetworkOnlyResource<String>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<String>> callBack) {
                getGroupManager().asyncUpdateGroupAnnouncement(groupId, announcement, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        callBack.onSuccess(createLiveData(announcement));
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
        }.asLiveData();
    }

    /**
     * 设置群描述
     * @param groupId
     * @param description
     * @return
     */
    public LiveData<Resource<String>> setGroupDescription(String groupId, String description) {
        return new NetworkOnlyResource<String>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<String>> callBack) {
                getGroupManager().asyncChangeGroupDescription(groupId, description, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        callBack.onSuccess(createLiveData(description));
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
        }.asLiveData();
    }

    /**
     * 获取共享文件
     * @param groupId
     * @param pageNum
     * @param pageSize
     * @return
     */
    public LiveData<Resource<List<EMMucSharedFile>>> getSharedFiles(String groupId, int pageNum, int pageSize) {
        return new NetworkOnlyResource<List<EMMucSharedFile>>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<List<EMMucSharedFile>>> callBack) {
                getGroupManager().asyncFetchGroupSharedFileList(groupId, pageNum, pageSize, new EMValueCallBack<List<EMMucSharedFile>>() {
                    @Override
                    public void onSuccess(List<EMMucSharedFile> value) {
                        callBack.onSuccess(createLiveData(value));
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        callBack.onError(error, errorMsg);
                    }
                });
            }
        }.asLiveData();
    }

    /**
     * 下载共享文件
     * @param groupId
     * @param fileId
     * @param localFile
     * @return
     */
    public LiveData<Resource<File>> downloadFile(String groupId, String fileId, File localFile) {
        return new NetworkOnlyResource<File>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<File>> callBack) {
                getGroupManager().asyncDownloadGroupSharedFile(groupId, fileId, localFile.getAbsolutePath(), new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        callBack.onSuccess(createLiveData(localFile));
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
        }.asLiveData();
    }

    /**
     * 删除服务器端的文件
     * @param groupId
     * @param fileId
     * @return
     */
    public LiveData<Resource<Boolean>> deleteFile(String groupId, String fileId) {
        return new NetworkOnlyResource<Boolean>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<Boolean>> callBack) {
                getGroupManager().asyncDeleteGroupSharedFile(groupId, fileId, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        callBack.onSuccess(createLiveData(true));
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
        }.asLiveData();
    }

    /**
     * 上传文件
     * @param groupId
     * @param filePath
     * @return
     */
    public LiveData<Resource<Boolean>> uploadFile(String groupId, String filePath) {
        return new NetworkOnlyResource<Boolean>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<Boolean>> callBack) {
                getGroupManager().asyncUploadGroupSharedFile(groupId, filePath, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        callBack.onSuccess(createLiveData(true));
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
        }.asLiveData();
    }

    /**
     * 运管端邀请群成员
     */
    public LiveData<Resource<Boolean>> addMembersWithAdmin(String groupId, List<String> customerList, List<String> waiterList) {
        return new NetworkOnlyResource<Boolean>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<Boolean>> callBack) {
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

                    RequestBody body = RequestBody.create(json.toString(), JSON);

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
                                    callBack.onSuccess(createLiveData(true));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "");
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
     * 极狐app邀请群成员
     */
    public LiveData<Resource<Boolean>> addMembersWithCustomer(String groupId, List<String> customerList, List<String> waiterList) {
        return new NetworkOnlyResource<Boolean>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<Boolean>> callBack) {
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

                    RequestBody body = RequestBody.create(json.toString(), JSON);

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
                                    callBack.onSuccess(createLiveData(true));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "");
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
     * 移交群主权限
     * @param groupId
     * @param username
     * @return
     */
    public LiveData<Resource<Boolean>> changeOwner(String groupId, String username) {
        return new NetworkOnlyResource<Boolean>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<Boolean>> callBack) {
                getGroupManager().asyncChangeOwner(groupId, username, new EMValueCallBack<EMGroup>() {
                    @Override
                    public void onSuccess(EMGroup value) {
                        callBack.onSuccess(createLiveData(true));
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        callBack.onError(error, errorMsg);
                    }
                });
            }
        }.asLiveData();
    }

    /**
     * 设为群管理员
     * @param groupId
     * @param username
     * @return
     */
    public LiveData<Resource<String>> addGroupAdmin(String groupId, String username) {
        return new NetworkOnlyResource<String>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<String>> callBack) {
                getGroupManager().asyncAddGroupAdmin(groupId, username, new EMValueCallBack<EMGroup>() {
                    @Override
                    public void onSuccess(EMGroup value) {
                        callBack.onSuccess(createLiveData(getContext().getString(R.string.demo_group_member_add_admin, username)));
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        callBack.onError(error, errorMsg);
                    }
                });
            }
        }.asLiveData();
    }

    /**
     * 移除群管理员
     * @param groupId
     * @param username
     * @return
     */
    public LiveData<Resource<String>> removeGroupAdmin(String groupId, String username) {
        return new NetworkOnlyResource<String>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<String>> callBack) {
                getGroupManager().asyncRemoveGroupAdmin(groupId, username, new EMValueCallBack<EMGroup>() {
                    @Override
                    public void onSuccess(EMGroup value) {
                        callBack.onSuccess(createLiveData(getContext().getString(R.string.demo_group_member_remove_admin, username)));
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        callBack.onError(error, errorMsg);
                    }
                });
            }
        }.asLiveData();
    }

    /**
     * 移出群
     * @param groupId
     * @param username
     * @return
     */
    public LiveData<Resource<String>> removeUserFromGroup(String groupId, String username) {
        return new NetworkOnlyResource<String>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<String>> callBack) {
                getGroupManager().asyncRemoveUserFromGroup(groupId, username, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        callBack.onSuccess(createLiveData(getContext().getString(R.string.demo_group_member_remove, username)));
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
        }.asLiveData();
    }

    /**
     * 移出群
     * @param groupId
     * @param usernames
     * @return
     */
    public LiveData<Resource<String>> removeUsersFromGroup(String groupId, List<String> usernames) {
        return new NetworkOnlyResource<String>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<String>> callBack) {
                getGroupManager().asyncRemoveUsersFromGroup(groupId, usernames, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        callBack.onSuccess(createLiveData("remove success"));
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
        }.asLiveData();
    }

    /**
     * 获取群组申请
     * @param page
     * @param size
     * @return
     */
    public LiveData<Resource<List<GroupApplyBean>>> fetchGroupApply(int page, int size){
        return new NetworkOnlyResource<List<GroupApplyBean>>(){
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<List<GroupApplyBean>>> callBack) {
                try{
                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    json.put("username", EaseIMHelper.getInstance().getCurrentUser());
                    json.put("page", page);
                    json.put("size", size);
                    RequestBody body = RequestBody.create(json.toString(), JSON);

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

                                    callBack.onSuccess(createLiveData(list));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "");
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
     * 处理群组申请
     * @param bean
     * @param state
     * @return
     */
    public LiveData<Resource<GroupApplyBean>> operationGroupApply(GroupApplyBean bean, String state){
        return new NetworkOnlyResource<GroupApplyBean>(){
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<GroupApplyBean>> callBack) {
                try{
                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    json.put("groupId", bean.getGroupId());
                    json.put("username", bean.getUserName());
                    json.put("role", bean.getRole());
                    json.put("option", state);
                    RequestBody body = RequestBody.create(json.toString(), JSON);

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
                                    bean.setOperated(true);
                                    bean.setOperatedResult(state);
                                    callBack.onSuccess(createLiveData(bean));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "");
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
     * 添加到群黑名单
     * @param groupId
     * @param username
     * @return
     */
    public LiveData<Resource<String>> blockUser(String groupId, String username) {
        return new NetworkOnlyResource<String>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<String>> callBack) {
                getGroupManager().asyncBlockUser(groupId, username, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        callBack.onSuccess(createLiveData(getContext().getString(R.string.demo_group_member_add_black, username)));
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
        }.asLiveData();
    }

    /**
     * 移出群黑名单
     * @param groupId
     * @param username
     * @return
     */
    public LiveData<Resource<String>> unblockUser(String groupId, String username) {
        return new NetworkOnlyResource<String>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<String>> callBack) {
                getGroupManager().asyncUnblockUser(groupId, username, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        callBack.onSuccess(createLiveData(getContext().getString(R.string.demo_group_member_remove_black, username)));
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
        }.asLiveData();
    }

    /**
     * 禁言
     * @param groupId
     * @param usernames
     * @return
     */
    public LiveData<Resource<String>> muteGroupMembers(String groupId, List<String> usernames, long duration) {
        return new NetworkOnlyResource<String>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<String>> callBack) {
                getGroupManager().aysncMuteGroupMembers(groupId, usernames, duration, new EMValueCallBack<EMGroup>() {
                    @Override
                    public void onSuccess(EMGroup value) {
                        callBack.onSuccess(createLiveData(getContext().getString(R.string.demo_group_member_mute, usernames.get(0))));
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        callBack.onError(error, errorMsg);
                    }
                });
            }
        }.asLiveData();
    }

    /**
     * 解除禁言
     * @param groupId
     * @param usernames
     * @return
     */
    public LiveData<Resource<String>> unMuteGroupMembers(String groupId, List<String> usernames) {
        return new NetworkOnlyResource<String>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<String>> callBack) {
                getGroupManager().asyncUnMuteGroupMembers(groupId, usernames, new EMValueCallBack<EMGroup>() {
                    @Override
                    public void onSuccess(EMGroup value) {
                        callBack.onSuccess(createLiveData(getContext().getString(R.string.demo_group_member_remove_mute, usernames.get(0))));
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        callBack.onError(error, errorMsg);
                    }
                });
            }
        }.asLiveData();
    }

    /**
     * 退群
     * @param groupId
     * @return
     */
    public LiveData<Resource<Boolean>> leaveGroup(String groupId) {
        return new NetworkOnlyResource<Boolean>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<Boolean>> callBack) {
                getGroupManager().asyncLeaveGroup(groupId, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        callBack.onSuccess(createLiveData(true));
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
        }.asLiveData();
    }

    /**
     * 解散群
     * @param groupId
     * @return
     */
    public LiveData<Resource<Boolean>> destroyGroup(String groupId) {
        return new NetworkOnlyResource<Boolean>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<Boolean>> callBack) {
                getGroupManager().asyncDestroyGroup(groupId, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        callBack.onSuccess(createLiveData(true));
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
        }.asLiveData();
    }

    /**
     * 创建群组
     */
    public LiveData<Resource<String>> createGroup(String groupName, String desc, List<String> customerList, List<String> waiterList) {
        return new NetworkOnlyResource<String>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<String>> callBack) {
//                getGroupManager().asyncCreateGroup(groupName, desc, allMembers, reason, option, new EMValueCallBack<EMGroup>() {
//                    @Override
//                    public void onSuccess(EMGroup value) {
//                        callBack.onSuccess(createLiveData(value));
//                    }
//
//                    @Override
//                    public void onError(int error, String errorMsg) {
//                        callBack.onError(error, errorMsg);
//                    }
//                });

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
                    RequestBody body = RequestBody.create(json.toString(), JSON);

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
                                    JSONObject data = result.getJSONObject("data");
                                    String groupId = data.getString("groupId");
                                    callBack.onSuccess(createLiveData(groupId));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "");
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
     * 屏蔽群消息
     * @param groupId
     * @return
     */
    public LiveData<Resource<Boolean>> blockGroupMessage(String groupId) {
        return new NetworkOnlyResource<Boolean>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<Boolean>> callBack) {
                getGroupManager().asyncBlockGroupMessage(groupId, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        callBack.onSuccess(createLiveData(true));
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
        }.asLiveData();
    }

    /**
     * 取消屏蔽群消息
     * @param groupId
     * @return
     */
    public LiveData<Resource<Boolean>> unblockGroupMessage(String groupId) {
        return new NetworkOnlyResource<Boolean>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<Boolean>> callBack) {
                getGroupManager().asyncUnblockGroupMessage(groupId, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        callBack.onSuccess(createLiveData(true));
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
        }.asLiveData();
    }

    /**
     * 获取服务备注
     */
    public LiveData<Resource<String>> getServiceNote(String groupId){
        return new NetworkOnlyResource<String>(){

            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<String>> callBack) {
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
                                    JSONObject entity = result.getJSONObject("data");
                                    String note = entity.optString("businessRemark");
                                    callBack.onSuccess(createLiveData(note));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "");
                            }
                        }
                    });
            }
        }.asLiveData();
    }

    /**
     * 编辑服务备注
     */
    public LiveData<Resource<String>> changeServiceNote(String groupId, String note){
        return new NetworkOnlyResource<String>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<String>> callBack) {
                try{
                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    json.put("businessRemark", note);
                    RequestBody body = RequestBody.create(json.toString(), JSON);

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
                                callBack.onSuccess(createLiveData(note));
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "");
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
     * 查询群列表
     * @return
     */
    public LiveData<Resource<List<SearchResult>>> searchGroupChat(String aid, String mobile, String orderId, String vin, String groupType, String groupName, String source){
        return new NetworkOnlyResource<List<SearchResult>>(){

            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<List<SearchResult>>> callBack) {
                try{
                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    json.put("aid", aid);
                    json.put("mobile", mobile);
                    json.put("orderId", orderId);
                    json.put("vin", vin);
                    json.put("groupType", groupType);
                    json.put("groupName", groupName);
                    json.put("source", source);
                    RequestBody body = RequestBody.create(json.toString(), JSON);

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
                                    callBack.onSuccess(createLiveData(list));
                                }catch (JSONException e){
                                    e.printStackTrace();
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "");
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
