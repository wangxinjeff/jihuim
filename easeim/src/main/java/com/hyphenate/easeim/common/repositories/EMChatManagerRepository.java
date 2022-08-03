package com.hyphenate.easeim.common.repositories;

import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMError;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.common.db.entity.InviteMessage;
import com.hyphenate.easeim.common.db.entity.MsgTypeManageEntity;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeim.common.model.EMOrder;
import com.hyphenate.easeim.common.net.ErrorCode;
import com.hyphenate.easeim.common.net.Resource;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.modules.conversation.model.EaseConversationInfo;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.exceptions.HyphenateException;

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

/**
 * 处理与chat相关的逻辑
 */
public class EMChatManagerRepository extends BaseEMRepository{

    /**
     * 获取会话列表
     * @return
     */
    public LiveData<Resource<List<Object>>> loadConversationList() {
        return new NetworkOnlyResource<List<Object>>() {

            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<List<Object>>> callBack) {
                List<Object> emConversations = loadConversationListFromCache();
                callBack.onSuccess(new MutableLiveData<>(emConversations));
            }

        }.asLiveData();
    }

    /**
     * load conversation list
     *
     * @return
    +    */
    protected List<Object> loadConversationListFromCache(){
        // get all conversations
        Map<String, EMConversation> conversations = getChatManager().getAllConversations();
        List<Pair<Long, Object>> sortList = new ArrayList<Pair<Long, Object>>();
        List<Pair<Long, Object>> topSortList = new ArrayList<Pair<Long, Object>>();
        /**
         * lastMsgTime will change if there is new message during sorting
         * so use synchronized to make sure timestamp of last message won't change.
         */
        synchronized (conversations) {
            for (EMConversation conversation : conversations.values()) {
                if (conversation.getAllMessages().size() != 0) {
                    String extField = conversation.getExtField();
                    if(!TextUtils.isEmpty(extField) && EaseCommonUtils.isTimestamp(extField)) {
                        topSortList.add(new Pair<>(Long.valueOf(extField), conversation));
                    }else {
                        sortList.add(new Pair<Long, Object>(conversation.getLastMessage().getMsgTime(), conversation));
                    }
                }
            }
        }
        List<MsgTypeManageEntity> manageEntities = null;
        if(getMsgTypeManageDao() != null) {
            manageEntities = getMsgTypeManageDao().loadAllMsgTypeManage();
        }
        if(manageEntities != null && !manageEntities.isEmpty()) {
            synchronized (EMChatManagerRepository.class) {
                for (MsgTypeManageEntity manage : manageEntities) {
                    String extField = manage.getExtField();
                    if(!TextUtils.isEmpty(extField) && EaseCommonUtils.isTimestamp(extField)) {
                        topSortList.add(new Pair<>(Long.valueOf(extField), manage));
                    }else {
                        Object lastMsg = manage.getLastMsg();
                        if(lastMsg instanceof InviteMessage) {
                            long time = ((InviteMessage) lastMsg).getTime();
                            sortList.add(new Pair<>(time, manage));
                        }
                    }
                }
            }
        }
        try {
            // Internal is TimSort algorithm, has bug
            if(topSortList.size() > 0) {
                sortConversationByLastChatTime(topSortList);
            }
            sortConversationByLastChatTime(sortList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sortList.addAll(0, topSortList);
        List<Object> list = new ArrayList<Object>();
        for (Pair<Long, Object> sortItem : sortList) {
            list.add(sortItem.second);
        }
        return list;
    }

    /**
     * sort conversations according time stamp of last message
     *
     * @param conversationList
     */
    private void sortConversationByLastChatTime(List<Pair<Long, Object>> conversationList) {
        Collections.sort(conversationList, new Comparator<Pair<Long, Object>>() {
            @Override
            public int compare(final Pair<Long, Object> con1, final Pair<Long, Object> con2) {

                if (con1.first.equals(con2.first)) {
                    return 0;
                } else if (con2.first.longValue() > con1.first.longValue()) {
                    return 1;
                } else {
                    return -1;
                }
            }

        });
    }

    public LiveData<Resource<Boolean>> deleteConversationById(String conversationId) {
        return new NetworkOnlyResource<Boolean>() {

            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<Boolean>> callBack) {
                boolean isDelete = getChatManager().deleteConversation(conversationId, true);
                if(isDelete) {
                    callBack.onSuccess(new MutableLiveData<>(true));
                }else {
                    callBack.onError(ErrorCode.EM_DELETE_CONVERSATION_ERROR);
                }
            }

        }.asLiveData();
    }

    /**
     * 将会话置为已读
     * @param conversationId
     * @return
     */
    public LiveData<Resource<Boolean>> makeConversationRead(String conversationId) {
        return new NetworkOnlyResource<Boolean>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<Boolean>> callBack) {
                EMConversation conversation = getChatManager().getConversation(conversationId);
                if(conversation == null) {
                    callBack.onError(ErrorCode.EM_DELETE_CONVERSATION_ERROR);
                }else {
                    conversation.markAllMessagesAsRead();
                    callBack.onSuccess(createLiveData(true));
                }
            }
        }.asLiveData();
    }

    /**
     * 获取会话列表
     * @return
     */
    public LiveData<Resource<List<EaseConversationInfo>>> fetchConversationsFromServer() {
        return new NetworkOnlyResource<List<EaseConversationInfo>>() {

            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<List<EaseConversationInfo>>> callBack) {
                EMClient.getInstance().chatManager().asyncFetchConversationsFromServer(new EMValueCallBack<Map<String, EMConversation>>() {
                    @Override
                    public void onSuccess(Map<String, EMConversation> value) {
                        List<EMConversation> conversations = new ArrayList<EMConversation>(value.values());
                        List<EaseConversationInfo> infoList = new ArrayList<>();
                        if(!conversations.isEmpty()) {
                            EaseConversationInfo info = null;
                            for(EMConversation conversation : conversations) {
                                info = new EaseConversationInfo();
                                info.setInfo(conversation);
                                info.setTimestamp(conversation.getLastMessage().getMsgTime());
                                infoList.add(info);
                            }
                        }
                        callBack.onSuccess(createLiveData(infoList));
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
     * 调用api请求将会话置为已读
     * @param conversationId
     * @return
     */
    public LiveData<Resource<Boolean>> makeConversationReadByAck(String conversationId) {
        return new NetworkOnlyResource<Boolean>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<Boolean>> callBack) {
                runOnIOThread(()-> {
                    try {
                        getChatManager().ackConversationRead(conversationId);
                        callBack.onSuccess(createLiveData(true));
                    } catch (HyphenateException e) {
                        e.printStackTrace();
                        callBack.onError(e.getErrorCode(), e.getDescription());
                    }
                });
            }
        }.asLiveData();
    }

    /**
     * 设置单聊用户聊天免打扰
     *
     * @param userId 用户名
     * @param noPush 是否免打扰
     */
    public LiveData<Resource<Boolean>> setUserNotDisturb(String userId, boolean noPush) {
        return new NetworkOnlyResource<Boolean>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<Boolean>> callBack) {
                runOnIOThread(new Runnable() {
                    @Override
                    public void run() {
                        List<String> onPushList = new ArrayList<>();
                        onPushList.add(userId);
                        try {
                            getPushManager().updatePushServiceForUsers(onPushList, noPush);
                            EMMessage message = EMMessage.createSendMessage(EMMessage.Type.CMD);
                            EMCmdMessageBody body = new EMCmdMessageBody("event");
                            body.deliverOnlineOnly(true);
                            message.addBody(body);
                            message.setTo(EaseIMHelper.getInstance().getCurrentUser());
                            message.setAttribute(EaseConstant.MESSAGE_ATTR_EVENT_TYPE, EaseConstant.EVENT_TYPE_USER_NO_PUSH);
                            message.setAttribute(EaseConstant.MESSAGE_ATTR_NO_PUSH, noPush);
                            message.setAttribute(EaseConstant.MESSAGE_ATTR_NO_PUSH_ID, userId);
                            callBack.onSuccess(createLiveData(true));
                        } catch (HyphenateException e) {
                            e.printStackTrace();
                            callBack.onError(e.getErrorCode(), e.getDescription());
                        }
                    }
                });

            }
        }.asLiveData();
    }

    /**
     * 获取聊天免打扰用户
     */
    public LiveData<Resource<List<String>>> getNoPushUsers() {
        return new NetworkOnlyResource<List<String>>() {
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<List<String>>> callBack) {
                runOnIOThread(new Runnable() {
                    @Override
                    public void run() {
                        List<String> noPushUsers = getPushManager().getNoPushUsers();
                        if (noPushUsers != null && noPushUsers.size() != 0) {
                            callBack.onSuccess(createLiveData(noPushUsers));
                        }
                    }
                });

            }
        }.asLiveData();
    }

    /**
     * 获取订单列表
     * @param type
     * @return
     */
    public LiveData<Resource<List<EMOrder>>> fetchOrderListFroServer(String type){
        return new NetworkOnlyResource<List<EMOrder>>(){
            @Override
            protected void createCall(@NonNull ResultCallBack<LiveData<List<EMOrder>>> callBack) {
                try{
                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
//                    json.put("aid", EaseIMHelper.getInstance().getModel().getAid());
//                    json.put("type", type);
//                    json.put("token", EaseIMHelper.getInstance().getModel().getAidToken());
                    json.put("aid", "222510");
                    json.put("orderType", type);
                    json.put("token", "ad8s8d9adhka");
                    RequestBody body = RequestBody.create(json.toString(), JSON);

                    Headers headers = new Headers.Builder()
                            .add("Authorization", EMClient.getInstance().getAccessToken())
                            .add("username", EaseIMHelper.getInstance().getCurrentUser())
                            .build();
                    Request request = new Request.Builder()
                            .url(EaseIMHelper.getInstance().getServerHost()+"/v4/gov/arcfox/transport/"+EaseIMHelper.getInstance().getCurrentUser()+ "/getOrders")
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
                                    List<EMOrder> list = new ArrayList<>();
                                    JSONObject result = new JSONObject(responseBody);
                                    JSONObject entity = result.getJSONObject("entity");
                                    JSONArray data = entity.getJSONArray("data");
                                    if(data.length() > 0){
                                        for(int i = 0; i < data.length(); i ++){
                                            JSONObject item = data.getJSONObject(i);
                                            EMOrder order = new EMOrder();
                                            order.setId(item.optString("orderId"));
                                            order.setName(item.optString("productName"));
                                            order.setType(item.optString("orderType"));
                                            order.setDate(item.optString("orderDate"));
                                            list.add(order);
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

}
