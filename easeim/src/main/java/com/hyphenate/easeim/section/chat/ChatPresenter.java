package com.hyphenate.easeim.section.chat;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMChatRoomChangeListener;
import com.hyphenate.EMConnectionListener;
import com.hyphenate.EMContactListener;
import com.hyphenate.EMConversationListener;
import com.hyphenate.EMError;
import com.hyphenate.EMMultiDeviceListener;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMMucSharedFile;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.chat.EMUserInfo;
import com.hyphenate.chat.adapter.EMAChatRoomManagerListener;
import com.hyphenate.easecallkit.EaseCallKit;
import com.hyphenate.easecallkit.utils.EaseMsgUtils;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.common.repositories.EMClientRepository;
import com.hyphenate.easeim.common.utils.ToastUtils;
import com.hyphenate.easeim.common.widget.InAppNotification;
import com.hyphenate.easeim.section.conversation.ConversationListActivity;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.db.EaseDbHelper;
import com.hyphenate.easeim.common.db.dao.EmUserDao;
import com.hyphenate.easeim.common.db.entity.EmUserEntity;
import com.hyphenate.easeim.common.db.entity.InviteMessageStatus;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeim.common.livedatas.LiveDataBus;
import com.hyphenate.easeim.common.manager.PushAndMessageHelper;
import com.hyphenate.easeim.common.repositories.EMContactManagerRepository;
import com.hyphenate.easeim.common.repositories.EMGroupManagerRepository;
import com.hyphenate.easeim.common.repositories.EMPushManagerRepository;
import com.hyphenate.easeim.section.chat.activity.ChatActivity;
import com.hyphenate.easeim.section.group.GroupHelper;
import com.hyphenate.easeui.EaseIM;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.interfaces.EaseGroupListener;
import com.hyphenate.easeui.manager.EaseAtMessageHelper;
import com.hyphenate.easeui.manager.EaseChatPresenter;
import com.hyphenate.easeui.manager.EaseSystemMsgManager;
import com.hyphenate.easeui.manager.EaseThreadManager;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.EMLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 主要用于chat过程中的全局监听，并对相应的事件进行处理
 * {@link #init()}方法建议在登录成功以后进行调用
 */
public class ChatPresenter extends EaseChatPresenter {
    private static final String TAG = ChatPresenter.class.getSimpleName();
    private static final int HANDLER_SHOW_TOAST = 0;
    private static ChatPresenter instance;
    private LiveDataBus messageChangeLiveData;
    private boolean isGroupsSyncedWithServer = false;
    private boolean isContactsSyncedWithServer = false;
    private boolean isBlackListSyncedWithServer = false;
    private boolean isPushConfigsWithServer = false;
    private Context appContext;
    protected Handler handler;
    private ChatConnectionListener connectionListener;
    private ChatMultiDeviceListener multiDeviceListener;
    private ChatGroupListener groupListener;
    private ChatContactListener contactListener;
    private ChatRoomListener chatRoomListener;
    private ChatConversationListener conversationListener;

    Queue<String> msgQueue = new ConcurrentLinkedQueue<>();

    private ChatPresenter() {
        appContext = EaseIMHelper.getInstance().getApplication();
        initHandler(appContext.getMainLooper());
        messageChangeLiveData = LiveDataBus.get();
        connectionListener = new ChatConnectionListener();
        multiDeviceListener = new ChatMultiDeviceListener();
        groupListener = new ChatGroupListener();
        contactListener = new ChatContactListener();
        chatRoomListener = new ChatRoomListener();
        conversationListener = new ChatConversationListener();
        //添加网络连接状态监听
        EaseIMHelper.getInstance().getEMClient().addConnectionListener(connectionListener);
        //添加多端登录监听
        EaseIMHelper.getInstance().getEMClient().addMultiDeviceListener(multiDeviceListener);
        //添加群组监听
        EaseIMHelper.getInstance().getGroupManager().addGroupChangeListener(groupListener);
        //添加联系人监听
        EaseIMHelper.getInstance().getContactManager().setContactListener(contactListener);
        //添加聊天室监听
        EaseIMHelper.getInstance().getChatroomManager().addChatRoomChangeListener(chatRoomListener);
        //添加对会话的监听（监听已读回执）
        EaseIMHelper.getInstance().getChatManager().addConversationListener(conversationListener);
    }

    public static ChatPresenter getInstance() {
        if(instance == null) {
            synchronized (ChatPresenter.class) {
                if(instance == null) {
                    instance = new ChatPresenter();
                }
            }
        }
        return instance;
    }

    /**
     * 将需要登录成功进入MainActivity中初始化的逻辑，放到此处进行处理
     */
    public void init() {

    }

    public void clear() {
        if(connectionListener != null) {
            EaseIMHelper.getInstance().getEMClient().removeConnectionListener(connectionListener);
        }
        if(multiDeviceListener != null) {
            EaseIMHelper.getInstance().getEMClient().removeMultiDeviceListener(multiDeviceListener);
        }
        if(groupListener != null) {
            EaseIMHelper.getInstance().getGroupManager().removeGroupChangeListener(groupListener);
        }
        if(contactListener != null) {
            EaseIMHelper.getInstance().getContactManager().removeContactListener(contactListener);
        }
        if(chatRoomListener != null) {
            EaseIMHelper.getInstance().getChatroomManager().removeChatRoomListener(chatRoomListener);
        }
        if(conversationListener != null) {
            EaseIMHelper.getInstance().getChatManager().removeConversationListener(conversationListener);
        }
        if(handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        instance = null;
    }

    public void initHandler(Looper looper) {
        handler = new Handler(looper) {
            @Override
            public void handleMessage(Message msg) {
                Object obj = msg.obj;
                switch (msg.what) {
                    case HANDLER_SHOW_TOAST :
                        if(obj instanceof String) {
                            String str = (String) obj;
                            //ToastUtils.showToast(str);
                            Toast.makeText(appContext, str, Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        };
        while (!msgQueue.isEmpty()) {
            showToast(msgQueue.remove());
        }
    }

    void showToast(@StringRes int mesId) {
        showToast(context.getString(mesId));
    }

    void showToast(final String message) {
        Log.d(TAG, "receive invitation to join the group：" + message);
        if (handler != null) {
            Message msg = Message.obtain(handler, HANDLER_SHOW_TOAST, message);
            handler.sendMessage(msg);
        } else {
            msgQueue.add(message);
        }
    }

    @Override
    public void onMessageReceived(List<EMMessage> messages) {
        super.onMessageReceived(messages);
        // 通话邀请不处理
        for(EMMessage message : messages){
            String messageType = message.getStringAttribute(EaseMsgUtils.CALL_MSG_TYPE, "");
            //有关通话控制信令
            if(TextUtils.equals(messageType, EaseMsgUtils.CALL_MSG_INFO)){
                return;
            }
        }

        EaseEvent event = EaseEvent.create(EaseConstant.MESSAGE_CHANGE_RECEIVE, EaseEvent.TYPE.MESSAGE);
        messageChangeLiveData.with(EaseConstant.MESSAGE_CHANGE_CHANGE).postValue(event);
        for (EMMessage message : messages) {
            EMLog.d(TAG, "onMessageReceived id : " + message.getMsgId());
            EMLog.d(TAG, "onMessageReceived: " + message.getType());

            if(TextUtils.equals(message.getFrom(), EaseIMHelper.getInstance().getCurrentUser())){
                return;
            }

            if(message.getChatType() == EMMessage.ChatType.GroupChat){
                if(EaseAtMessageHelper.get().isAtMeMessage(message)) {

                } else {
                    if(EaseIMHelper.getInstance().isAdmin() && !EaseIMHelper.getInstance().getModel().getSettingMsgNotification()){
                        return;
                    }
                    // 如果设置群组离线消息免打扰，则不进行消息通知
                    List<String> disabledIds = EaseIMHelper.getInstance().getPushManager().getNoPushGroups();
                    if(disabledIds != null && disabledIds.contains(message.conversationId())) {
                        return;
                    }
                }
            } else if(message.getChatType() == EMMessage.ChatType.Chat){
                if(EaseIMHelper.getInstance().isAdmin() && !EaseIMHelper.getInstance().getModel().getSettingMsgNotification()){
                    return;
                }
                List<String> noPushUserIds = EaseIMHelper.getInstance().getPushManager().getNoPushUsers();
                if(noPushUserIds != null && noPushUserIds.contains(message.conversationId())) {
                    return;
                }
            }

            if(EaseIMHelper.getInstance().getLifecycleCallbacks().isFront()) {
                if (message.getChatType() == EMMessage.ChatType.GroupChat && !TextUtils.equals(message.getTo(), EaseIMHelper.getInstance().getChatPageConId())) {
                    EaseThreadManager.getInstance().runOnMainThread(() -> InAppNotification.getInstance().show(message));
                    getNotifier().notify(message);
                    getNotifier().vibrate();
                } else if(message.getChatType() == EMMessage.ChatType.Chat && !TextUtils.equals(message.getFrom(), EaseIMHelper.getInstance().getChatPageConId())){
                    if(EaseIMHelper.getInstance().isAdmin()){
                        getNotifier().notify(message);
                        getNotifier().vibrate();
                    }
                }
            } else {
                // in background, do not refresh UI, notify it in notification bar
//                if (!DemoApplication.getInstance().getLifecycleCallbacks().isFront()) {
                    getNotifier().notify(message);
                    getNotifier().vibrateAndPlayTone(message);
//                }
            }

            //notify new message
//            getNotifier().vibrateAndPlayTone(message);
        }
    }



    /**
     * 判断是否已经启动了MainActivity
     * @return
     */
    private synchronized boolean isAppLaunchMain() {
        List<Activity> activities = EaseIMHelper.getInstance().getLifecycleCallbacks().getActivityList();
        if(activities != null && !activities.isEmpty()) {
            for(int i = activities.size() - 1; i >= 0 ; i--) {
                if(activities.get(i) instanceof ConversationListActivity) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onCmdMessageReceived(List<EMMessage> messages) {
        super.onCmdMessageReceived(messages);

        EaseCallKit.getInstance().onCmdMessageReceived(messages);

        for(EMMessage message : messages){
            EMCmdMessageBody body = (EMCmdMessageBody)message.getBody();
            EaseEvent event = EaseEvent.create(EaseConstant.MESSAGE_CHANGE_CMD_RECEIVE, EaseEvent.TYPE.MESSAGE, body.action());

            if(message.getChatType() == EMMessage.ChatType.GroupChat){
                String callState = message.getStringAttribute(EaseConstant.MESSAGE_ATTR_CALL_STATE, "");
                if(!TextUtils.isEmpty(callState)){
                    EMMessage textMessage = EMMessage.createTextSendMessage("callState", message.getTo());
                    textMessage.setChatType(EMMessage.ChatType.GroupChat);
                    textMessage.setAttribute(EaseConstant.MESSAGE_ATTR_CALL_STATE, callState);
                    textMessage.setAttribute(EaseConstant.MESSAGE_ATTR_CALL_USER, message.getFrom());
                    textMessage.setMsgTime(message.getMsgTime());
                    textMessage.setStatus(EMMessage.Status.SUCCESS);
                    textMessage.setMsgId(message.getMsgId());
                    JSONObject userInfo = null;
                    try{
                        userInfo = message.getJSONObjectAttribute(EaseConstant.MESSAGE_ATTR_USER_INFO);
                    }catch (HyphenateException e){
                        e.printStackTrace();
                    }
                    if(userInfo != null){
                        textMessage.setAttribute(EaseConstant.MESSAGE_ATTR_USER_INFO, userInfo);
                    }
                    EMClient.getInstance().chatManager().saveMessage(textMessage);
                }

                String eventType = message.getStringAttribute(EaseConstant.MESSAGE_ATTR_EVENT_TYPE, "");
                if(!TextUtils.isEmpty(eventType)){
                    if(TextUtils.equals(EaseConstant.CREATE_GROUP_PROMPT, eventType)){
                        if(message.getBooleanAttribute("action", false)){
                            try {
                                String json = EaseIMHelper.getInstance().getModel().getServiceGroup();
                                JSONObject jsonObject;
                                if(TextUtils.isEmpty(json)){
                                    jsonObject = new JSONObject();
                                } else {
                                    jsonObject = new JSONObject(json);
                                }
                                jsonObject.put(message.getTo(), message.getStringAttribute(EaseConstant.GROUP_NAME, message.getTo()));
                                EaseIMHelper.getInstance().getModel().setServiceGroup(jsonObject.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        EMMessage promptMessage = EMMessage.createTextSendMessage("groupEvent", message.getTo());
                        promptMessage.setChatType(EMMessage.ChatType.GroupChat);
                        promptMessage.setAttribute(EaseConstant.CREATE_GROUP_PROMPT, true);
                        promptMessage.setAttribute(EaseConstant.GROUP_NAME, message.getStringAttribute(EaseConstant.GROUP_NAME, message.getTo()));
                        promptMessage.setStatus(EMMessage.Status.SUCCESS);
                        promptMessage.setMsgTime(message.getMsgTime());
                        promptMessage.setMsgId(message.getMsgId());
                        EMClient.getInstance().chatManager().saveMessage(promptMessage);
                    } else if(TextUtils.equals(EaseConstant.JOIN_GROUP_PROMPT, eventType)){
                        EMMessage promptMessage = EMMessage.createTextSendMessage("groupEvent", message.getTo());
                        promptMessage.setChatType(EMMessage.ChatType.GroupChat);
                        promptMessage.setAttribute(EaseConstant.JOIN_GROUP_PROMPT, true);
                        promptMessage.setAttribute(EaseConstant.USER_NAME, message.getStringAttribute(EaseConstant.USER_NAME, ""));
                        promptMessage.setStatus(EMMessage.Status.SUCCESS);
                        promptMessage.setMsgTime(message.getMsgTime());
                        promptMessage.setMsgId(message.getMsgId());
                        EMClient.getInstance().chatManager().saveMessage(promptMessage);
                    }
                }
            } else if(message.getChatType() == EMMessage.ChatType.Chat){
                if(TextUtils.equals("requestJoinGroupEvent", body.action())){
                    event = EaseEvent.create(EaseConstant.MESSAGE_CHANGE_CMD_RECEIVE, EaseEvent.TYPE.MESSAGE, EaseConstant.NEW_GROUP_APPLY);
                } else if(TextUtils.equals("event", body.action())){
                    String eventType = message.getStringAttribute(EaseConstant.MESSAGE_ATTR_EVENT_TYPE, "");
                    if(TextUtils.equals(EaseConstant.EVENT_TYPE_USER_NO_PUSH, eventType) || TextUtils.equals(EaseConstant.EVENT_TYPE_GROUP_NO_PUSH, eventType)){
                        if(TextUtils.equals(EaseConstant.EVENT_TYPE_USER_NO_PUSH, eventType)){
                            boolean noPush = message.getBooleanAttribute(EaseConstant.MESSAGE_ATTR_NO_PUSH, false);
                            String username = message.getStringAttribute(EaseConstant.MESSAGE_ATTR_NO_PUSH_ID, "");

                        } else if(TextUtils.equals(EaseConstant.EVENT_TYPE_GROUP_NO_PUSH, eventType)){
                            boolean noPush = message.getBooleanAttribute(EaseConstant.MESSAGE_ATTR_NO_PUSH, false);
                            String username =message.getStringAttribute(EaseConstant.MESSAGE_ATTR_NO_PUSH_ID, "");

                        }
                        event = EaseEvent.create(EaseConstant.MESSAGE_CHANGE_CMD_RECEIVE, EaseEvent.TYPE.MESSAGE, EaseConstant.NO_PUSH_CHANGE);
                    }


                }
            }
            messageChangeLiveData.with(EaseConstant.MESSAGE_CHANGE_CHANGE).postValue(event);
        }
    }

    @Override
    public void onMessageRead(List<EMMessage> messages) {
        super.onMessageRead(messages);
        if(!(EaseIMHelper.getInstance().getLifecycleCallbacks().current() instanceof ChatActivity)) {
            EaseEvent event = EaseEvent.create(EaseConstant.MESSAGE_CHANGE_RECALL, EaseEvent.TYPE.MESSAGE);
            messageChangeLiveData.with(EaseConstant.MESSAGE_CHANGE_CHANGE).postValue(event);
        }
    }

    @Override
    public void onMessageRecalled(List<EMMessage> messages) {

        for (EMMessage msg : messages) {
            if(msg.getChatType() == EMMessage.ChatType.GroupChat && EaseAtMessageHelper.get().isAtMeMessage(msg)){
                EaseAtMessageHelper.get().removeAtMeGroup(msg.getTo());
            }
            EMMessage msgNotification = EMMessage.createReceiveMessage(EMMessage.Type.TXT);
            String recaller = msg.getRecaller();
            EMTextMessageBody txtBody = new EMTextMessageBody("recall");
            msgNotification.addBody(txtBody);
            msgNotification.setDirection(msg.direct());
            msgNotification.setFrom(msg.getFrom());
            msgNotification.setTo(msg.getTo());
            msgNotification.setUnread(false);
            msgNotification.setMsgTime(msg.getMsgTime());
            msgNotification.setLocalTime(msg.getMsgTime());
            msgNotification.setChatType(msg.getChatType());
            msgNotification.setAttribute(EaseConstant.MESSAGE_TYPE_RECALL, true);
            msgNotification.setAttribute(EaseConstant.MESSAGE_TYPE_RECALLER, recaller);
            msgNotification.setStatus(EMMessage.Status.SUCCESS);
            EMClient.getInstance().chatManager().saveMessage(msgNotification);
        }

        EaseEvent event = EaseEvent.create(EaseConstant.MESSAGE_CHANGE_RECALL, EaseEvent.TYPE.MESSAGE);
        messageChangeLiveData.with(EaseConstant.MESSAGE_CHANGE_CHANGE).postValue(event);
    }

    private class ChatConversationListener implements EMConversationListener {

        @Override
        public void onCoversationUpdate() {

        }

        @Override
        public void onConversationRead(String from, String to) {
            EaseEvent event = EaseEvent.create(EaseConstant.CONVERSATION_READ, EaseEvent.TYPE.MESSAGE);
            messageChangeLiveData.with(EaseConstant.CONVERSATION_READ).postValue(event);
        }
    }

    private class ChatConnectionListener implements EMConnectionListener {

        @Override
        public void onConnected() {
            EMLog.i(TAG, "onConnected");

            if(!isGroupsSyncedWithServer) {
                EMLog.i(TAG, "isGroupsSyncedWithServer");
                new EMGroupManagerRepository().getAllGroups(new ResultCallBack<List<EMGroup>>() {
                    @Override
                    public void onSuccess(List<EMGroup> value) {
                        //加载完群组信息后，刷新会话列表页面，保证展示群组名称
                        EMLog.i(TAG, "isGroupsSyncedWithServer success");
                        EaseEvent event = EaseEvent.create(EaseConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP);
                        messageChangeLiveData.with(EaseConstant.GROUP_CHANGE).postValue(event);
                    }

                    @Override
                    public void onError(int error, String errorMsg) {

                    }
                });
                isGroupsSyncedWithServer = true;
            }
            if(!isPushConfigsWithServer) {
                EMLog.i(TAG, "isPushConfigsWithServer");
                //首先获取push配置，否则获取push配置项会为空
                new EMPushManagerRepository().fetchPushConfigsFromServer();
                new EMClientRepository().fetchSelfInfo();
                isPushConfigsWithServer = true;
            }

            LiveDataBus.get().with(EaseConstant.ACCOUNT_CHANGE).postValue(new EaseEvent(EaseConstant.ACCOUNT_CONNECT, EaseEvent.TYPE.ACCOUNT));
        }

        /**
         * 用来监听账号异常
         * @param error
         */
        @Override
        public void onDisconnected(int error) {
            EMLog.i(TAG, "onDisconnected ="+error);
            String event = null;
            if (error == EMError.USER_REMOVED) {
                event = EaseConstant.ACCOUNT_REMOVED;
            } else if (error == EMError.USER_LOGIN_ANOTHER_DEVICE
                    || error == EMError.USER_BIND_ANOTHER_DEVICE
                    || error == EMError.USER_DEVICE_CHANGED
                    || error == EMError.USER_LOGIN_TOO_MANY_DEVICES) {
                event = EaseConstant.ACCOUNT_CONFLICT;
            } else if (error == EMError.SERVER_SERVICE_RESTRICTED) {
                event = EaseConstant.ACCOUNT_FORBIDDEN;
            } else if (error == EMError.USER_KICKED_BY_CHANGE_PASSWORD) {
                event = EaseConstant.ACCOUNT_KICKED_BY_CHANGE_PASSWORD;
            } else if (error == EMError.USER_KICKED_BY_OTHER_DEVICE) {
                event = EaseConstant.ACCOUNT_KICKED_BY_OTHER_DEVICE;
            } else if (error == EMError.NETWORK_ERROR){
                event = EaseConstant.ACCOUNT_DIS;
            }
            if(!TextUtils.isEmpty(event)) {
                LiveDataBus.get().with(EaseConstant.ACCOUNT_CHANGE).postValue(new EaseEvent(event, EaseEvent.TYPE.ACCOUNT));
                EMLog.i(TAG, event);
            }
        }
    }

    private class ChatGroupListener extends EaseGroupListener {

        @Override
        public void onInvitationReceived(String groupId, String groupName, String inviter, String reason) {
            super.onInvitationReceived(groupId, groupName, inviter, reason);
            //移除相同的请求
            List<EMMessage> allMessages = EaseSystemMsgManager.getInstance().getAllMessages();
            if(allMessages != null && !allMessages.isEmpty()) {
                for (EMMessage message : allMessages) {
                    Map<String, Object> ext = message.ext();
                    if(ext != null && (ext.containsKey(EaseConstant.SYSTEM_MESSAGE_GROUP_ID) && TextUtils.equals(groupId, (String)ext.get(EaseConstant.SYSTEM_MESSAGE_GROUP_ID)))
                            && (ext.containsKey(EaseConstant.SYSTEM_MESSAGE_INVITER) && TextUtils.equals(inviter, (String)ext.get(EaseConstant.SYSTEM_MESSAGE_INVITER)))) {
                        EaseSystemMsgManager.getInstance().removeMessage(message);
                    }
                }
            }
            groupName = TextUtils.isEmpty(groupName) ? groupId : groupName;
            Map<String, Object> ext = EaseSystemMsgManager.getInstance().createMsgExt();
            ext.put(EaseConstant.SYSTEM_MESSAGE_FROM, groupId);
            ext.put(EaseConstant.SYSTEM_MESSAGE_GROUP_ID, groupId);
            ext.put(EaseConstant.SYSTEM_MESSAGE_REASON, reason);
            ext.put(EaseConstant.SYSTEM_MESSAGE_NAME, groupName);
            ext.put(EaseConstant.SYSTEM_MESSAGE_INVITER, inviter);
            ext.put(EaseConstant.SYSTEM_MESSAGE_STATUS, InviteMessageStatus.GROUPINVITATION.name());
            EMMessage message = EaseSystemMsgManager.getInstance().createMessage(PushAndMessageHelper.getSystemMessage(ext), ext);

            notifyNewInviteMessage(message);
            EaseEvent event = EaseEvent.create(EaseConstant.NOTIFY_GROUP_INVITE_RECEIVE, EaseEvent.TYPE.NOTIFY);
            messageChangeLiveData.with(EaseConstant.NOTIFY_CHANGE).postValue(event);

            showToast(context.getString(InviteMessageStatus.GROUPINVITATION.getMsgContent(), inviter, groupName));
            EMLog.i(TAG, context.getString(InviteMessageStatus.GROUPINVITATION.getMsgContent(), inviter, groupName));
        }

        @Override
        public void onInvitationAccepted(String groupId, String invitee, String reason) {
            super.onInvitationAccepted(groupId, invitee, reason);
            //user accept your invitation
            String groupName = GroupHelper.getGroupName(groupId);

            Map<String, Object> ext = EaseSystemMsgManager.getInstance().createMsgExt();
            ext.put(EaseConstant.SYSTEM_MESSAGE_FROM, groupId);
            ext.put(EaseConstant.SYSTEM_MESSAGE_GROUP_ID, groupId);
            ext.put(EaseConstant.SYSTEM_MESSAGE_REASON, reason);
            ext.put(EaseConstant.SYSTEM_MESSAGE_NAME, groupName);
            ext.put(EaseConstant.SYSTEM_MESSAGE_INVITER, invitee);
            ext.put(EaseConstant.SYSTEM_MESSAGE_STATUS, InviteMessageStatus.GROUPINVITATION_ACCEPTED.name());
            EMMessage message = EaseSystemMsgManager.getInstance().createMessage(PushAndMessageHelper.getSystemMessage(ext), ext);

            notifyNewInviteMessage(message);
            EaseEvent event = EaseEvent.create(EaseConstant.NOTIFY_GROUP_INVITE_ACCEPTED, EaseEvent.TYPE.NOTIFY);
            messageChangeLiveData.with(EaseConstant.NOTIFY_CHANGE).postValue(event);

            showToast(context.getString(InviteMessageStatus.GROUPINVITATION_ACCEPTED.getMsgContent(), invitee));
            EMLog.i(TAG, context.getString(InviteMessageStatus.GROUPINVITATION_ACCEPTED.getMsgContent(), invitee));
        }

        @Override
        public void onInvitationDeclined(String groupId, String invitee, String reason) {
            super.onInvitationDeclined(groupId, invitee, reason);
            //user declined your invitation
            String groupName = GroupHelper.getGroupName(groupId);

            Map<String, Object> ext = EaseSystemMsgManager.getInstance().createMsgExt();
            ext.put(EaseConstant.SYSTEM_MESSAGE_FROM, groupId);
            ext.put(EaseConstant.SYSTEM_MESSAGE_GROUP_ID, groupId);
            ext.put(EaseConstant.SYSTEM_MESSAGE_REASON, reason);
            ext.put(EaseConstant.SYSTEM_MESSAGE_NAME, groupName);
            ext.put(EaseConstant.SYSTEM_MESSAGE_INVITER, invitee);
            ext.put(EaseConstant.SYSTEM_MESSAGE_STATUS, InviteMessageStatus.GROUPINVITATION_DECLINED.name());
            EMMessage message = EaseSystemMsgManager.getInstance().createMessage(PushAndMessageHelper.getSystemMessage(ext), ext);

            notifyNewInviteMessage(message);
            EaseEvent event = EaseEvent.create(EaseConstant.NOTIFY_GROUP_INVITE_DECLINED, EaseEvent.TYPE.NOTIFY);
            messageChangeLiveData.with(EaseConstant.NOTIFY_CHANGE).postValue(event);

            showToast(context.getString(InviteMessageStatus.GROUPINVITATION_DECLINED.getMsgContent(), invitee));
            EMLog.i(TAG, context.getString(InviteMessageStatus.GROUPINVITATION_DECLINED.getMsgContent(), invitee));
        }

        @Override
        public void onUserRemoved(String groupId, String groupName) {
            EaseEvent easeEvent = new EaseEvent(EaseConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP_LEAVE);
            easeEvent.message = groupId;
            messageChangeLiveData.with(EaseConstant.GROUP_CHANGE).postValue(easeEvent);

            EMLog.i(TAG, context.getString(R.string.demo_group_listener_onUserRemoved, groupName));
        }

        @Override
        public void onGroupDestroyed(String groupId, String groupName) {
            EaseEvent easeEvent = new EaseEvent(EaseConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP_LEAVE);
            easeEvent.message = groupId;
            messageChangeLiveData.with(EaseConstant.GROUP_CHANGE).postValue(easeEvent);

            EMLog.i(TAG, context.getString(R.string.demo_group_listener_onGroupDestroyed, groupName));
        }

        @Override
        public void onRequestToJoinReceived(String groupId, String groupName, String applicant, String reason) {
            super.onRequestToJoinReceived(groupId, groupName, applicant, reason);
            //移除相同的请求
            List<EMMessage> allMessages = EaseSystemMsgManager.getInstance().getAllMessages();
            if(allMessages != null && !allMessages.isEmpty()) {
                for (EMMessage message : allMessages) {
                    Map<String, Object> ext = message.ext();
                    if(ext != null && (ext.containsKey(EaseConstant.SYSTEM_MESSAGE_GROUP_ID) && TextUtils.equals(groupId, (String)ext.get(EaseConstant.SYSTEM_MESSAGE_GROUP_ID)))
                            && (ext.containsKey(EaseConstant.SYSTEM_MESSAGE_FROM) && TextUtils.equals(applicant, (String)ext.get(EaseConstant.SYSTEM_MESSAGE_FROM)))) {
                        EaseSystemMsgManager.getInstance().removeMessage(message);
                    }
                }
            }
            // user apply to join group
            Map<String, Object> ext = EaseSystemMsgManager.getInstance().createMsgExt();
            ext.put(EaseConstant.SYSTEM_MESSAGE_FROM, applicant);
            ext.put(EaseConstant.SYSTEM_MESSAGE_GROUP_ID, groupId);
            ext.put(EaseConstant.SYSTEM_MESSAGE_REASON, reason);
            ext.put(EaseConstant.SYSTEM_MESSAGE_NAME, groupName);
            ext.put(EaseConstant.SYSTEM_MESSAGE_STATUS, InviteMessageStatus.BEAPPLYED.name());
            EMMessage message = EaseSystemMsgManager.getInstance().createMessage(PushAndMessageHelper.getSystemMessage(ext), ext);

            notifyNewInviteMessage(message);
            EaseEvent event = EaseEvent.create(EaseConstant.NOTIFY_GROUP_JOIN_RECEIVE, EaseEvent.TYPE.NOTIFY);
            messageChangeLiveData.with(EaseConstant.NOTIFY_CHANGE).postValue(event);

            showToast(context.getString(InviteMessageStatus.BEAPPLYED.getMsgContent(), applicant, groupName));
            EMLog.i(TAG, context.getString(InviteMessageStatus.BEAPPLYED.getMsgContent(), applicant, groupName));
        }

        @Override
        public void onRequestToJoinAccepted(String groupId, String groupName, String accepter) {
            super.onRequestToJoinAccepted(groupId, groupName, accepter);
            // your application was accepted
            EMMessage msg = EMMessage.createReceiveMessage(EMMessage.Type.TXT);
            msg.setChatType(EMMessage.ChatType.GroupChat);
            msg.setFrom(accepter);
            msg.setTo(groupId);
            msg.setMsgId(UUID.randomUUID().toString());
            msg.setAttribute(EaseConstant.EM_NOTIFICATION_TYPE, true);
            msg.addBody(new EMTextMessageBody(context.getString(R.string.demo_group_listener_onRequestToJoinAccepted, accepter, groupName)));
            msg.setStatus(EMMessage.Status.SUCCESS);
            // save accept message
            EMClient.getInstance().chatManager().saveMessage(msg);
            // notify the accept message
//            getNotifier().vibrateAndPlayTone(msg);

            EaseEvent event = EaseEvent.create(EaseConstant.MESSAGE_GROUP_JOIN_ACCEPTED, EaseEvent.TYPE.MESSAGE);
            messageChangeLiveData.with(EaseConstant.MESSAGE_CHANGE_CHANGE).postValue(event);

            EMLog.i(TAG, context.getString(R.string.demo_group_listener_onRequestToJoinAccepted, accepter, groupName));
        }

        @Override
        public void onRequestToJoinDeclined(String groupId, String groupName, String decliner, String reason) {
            super.onRequestToJoinDeclined(groupId, groupName, decliner, reason);
            EMLog.i(TAG, context.getString(R.string.demo_group_listener_onRequestToJoinDeclined, decliner, groupName));
        }

        @Override
        public void onAutoAcceptInvitationFromGroup(String groupId, String inviter, String inviteMessage) {
            super.onAutoAcceptInvitationFromGroup(groupId, inviter, inviteMessage);
//            String groupName = GroupHelper.getGroupName(groupId);
//            EMMessage msg = EMMessage.createReceiveMessage(EMMessage.Type.TXT);
//            msg.setChatType(EMMessage.ChatType.GroupChat);
//            msg.setFrom(inviter);
//            msg.setTo(groupId);
//            msg.setMsgId(UUID.randomUUID().toString());
//            msg.setAttribute(EaseConstant.EM_NOTIFICATION_TYPE, true);
//            msg.addBody(new EMTextMessageBody(context.getString(R.string.demo_group_listener_onAutoAcceptInvitationFromGroup, groupName)));
//            msg.setStatus(EMMessage.Status.SUCCESS);
//            // save invitation as messages
//            EMClient.getInstance().chatManager().saveMessage(msg);
//            // notify invitation message
//            getNotifier().vibrateAndPlayTone(msg);
//            EaseEvent event = EaseEvent.create(EaseConstant.MESSAGE_GROUP_AUTO_ACCEPT, EaseEvent.TYPE.MESSAGE);
//            messageChangeLiveData.with(EaseConstant.MESSAGE_CHANGE_CHANGE).postValue(event);
//
//            EMLog.i(TAG, context.getString(R.string.demo_group_listener_onAutoAcceptInvitationFromGroup, groupName));
        }

        @Override
        public void onMuteListAdded(String groupId, List<String> mutes, long muteExpire) {
            super.onMuteListAdded(groupId, mutes, muteExpire);
            for(String user: mutes){
                if(TextUtils.equals(user, EaseIMHelper.getInstance().getCurrentUser())){
                    EaseThreadManager.getInstance().runOnMainThread(() -> ToastUtils.showCenterToast("", context.getString(R.string.em_group_yourself_mute), 0 ,Toast.LENGTH_SHORT));
                }
            }
            String content = getContentFromList(mutes);
            EMLog.i(TAG, context.getString(R.string.demo_group_listener_onMuteListAdded, content));
        }

        @Override
        public void onMuteListRemoved(String groupId, List<String> mutes) {
            super.onMuteListRemoved(groupId, mutes);
            for(String user: mutes){
                if(TextUtils.equals(user, EaseIMHelper.getInstance().getCurrentUser())){
                    EaseThreadManager.getInstance().runOnMainThread(() -> ToastUtils.showCenterToast("", context.getString(R.string.em_group_yourself_un_mute), 0 ,Toast.LENGTH_SHORT));
                }
            }
            String content = getContentFromList(mutes);
            EMLog.i(TAG, context.getString(R.string.demo_group_listener_onMuteListRemoved, content));
        }

        @Override
        public void onWhiteListAdded(String groupId, List<String> whitelist) {
            EaseEvent easeEvent = new EaseEvent(EaseConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP);
            easeEvent.message = groupId;
            messageChangeLiveData.with(EaseConstant.GROUP_CHANGE).postValue(easeEvent);

            String content = getContentFromList(whitelist);
            EMLog.i(TAG, context.getString(R.string.demo_group_listener_onWhiteListAdded, content));
        }

        @Override
        public void onWhiteListRemoved(String groupId, List<String> whitelist) {
            EaseEvent easeEvent = new EaseEvent(EaseConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP);
            easeEvent.message = groupId;
            messageChangeLiveData.with(EaseConstant.GROUP_CHANGE).postValue(easeEvent);

            String content = getContentFromList(whitelist);
            EMLog.i(TAG, context.getString(R.string.demo_group_listener_onWhiteListRemoved, content));
        }

        @Override
        public void onAllMemberMuteStateChanged(String groupId, boolean isMuted) {
            EaseEvent easeEvent = new EaseEvent(EaseConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP);
            easeEvent.message = groupId;
            messageChangeLiveData.with(EaseConstant.GROUP_CHANGE).postValue(easeEvent);


            EMLog.i(TAG, context.getString(isMuted ? R.string.demo_group_listener_onAllMemberMuteStateChanged_mute
                    : R.string.demo_group_listener_onAllMemberMuteStateChanged_not_mute));
        }

        @Override
        public void onAdminAdded(String groupId, String administrator) {
            super.onAdminAdded(groupId, administrator);
            LiveDataBus.get().with(EaseConstant.GROUP_CHANGE).postValue(EaseEvent.create(EaseConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP));
            EMLog.i(TAG, context.getString(R.string.demo_group_listener_onAdminAdded, administrator));
        }

        @Override
        public void onAdminRemoved(String groupId, String administrator) {
            LiveDataBus.get().with(EaseConstant.GROUP_CHANGE).postValue(EaseEvent.create(EaseConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP));
            EMLog.i(TAG, context.getString(R.string.demo_group_listener_onAdminRemoved, administrator));
        }

        @Override
        public void onOwnerChanged(String groupId, String newOwner, String oldOwner) {
            LiveDataBus.get().with(EaseConstant.GROUP_CHANGE).postValue(EaseEvent.create(EaseConstant.GROUP_OWNER_TRANSFER, EaseEvent.TYPE.GROUP));
            EMLog.i(TAG, context.getString(R.string.demo_group_listener_onOwnerChanged, oldOwner, newOwner));
        }

        @Override
        public void onMemberJoined(String groupId, String member) {
            LiveDataBus.get().with(EaseConstant.GROUP_CHANGE).postValue(EaseEvent.create(EaseConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP, groupId));
            EMLog.i(TAG, context.getString(R.string.demo_group_listener_onMemberJoined, member));
        }

        @Override
        public void onMemberExited(String groupId, String member) {
            LiveDataBus.get().with(EaseConstant.GROUP_CHANGE).postValue(EaseEvent.create(EaseConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP, groupId));
            EMLog.i(TAG, context.getString(R.string.demo_group_listener_onMemberExited, member));
        }

        @Override
        public void onAnnouncementChanged(String groupId, String announcement) {
            LiveDataBus.get().with(EaseConstant.GROUP_CHANGE).postValue(EaseEvent.create(EaseConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP, groupId));
            EMLog.i(TAG, context.getString(R.string.demo_group_listener_onAnnouncementChanged));
        }

        @Override
        public void onSharedFileAdded(String groupId, EMMucSharedFile sharedFile) {
            LiveDataBus.get().with(EaseConstant.GROUP_SHARE_FILE_CHANGE).postValue(EaseEvent.create(EaseConstant.GROUP_SHARE_FILE_CHANGE, EaseEvent.TYPE.GROUP));
            EMLog.i(TAG, context.getString(R.string.demo_group_listener_onSharedFileAdded, sharedFile.getFileName()));
        }

        @Override
        public void onSharedFileDeleted(String groupId, String fileId) {
            LiveDataBus.get().with(EaseConstant.GROUP_SHARE_FILE_CHANGE).postValue(EaseEvent.create(EaseConstant.GROUP_SHARE_FILE_CHANGE, EaseEvent.TYPE.GROUP));
            EMLog.i(TAG, context.getString(R.string.demo_group_listener_onSharedFileDeleted, fileId));
        }

        @Override
        public void onSpecificationChanged(EMGroup emGroup) {
            LiveDataBus.get().with(EaseConstant.GROUP_CHANGE).postValue(EaseEvent.create(EaseConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP, emGroup.getGroupId()));
        }
    }

    private class ChatContactListener implements EMContactListener {

        @Override
        public void onContactAdded(String username) {
            EMLog.i("ChatContactListener", "onContactAdded");
            String[] userId = new String[1];
            userId[0] = username;
            EMClient.getInstance().userInfoManager().fetchUserInfoByUserId(userId, new EMValueCallBack<Map<String, EMUserInfo>>() {
                @Override
                public void onSuccess(Map<String, EMUserInfo> value) {
                    EMUserInfo userInfo = value.get(username);
                    EmUserEntity entity = new EmUserEntity();
                    entity.setUsername(username);
                    if(userInfo != null){
                        entity.setNickname(userInfo.getNickName());
                        entity.setEmail(userInfo.getEmail());
                        entity.setAvatar(userInfo.getAvatarUrl());
                        entity.setBirth(userInfo.getBirth());
                        entity.setGender(userInfo.getGender());
                        entity.setExt(userInfo.getExt());
                        entity.setContact(0);
                        entity.setSign(userInfo.getSignature());
                    }
                    EaseIMHelper.getInstance().getModel().insert(entity);
                    EaseIMHelper.getInstance().updateContactList();
                    EaseEvent event = EaseEvent.create(EaseConstant.CONTACT_ADD, EaseEvent.TYPE.CONTACT);
                    event.message = username;
                    messageChangeLiveData.with(EaseConstant.CONTACT_ADD).postValue(event);

                    EMLog.i(TAG, context.getString(R.string.demo_contact_listener_onContactAdded, username));
                }

                @Override
                public void onError(int error, String errorMsg) {
                    EMLog.i(TAG, context.getString(R.string.demo_contact_get_userInfo_failed) +  username + "error:" + error + " errorMsg:" +errorMsg);
                }
            });
        }

        @Override
        public void onContactDeleted(String username) {
            EMLog.i("ChatContactListener", "onContactDeleted");
            boolean deleteUsername = EaseIMHelper.getInstance().getModel().isDeleteUsername(username);
            int num = EaseIMHelper.getInstance().deleteContact(username);
            EaseIMHelper.getInstance().updateContactList();
            EaseEvent event = EaseEvent.create(EaseConstant.CONTACT_DELETE, EaseEvent.TYPE.CONTACT);
            event.message = username;
            messageChangeLiveData.with(EaseConstant.CONTACT_DELETE).postValue(event);

            if(deleteUsername || num == 0) {
                EMLog.i(TAG, context.getString(R.string.demo_contact_listener_onContactDeleted, username));
            }else {
                EMLog.i(TAG, context.getString(R.string.demo_contact_listener_onContactDeleted_by_other, username));
            }
        }



        @Override
        public void onContactInvited(String username, String reason) {
            EMLog.i("ChatContactListener", "onContactInvited");
            List<EMMessage> allMessages = EaseSystemMsgManager.getInstance().getAllMessages();
            if(allMessages != null && !allMessages.isEmpty()) {
                for (EMMessage message : allMessages) {
                    Map<String, Object> ext = message.ext();
                    if(ext != null && !ext.containsKey(EaseConstant.SYSTEM_MESSAGE_GROUP_ID)
                            && (ext.containsKey(EaseConstant.SYSTEM_MESSAGE_FROM) && TextUtils.equals(username, (String)ext.get(EaseConstant.SYSTEM_MESSAGE_FROM)))) {
                        EaseSystemMsgManager.getInstance().removeMessage(message);
                    }
                }
            }

            Map<String, Object> ext = EaseSystemMsgManager.getInstance().createMsgExt();
            ext.put(EaseConstant.SYSTEM_MESSAGE_FROM, username);
            ext.put(EaseConstant.SYSTEM_MESSAGE_REASON, reason);
            ext.put(EaseConstant.SYSTEM_MESSAGE_STATUS, InviteMessageStatus.BEINVITEED.name());
            EMMessage message = EaseSystemMsgManager.getInstance().createMessage(PushAndMessageHelper.getSystemMessage(ext), ext);

            notifyNewInviteMessage(message);
            EaseEvent event = EaseEvent.create(EaseConstant.CONTACT_CHANGE, EaseEvent.TYPE.CONTACT);
            messageChangeLiveData.with(EaseConstant.CONTACT_CHANGE).postValue(event);

            EMLog.i(TAG, context.getString(InviteMessageStatus.BEINVITEED.getMsgContent(), username));
        }

        @Override
        public void onFriendRequestAccepted(String username) {
            EMLog.i("ChatContactListener", "onFriendRequestAccepted");
            List<EMMessage> allMessages = EaseSystemMsgManager.getInstance().getAllMessages();
            if(allMessages != null && !allMessages.isEmpty()) {
                for (EMMessage message : allMessages) {
                    Map<String, Object> ext = message.ext();
                    if(ext != null && (ext.containsKey(EaseConstant.SYSTEM_MESSAGE_FROM)
                            && TextUtils.equals(username, (String)ext.get(EaseConstant.SYSTEM_MESSAGE_FROM)))) {
                        updateMessage(message);
                        return;
                    }
                }
            }
            Map<String, Object> ext = EaseSystemMsgManager.getInstance().createMsgExt();
            ext.put(EaseConstant.SYSTEM_MESSAGE_FROM, username);
            ext.put(EaseConstant.SYSTEM_MESSAGE_STATUS, InviteMessageStatus.BEAGREED.name());
            EMMessage message = EaseSystemMsgManager.getInstance().createMessage(PushAndMessageHelper.getSystemMessage(ext), ext);

            notifyNewInviteMessage(message);
            EaseEvent event = EaseEvent.create(EaseConstant.CONTACT_CHANGE, EaseEvent.TYPE.CONTACT);
            messageChangeLiveData.with(EaseConstant.CONTACT_CHANGE).postValue(event);

            EMLog.i(TAG, context.getString(InviteMessageStatus.BEAGREED.getMsgContent()));
        }

        @Override
        public void onFriendRequestDeclined(String username) {
            EMLog.i("ChatContactListener", "onFriendRequestDeclined");
            Map<String, Object> ext = EaseSystemMsgManager.getInstance().createMsgExt();
            ext.put(EaseConstant.SYSTEM_MESSAGE_FROM, username);
            ext.put(EaseConstant.SYSTEM_MESSAGE_STATUS, InviteMessageStatus.BEREFUSED.name());
            EMMessage message = EaseSystemMsgManager.getInstance().createMessage(PushAndMessageHelper.getSystemMessage(ext), ext);

            notifyNewInviteMessage(message);

            EaseEvent event = EaseEvent.create(EaseConstant.CONTACT_CHANGE, EaseEvent.TYPE.CONTACT);
            messageChangeLiveData.with(EaseConstant.CONTACT_CHANGE).postValue(event);
            EMLog.i(TAG, context.getString(InviteMessageStatus.BEREFUSED.getMsgContent(), username));
        }
    }


    private void updateMessage(EMMessage message) {
        message.setAttribute(EaseConstant.SYSTEM_MESSAGE_STATUS, InviteMessageStatus.BEAGREED.name());
        EMTextMessageBody body = new EMTextMessageBody(PushAndMessageHelper.getSystemMessage(message.ext()));
        message.addBody(body);
        EaseSystemMsgManager.getInstance().updateMessage(message);
    }

    private class ChatMultiDeviceListener implements EMMultiDeviceListener {


        @Override
        public void onContactEvent(int event, String target, String ext) {

        }

        @Override
        public void onGroupEvent(int event, String groupId, List<String> usernames) {
            EMLog.i(TAG, "onGroupEvent event"+event);
            String message = null;
            switch (event) {
                case GROUP_CREATE:
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/"", /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_CREATE);

                    break;
                case GROUP_DESTROY:
                    removeTargetSystemMessage(groupId, EaseConstant.SYSTEM_MESSAGE_GROUP_ID);
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/"", /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_DESTROY);
                    message = EaseConstant.GROUP_CHANGE;

                    break;
                case GROUP_JOIN:
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/"", /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_JOIN);
                    message = EaseConstant.GROUP_CHANGE;

                    break;
                case GROUP_LEAVE:
                    removeTargetSystemMessage(groupId, EaseConstant.SYSTEM_MESSAGE_GROUP_ID);
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/"", /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_LEAVE);
                    message = EaseConstant.GROUP_CHANGE;

                    break;
                case GROUP_APPLY:
                    removeTargetSystemMessage(groupId, EaseConstant.SYSTEM_MESSAGE_GROUP_ID);
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/"", /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_APPLY);

                    break;
                case GROUP_APPLY_ACCEPT:
                    removeTargetSystemMessage(groupId, EaseConstant.SYSTEM_MESSAGE_GROUP_ID, usernames.get(0), EaseConstant.SYSTEM_MESSAGE_FROM);
                    // TODO: person, reason from ext
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/usernames.get(0), /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_APPLY_ACCEPT);

                    break;
                case GROUP_APPLY_DECLINE:
                    removeTargetSystemMessage(groupId, EaseConstant.SYSTEM_MESSAGE_GROUP_ID, usernames.get(0), EaseConstant.SYSTEM_MESSAGE_FROM);
                    // TODO: person, reason from ext
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/usernames.get(0), /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_APPLY_DECLINE);

                    break;
                case GROUP_INVITE:
                    // TODO: person, reason from ext
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/usernames.get(0), /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_INVITE);

                    break;
                case GROUP_INVITE_ACCEPT:
                    String st3 = context.getString(R.string.Invite_you_to_join_a_group_chat);
                    EMMessage msg = EMMessage.createReceiveMessage(EMMessage.Type.TXT);
                    msg.setChatType(EMMessage.ChatType.GroupChat);
                    // TODO: person, reason from ext
                    String from = "";
                    if (usernames != null && usernames.size() > 0) {
                        msg.setFrom(usernames.get(0));
                    }
                    msg.setTo(groupId);
                    msg.setMsgId(UUID.randomUUID().toString());
                    msg.setAttribute(EaseConstant.EM_NOTIFICATION_TYPE, true);
                    msg.addBody(new EMTextMessageBody(msg.getFrom() + " " +st3));
                    msg.setStatus(EMMessage.Status.SUCCESS);
                    // save invitation as messages
                    EMClient.getInstance().chatManager().saveMessage(msg);

                    removeTargetSystemMessage(groupId, EaseConstant.SYSTEM_MESSAGE_GROUP_ID);
                    // TODO: person, reason from ext
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/"", /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_INVITE_ACCEPT);
                    message = EaseConstant.GROUP_CHANGE;

                    break;
                case GROUP_INVITE_DECLINE:
                    removeTargetSystemMessage(groupId, EaseConstant.SYSTEM_MESSAGE_GROUP_ID);
                    // TODO: person, reason from ext
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/usernames.get(0), /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_INVITE_DECLINE);

                    showToast("GROUP_INVITE_DECLINE");
                    break;
                case GROUP_KICK:
                    // TODO: person, reason from ext
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/usernames.get(0), /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_KICK);
                    message = EaseConstant.GROUP_CHANGE;

                    break;
                case GROUP_BAN:
                    // TODO: person from ext
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/usernames.get(0), /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_BAN);
                    message = EaseConstant.GROUP_CHANGE;

                    break;
                case GROUP_ALLOW:
                    // TODO: person from ext
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/usernames.get(0), /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_ALLOW);

                    break;
                case GROUP_BLOCK:
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/"", /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_BLOCK);

                    break;
                case GROUP_UNBLOCK:
                    // TODO: person from ext
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/"", /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_UNBLOCK);

                    break;
                case GROUP_ASSIGN_OWNER:
                    // TODO: person from ext
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/usernames.get(0), /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_ASSIGN_OWNER);

                    break;
                case GROUP_ADD_ADMIN:
                    // TODO: person from ext
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/usernames.get(0), /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_ADD_ADMIN);
                    message = EaseConstant.GROUP_CHANGE;

                    break;
                case GROUP_REMOVE_ADMIN:
                    // TODO: person from ext
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/usernames.get(0), /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_REMOVE_ADMIN);
                    message = EaseConstant.GROUP_CHANGE;

                    break;
                case GROUP_ADD_MUTE:
                    // TODO: person from ext
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/usernames.get(0), /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_ADD_MUTE);

                    break;
                case GROUP_REMOVE_MUTE:
                    // TODO: person from ext
                    saveGroupNotification(groupId, /*groupName*/"",  /*person*/usernames.get(0), /*reason*/"", InviteMessageStatus.MULTI_DEVICE_GROUP_REMOVE_MUTE);

                    break;
                default:
                    break;
            }
            if(!TextUtils.isEmpty(message)) {
                EaseEvent easeEvent = EaseEvent.create(message, EaseEvent.TYPE.GROUP);
                messageChangeLiveData.with(message).postValue(easeEvent);
            }
        }
    }

    /**
     * 移除目标所有的消息记录，如果目标被删除
     * @param target
     */
    private void removeTargetSystemMessage(String target, String params) {
        EMConversation conversation = EaseSystemMsgManager.getInstance().getConversation();
        List<EMMessage> messages = conversation.getAllMessages();
        if(messages != null && !messages.isEmpty()) {
            for (EMMessage message : messages) {
                String from = null;
                try {
                    from = message.getStringAttribute(params);
                } catch (HyphenateException e) {
                    e.printStackTrace();
                }
                if(TextUtils.equals(from, target)) {
                    conversation.removeMessage(message.getMsgId());
                }
            }
        }
    }

    /**
     * 移除目标所有的消息记录，如果目标被删除
     * @param target1
     */
    private void removeTargetSystemMessage(String target1, String params1, String target2, String params2) {
        EMConversation conversation = EaseSystemMsgManager.getInstance().getConversation();
        List<EMMessage> messages = conversation.getAllMessages();
        if(messages != null && !messages.isEmpty()) {
            for (EMMessage message : messages) {
                String targetParams1 = null;
                String targetParams2 = null;
                try {
                    targetParams1 = message.getStringAttribute(params1);
                    targetParams2 = message.getStringAttribute(params2);
                } catch (HyphenateException e) {
                    e.printStackTrace();
                }
                if(TextUtils.equals(targetParams1, target1) && TextUtils.equals(targetParams2, target2)) {
                    conversation.removeMessage(message.getMsgId());
                }
            }
        }
    }


    private void notifyNewInviteMessage(EMMessage msg) {
        // notify there is new message
//        getNotifier().vibrateAndPlayTone(null);
    }

    private void updateContactNotificationStatus(String from, String reason, InviteMessageStatus status) {
        EMMessage msg = null;
        EMConversation conversation = EaseSystemMsgManager.getInstance().getConversation();
        List<EMMessage> allMessages = conversation.getAllMessages();
        if(allMessages != null && !allMessages.isEmpty()) {
            for (EMMessage message : allMessages) {
                Map<String, Object> ext = message.ext();
                if(ext != null && (ext.containsKey(EaseConstant.SYSTEM_MESSAGE_FROM)
                        && TextUtils.equals(from, (String)ext.get(EaseConstant.SYSTEM_MESSAGE_FROM)))) {
                    msg = message;
                }
            }
        }

        if (msg != null) {
            msg.setAttribute(EaseConstant.SYSTEM_MESSAGE_STATUS, status.name());
            EaseSystemMsgManager.getInstance().updateMessage(msg);
        } else {
            // save invitation as message
            Map<String, Object> ext = EaseSystemMsgManager.getInstance().createMsgExt();
            ext.put(EaseConstant.SYSTEM_MESSAGE_FROM, from);
            ext.put(EaseConstant.SYSTEM_MESSAGE_REASON, reason);
            ext.put(EaseConstant.SYSTEM_MESSAGE_STATUS, status.name());
            msg = EaseSystemMsgManager.getInstance().createMessage(PushAndMessageHelper.getSystemMessage(ext), ext);
            notifyNewInviteMessage(msg);
        }
    }

    private void saveGroupNotification(String groupId, String groupName, String inviter, String reason, InviteMessageStatus status) {
    }

    private class ChatRoomListener implements EMChatRoomChangeListener {

        @Override
        public void onChatRoomDestroyed(String roomId, String roomName) {
            setChatRoomEvent(roomId, EaseEvent.TYPE.CHAT_ROOM_LEAVE);
            EMLog.i(TAG, context.getString(R.string.demo_chat_room_listener_onChatRoomDestroyed, roomName));
        }

        @Override
        public void onMemberJoined(String roomId, String participant) {
            setChatRoomEvent(roomId, EaseEvent.TYPE.CHAT_ROOM);
            EMLog.i(TAG, context.getString(R.string.demo_chat_room_listener_onMemberJoined, participant));
        }

        @Override
        public void onMemberExited(String roomId, String roomName, String participant) {
            setChatRoomEvent(roomId, EaseEvent.TYPE.CHAT_ROOM);
            EMLog.i(TAG, context.getString(R.string.demo_chat_room_listener_onMemberExited, participant));
        }

        @Override
        public void onRemovedFromChatRoom(int reason, String roomId, String roomName, String participant) {
            if(TextUtils.equals(EaseIMHelper.getInstance().getCurrentUser(), participant)) {
                setChatRoomEvent(roomId, EaseEvent.TYPE.CHAT_ROOM);
                if(reason == EMAChatRoomManagerListener.BE_KICKED) {
                }else {
                    EMLog.i(TAG, context.getString(R.string.demo_chat_room_listener_onRemovedFromChatRoom, participant));
                }

            }
        }

        @Override
        public void onMuteListAdded(String chatRoomId, List<String> mutes, long expireTime) {
            setChatRoomEvent(chatRoomId, EaseEvent.TYPE.CHAT_ROOM);

            String content = getContentFromList(mutes);
            EMLog.i(TAG, context.getString(R.string.demo_chat_room_listener_onMuteListAdded, content));
        }

        @Override
        public void onMuteListRemoved(String chatRoomId, List<String> mutes) {
            setChatRoomEvent(chatRoomId, EaseEvent.TYPE.CHAT_ROOM);
            String content = getContentFromList(mutes);
            EMLog.i(TAG, context.getString(R.string.demo_chat_room_listener_onMuteListRemoved, content));
        }

        @Override
        public void onWhiteListAdded(String chatRoomId, List<String> whitelist) {
            String content = getContentFromList(whitelist);
            EMLog.i(TAG, context.getString(R.string.demo_chat_room_listener_onWhiteListAdded, content));
        }

        @Override
        public void onWhiteListRemoved(String chatRoomId, List<String> whitelist) {
            String content = getContentFromList(whitelist);
            EMLog.i(TAG, context.getString(R.string.demo_chat_room_listener_onWhiteListRemoved, content));
        }

        @Override
        public void onAllMemberMuteStateChanged(String chatRoomId, boolean isMuted) {
            EMLog.i(TAG, context.getString(isMuted ? R.string.demo_chat_room_listener_onAllMemberMuteStateChanged_mute
                    : R.string.demo_chat_room_listener_onAllMemberMuteStateChanged_note_mute));
        }

        @Override
        public void onAdminAdded(String chatRoomId, String admin) {
            setChatRoomEvent(chatRoomId, EaseEvent.TYPE.CHAT_ROOM);

            EMLog.i(TAG, context.getString(R.string.demo_chat_room_listener_onAdminAdded, admin));
        }

        @Override
        public void onAdminRemoved(String chatRoomId, String admin) {
            setChatRoomEvent(chatRoomId, EaseEvent.TYPE.CHAT_ROOM);

            EMLog.i(TAG, context.getString(R.string.demo_chat_room_listener_onAdminRemoved, admin));
        }

        @Override
        public void onOwnerChanged(String chatRoomId, String newOwner, String oldOwner) {
            setChatRoomEvent(chatRoomId, EaseEvent.TYPE.CHAT_ROOM);

            EMLog.i(TAG, context.getString(R.string.demo_chat_room_listener_onOwnerChanged, oldOwner, newOwner));
        }

        @Override
        public void onAnnouncementChanged(String chatRoomId, String announcement) {
            setChatRoomEvent(chatRoomId, EaseEvent.TYPE.CHAT_ROOM);
            EMLog.i(TAG, context.getString(R.string.demo_chat_room_listener_onAnnouncementChanged));
        }
    }

    private void setChatRoomEvent(String roomId, EaseEvent.TYPE type) {
        EaseEvent easeEvent = new EaseEvent(EaseConstant.CHAT_ROOM_CHANGE, type);
        easeEvent.message = roomId;
        messageChangeLiveData.with(EaseConstant.CHAT_ROOM_CHANGE).postValue(easeEvent);
    }

    private String getContentFromList(List<String> members) {
        StringBuilder sb = new StringBuilder();
        for (String member : members) {
            if(!TextUtils.isEmpty(sb.toString().trim())) {
                sb.append(",");
            }
            sb.append(member);
        }
        String content = sb.toString();
        if(content.contains(EMClient.getInstance().getCurrentUser())) {
            content = context.getString(R.string.you);
        }
        return content;
    }
}
