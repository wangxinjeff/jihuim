package com.hyphenate.easeim.common.repositories;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Pair;

import android.support.annotation.Nullable;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMError;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMCursorResult;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeim.common.model.EMOrder;
import com.hyphenate.easeim.common.net.ErrorCode;
import com.hyphenate.easeim.common.net.Resource;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.manager.EaseThreadManager;
import com.hyphenate.easeui.modules.conversation.model.EaseConversationInfo;
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

/**
 * 处理与chat相关的逻辑
 */
public class EMChatManagerRepository extends BaseEMRepository{

    private static EMChatManagerRepository instance;

    public static EMChatManagerRepository getInstance() {
        if(instance == null) {
            synchronized (EMChatManagerRepository.class) {
                if(instance == null) {
                    instance = new EMChatManagerRepository();
                }
            }
        }
        return instance;
    }

    /**
     * 获取会话列表
     * @return
     */
    public void fetchConversationsFromServer(ResultCallBack<List<EaseConversationInfo>> callBack) {
                EMClient.getInstance().chatManager().asyncFetchConversationsFromServer(new EMValueCallBack<Map<String, EMConversation>>() {
                    @Override
                    public void onSuccess(Map<String, EMConversation> value) {
                        List<EMConversation> conversations = new ArrayList<EMConversation>(value.values());
                        List<EaseConversationInfo> infoList = new ArrayList<>();
                        if(!conversations.isEmpty()) {
                            EaseConversationInfo info = null;
                            for(EMConversation conversation : conversations) {
                                EMLog.e("conversationext:", conversation.getExtField());
                                info = new EaseConversationInfo();
                                info.setInfo(conversation);
                                info.setTimestamp(conversation.getLastMessage().getMsgTime());
                                infoList.add(info);
                            }
                        }
                        callBack.onSuccess(infoList);
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        callBack.onError(error, errorMsg);
                    }
                });
    }

    /**
     * 设置单聊用户聊天免打扰
     *
     * @param userId 用户名
     * @param noPush 是否免打扰
     */
    public void setUserNotDisturb(String userId, boolean noPush, EMCallBack callBack) {
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
                            EMClient.getInstance().chatManager().sendMessage(message);
                            callBack.onSuccess();
                        } catch (HyphenateException e) {
                            e.printStackTrace();
                            callBack.onError(e.getErrorCode(), e.getDescription());
                        }
                    }
                });
    }

    /**
     * 获取聊天免打扰用户
     */
    public void getNoPushUsers(ResultCallBack<List<String>> callBack) {
                runOnIOThread(new Runnable() {
                    @Override
                    public void run() {
                        List<String> noPushUsers = getPushManager().getNoPushUsers();
                        if (noPushUsers != null && noPushUsers.size() != 0) {
                            callBack.onSuccess(noPushUsers);
                        }
                    }
                });
    }

    /**
     * 获取订单列表
     * @param type
     * @return
     */
    public void fetchOrderListFroServer(String type, ResultCallBack<List<EMOrder>> callBack){
                try{
                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    json.put("aid", "222510");
                    json.put("orderType", type);
                    json.put("token", "ad8s8d9adhka");
                    RequestBody body = RequestBody.create(JSON, json.toString());

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
                                    String status = result.optString("status");
                                    if(TextUtils.equals("OK", status) || TextUtils.equals("SUCCEED", status)){
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
                                        callBack.onSuccess(list);
                                    } else {
                                        callBack.onError(EMError.GENERAL_ERROR, "fetchOrderListFroServer failed");
                                    }
                                } catch (JSONException e) {
                                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                                }
                            } else {
                                callBack.onError(EMError.GENERAL_ERROR, "fetchOrderListFroServer failed");
                            }
                        }
                    });
                }catch(JSONException e){
                    e.printStackTrace();
                    callBack.onError(EMError.GENERAL_ERROR, e.getMessage());
                }
    }

}
