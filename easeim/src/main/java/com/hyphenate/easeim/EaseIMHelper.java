package com.hyphenate.easeim;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.hyphenate.EMCallBack;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMChatManager;
import com.hyphenate.chat.EMChatRoomManager;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMContactManager;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMGroupManager;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.chat.EMPushManager;
import com.hyphenate.chat.EMUserInfo;
import com.hyphenate.cloud.EMHttpClient;
import com.hyphenate.easecallkit.EaseCallKit;
import com.hyphenate.easecallkit.base.EaseCallEndReason;
import com.hyphenate.easecallkit.base.EaseCallKitConfig;
import com.hyphenate.easecallkit.base.EaseCallKitListener;
import com.hyphenate.easecallkit.base.EaseCallKitTokenCallback;
import com.hyphenate.easecallkit.base.EaseCallType;
import com.hyphenate.easecallkit.base.EaseCallUserInfo;
import com.hyphenate.easecallkit.base.EaseGetUserAccountCallback;
import com.hyphenate.easecallkit.base.EaseUserAccount;
import com.hyphenate.easecallkit.event.CallCancelEvent;
import com.hyphenate.easecallkit.utils.EaseCallState;
import com.hyphenate.easeim.common.db.EaseDbHelper;
import com.hyphenate.easeim.common.interfaceOrImplement.UserActivityLifecycleCallbacks;
import com.hyphenate.easeim.common.livedatas.LiveDataBus;
import com.hyphenate.easeim.common.manager.UserProfileManager;
import com.hyphenate.easeim.common.model.EaseModel;
import com.hyphenate.easeim.common.receiver.HeadsetReceiver;
import com.hyphenate.easeim.common.repositories.EMClientRepository;
import com.hyphenate.easeim.common.utils.FetchUserInfoList;
import com.hyphenate.easeim.common.utils.FetchUserRunnable;
import com.hyphenate.easeim.common.utils.PreferenceManager;
import com.hyphenate.easeim.section.av.MultipleVideoActivity;
import com.hyphenate.easeim.section.av.VideoCallActivity;
import com.hyphenate.easeim.section.chat.ChatPresenter;
import com.hyphenate.easeim.section.chat.activity.ChatActivity;
import com.hyphenate.easeim.section.chat.delegates.ChatNoticeAdapterDelegate;
import com.hyphenate.easeim.section.conference.ConferenceInviteActivity;
import com.hyphenate.easeim.section.conversation.ConversationListActivity;
import com.hyphenate.easeui.EaseIM;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.delegate.EaseFileAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseImageAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseLocationAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseTextAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseVideoAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseVoiceAdapterDelegate;
import com.hyphenate.easeui.domain.EaseAvatarOptions;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.manager.EaseAtMessageHelper;
import com.hyphenate.easeui.manager.EaseMessageTypeSetManager;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.easeui.model.EaseNotifier;
import com.hyphenate.easeui.provider.EaseSettingsProvider;
import com.hyphenate.easeui.provider.EaseUserProfileProvider;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.push.EMPushConfig;
import com.hyphenate.push.EMPushHelper;
import com.hyphenate.push.EMPushType;
import com.hyphenate.util.EMLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

/**
 * ??????hyphenate-sdk???????????????????????????sdk??????????????????????????????
 */
public class EaseIMHelper {
    private static final String TAG = EaseIMHelper.class.getSimpleName();

    public boolean isSDKInit;//SDK???????????????
    private static EaseIMHelper mInstance;
    private EaseModel easeModel = null;
    private Map<String, EaseUser> contactList;
    private UserProfileManager userProManager;

    private EaseCallKitListener callKitListener;
    private Context mainContext;

    private String tokenUrl = "http://a1.easemob.com/token/rtcToken/v1";
    private String uIdUrl = "http://a1.easemob.com/channel/mapper";
    
    private FetchUserRunnable fetchUserRunnable;
    private Thread fetchUserTread;
    private FetchUserInfoList fetchUserInfoList;
    private boolean isAdmin = false;
    private String chatPageConId = "";
    private Application application;
    private UserActivityLifecycleCallbacks mLifecycleCallbacks = new UserActivityLifecycleCallbacks();
    private EMClientRepository clientRepository;
    private String serverHost = "http://182.92.236.214:12005/";

    private String miAppkey;
    private String miAppId;
    private String mzAppkey;
    private String mzAppId;
    private String oppoAppkey;
    private String oppoAppSecret;
    private String fcmSenderId;

    private EaseIMHelper() {}

    public static EaseIMHelper getInstance() {
        if(mInstance == null) {
            synchronized (EaseIMHelper.class) {
                if(mInstance == null) {
                    mInstance = new EaseIMHelper();
                }
            }
        }
        return mInstance;
    }

    public Application getApplication(){
        return application;
    }

    private void registerActivityLifecycleCallbacks() {
        application.registerActivityLifecycleCallbacks(mLifecycleCallbacks);
    }

    public UserActivityLifecycleCallbacks getLifecycleCallbacks() {
        return mLifecycleCallbacks;
    }

    public String getServerHost(){
        return serverHost;
    }

    public void init(Application application, String host){
        this.application = application;
        easeModel = new EaseModel(application);
        if(!TextUtils.isEmpty(host)){
            serverHost = host;
        }
//        Properties props = new Properties();
//        try {
//            InputStream inputStream = application.getAssets().open("config.properties");
//            props.load(inputStream);
//            miAppkey = props.getProperty("MI_PUSH_APPKEY");
//            miAppId = props.getProperty("MI_PUSH_APPID");
//
//            mzAppkey = props.getProperty("MEIZU_PUSH_APPKEY");
//            mzAppId = props.getProperty("MEIZU_PUSH_APPID");

//            oppoAppkey = props.getProperty("OPPO_PUSH_APPKEY");
//            oppoAppSecret = props.getProperty("OPPO_PUSH_APPSECRET");

//            fcmSenderId = props.getProperty("FCM_SENDERID");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public void initChat(boolean isAdmin){
        if(application != null){
            this.isAdmin = isAdmin;
            //?????????IM SDK
            if(initSDK(application)) {
                clientRepository = new EMClientRepository();
                // debug mode, you'd better set it to false, if you want release your App officially.
                EMClient.getInstance().setDebugMode(true);
                // set Call options
                setCallOptions(application);
                //???????????????
//                initPush(application);
                //callKit?????????
                InitCallKit(application);
                //?????????ease ui??????
                initEaseUI(application);
                //??????????????????
                registerConversationType();

                //??????????????????????????????
                fetchUserInfoList = FetchUserInfoList.getInstance();
                fetchUserRunnable = new FetchUserRunnable();
                fetchUserTread = new Thread(fetchUserRunnable);
                fetchUserTread.start();

                registerActivityLifecycleCallbacks();
            }
        } else {
            throw new NullPointerException("Please init first");
        }
    }


    /**
     * callKit?????????
     * @param context
     */
    private void InitCallKit(Context context){
        EaseCallKitConfig callKitConfig = new EaseCallKitConfig();
        //????????????????????????
        callKitConfig.setCallTimeOut(30 * 1000);
        //????????????AgoraAppId
        callKitConfig.setAgoraAppId("943bfefbbfb54b3cac36507a1b006a9f");
        callKitConfig.setEnableRTCToken(true);
        EaseCallKit.getInstance().init(context,callKitConfig);
        // Register the activities which you have registered in manifest
        EaseCallKit.getInstance().registerVideoCallClass(VideoCallActivity.class);
        EaseCallKit.getInstance().registerMultipleVideoClass(MultipleVideoActivity.class);
        addCallkitListener();
    }

    /**
     * ?????????SDK
     * @param context
     * @return
     */
    private boolean initSDK(Context context) {
        // ?????????????????????SDK????????????
        EMOptions options = initChatOptions(context);
        // ?????????SDK
        isSDKInit = EaseIM.getInstance().init(context, options);
        //??????????????????????????????????????????
        easeModel.setUserInfoTimeOut(30 * 60 * 1000);
        //??????????????????????????????
        updateTimeoutUsers();
        mainContext = context;
        return isSDKInit();
    }


    /**
     *??????????????????
     */
    private void registerConversationType() {
        EaseMessageTypeSetManager.getInstance()
                .addMessageType(EaseFileAdapterDelegate.class)             //??????
                .addMessageType(EaseImageAdapterDelegate.class)            //??????
                .addMessageType(EaseLocationAdapterDelegate.class)         //??????
                .addMessageType(EaseVideoAdapterDelegate.class)            //??????
                .addMessageType(EaseVoiceAdapterDelegate.class)            //??????
                .addMessageType(ChatNoticeAdapterDelegate.class)           //??????
                .setDefaultMessageType(EaseTextAdapterDelegate.class);       //??????
    }

    /**
     * ???????????????????????????
     * @return
     */
    public boolean isLoggedIn() {
        return getEMClient().isLoggedInBefore();
    }

    /**
     * ??????IM SDK????????????
     * @return
     */
    public EMClient getEMClient() {
        return EMClient.getInstance();
    }

    /**
     * ??????contact manager
     * @return
     */
    public EMContactManager getContactManager() {
        return getEMClient().contactManager();
    }

    /**
     * ??????group manager
     * @return
     */
    public EMGroupManager getGroupManager() {
        return getEMClient().groupManager();
    }

    /**
     * ??????chatroom manager
     * @return
     */
    public EMChatRoomManager getChatroomManager() {
        return getEMClient().chatroomManager();
    }


    /**
     * get EMChatManager
     * @return
     */
    public EMChatManager getChatManager() {
        return getEMClient().chatManager();
    }

    /**
     * get push manager
     * @return
     */
    public EMPushManager getPushManager() {
        return getEMClient().pushManager();
    }

    /**
     * get conversation
     * @param username
     * @param type
     * @param createIfNotExists
     * @return
     */
    public EMConversation getConversation(String username, EMConversation.EMConversationType type, boolean createIfNotExists) {
        return getChatManager().getConversation(username, type, createIfNotExists);
    }

    public String getCurrentUser() {
        return getEMClient().getCurrentUser();
    }

    /**
     * ChatPresenter????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * @param context
     */
    private void initEaseUI(Context context) {
        //??????ChatPresenter,ChatPresenter???????????????????????????????????????
        EaseIM.getInstance().addChatPresenter(ChatPresenter.getInstance());
        EaseIM.getInstance()
                .setAvatarOptions(getAvatarOptions())
                .setUserProvider(new EaseUserProfileProvider() {
                    @Override
                    public EaseUser getUser(String username) {
                        return getUserInfo(username);
                    }

                });
    }

    /**
     * ??????????????????
     * @return
     */
    private EaseAvatarOptions getAvatarOptions() {
        EaseAvatarOptions avatarOptions = new EaseAvatarOptions();
        avatarOptions.setAvatarShape(1);
        return avatarOptions;
    }

    public EaseUser getUserInfo(String username) {
        // To get instance of EaseUser, here we get it from the user list in memory
        // You'd better cache it if you get it from your server
        EaseUser user = null;
        if(TextUtils.equals(username, EMClient.getInstance().getCurrentUser()))
            return getUserProfileManager().getCurrentUserInfo();
        user = getContactList().get(username);
        if(user == null){
            //??????????????????????????? ????????????
            updateContactList();
            user = getContactList().get(username);
            //?????????????????????????????????????????? ????????????UI????????????
            if(user == null){
                if(fetchUserInfoList != null){
                    fetchUserInfoList.addUserId(username);
                }
            }
        }
        return user;
    }


    /**
     * ?????????????????????????????????
     * @param context
     * @return
     */
    private EMOptions initChatOptions(Context context){
        Log.d(TAG, "init HuanXin Options");

        EMOptions options = new EMOptions();
        // ???????????????????????????????????????,?????????true
        options.setAcceptInvitationAlways(false);
        // ???????????????????????????????????????
        options.setRequireAck(true);
        // ???????????????????????????????????????,??????false
        options.setRequireDeliveryAck(false);
        //??????fpa???????????????false
        options.setFpaEnable(true);

        /**
         * NOTE:????????????????????????????????????????????????????????????????????????????????????
         */
//        EMPushConfig.Builder builder = new EMPushConfig.Builder(context);
//
//        builder.enableVivoPush(); // ?????????AndroidManifest.xml?????????appId???appKey
//        if(miAppId != null && miAppkey != null && !TextUtils.isEmpty(miAppId) && !TextUtils.isEmpty(miAppkey)){
//            builder.enableMiPush(miAppId, miAppkey);
//        }
//        if(mzAppId != null && mzAppkey != null && !TextUtils.isEmpty(mzAppId) && !TextUtils.isEmpty(mzAppkey)){
//            builder.enableMeiZuPush(mzAppId, mzAppkey);
//        }
//        if(oppoAppkey != null && oppoAppSecret != null && !TextUtils.isEmpty(oppoAppkey) && !TextUtils.isEmpty(oppoAppSecret)){
//            builder.enableOppoPush(oppoAppkey,
//                    oppoAppSecret);
//        }
//
//        builder.enableHWPush(); // ?????????AndroidManifest.xml?????????appId
//        options.setPushConfig(builder.build());

        return options;
    }

    private void setCallOptions(Context context) {
        HeadsetReceiver headsetReceiver = new HeadsetReceiver();
        IntentFilter headsetFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        context.registerReceiver(headsetReceiver, headsetFilter);
    }

    public void initPush(Context context) {
        if(EaseIM.getInstance().isMainProcess(context)) {
            //OPPO SDK?????????2.1.0????????????????????????
//            HeytapPushManager.init(context, true);
        }
    }

    /**
     * ?????????????????????????????????????????????
     */
    public void logoutSuccess() {
        Log.d(TAG, "logout: onSuccess");
        setAutoLogin(false);
        EaseDbHelper.getInstance(application).closeDb();
        getUserProfileManager().reset();
        getModel().reset();
    }

    public EaseAvatarOptions getEaseAvatarOptions() {
        return EaseIM.getInstance().getAvatarOptions();
    }

    public EaseModel getModel(){
        if(easeModel == null) {
            easeModel = new EaseModel(mainContext);
        }
        return easeModel;
    }

    public String getCurrentLoginUser() {
        return getModel().getCurrentUsername();
    }

    /**
     * get instance of EaseNotifier
     * @return
     */
    public EaseNotifier getNotifier(){
        return EaseIM.getInstance().getNotifier();
    }

    /**
     * ???????????????????????????????????????
     * @param autoLogin
     */
    public void setAutoLogin(boolean autoLogin) {
        PreferenceManager.getInstance().setAutoLogin(autoLogin);
    }

    /**
     * ???????????????????????????????????????
     * @return
     */
    public boolean getAutoLogin() {
        return PreferenceManager.getInstance().getAutoLogin();
    }

    /**
     * ??????SDK???????????????
     * @param init
     */
    public void setSDKInit(boolean init) {
        isSDKInit = init;
    }

    public boolean isSDKInit() {
        return isSDKInit;
    }

    /**
     * update user list
     * @param users
     */
    public void updateUserList(List<EaseUser> users){
        easeModel.updateContactList(users);
    }

    /**
     * ?????????????????????????????????
     */
    public void updateTimeoutUsers() {
        List<String> userIds = easeModel.selectTimeOutUsers();
        if(userIds != null && userIds.size() > 0){
            if(fetchUserInfoList != null){
                for(int i = 0; i < userIds.size(); i++){
                    fetchUserInfoList.addUserId(userIds.get(i));
                }
            }
        }
    }

    /**
     * get contact list
     *
     * @return
     */
    public Map<String, EaseUser> getContactList() {
        if (isLoggedIn() && contactList == null) {
            updateTimeoutUsers();
            contactList = easeModel.getAllUserList();
        }

        // return a empty non-null object to avoid app crash
        if(contactList == null){
            return new Hashtable<String, EaseUser>();
        }
        return contactList;
    }

    /**
     * update contact list
     */
    public void updateContactList() {
        if(isLoggedIn()) {
            updateTimeoutUsers();
            contactList = easeModel.getContactList();
        }
    }

    public UserProfileManager getUserProfileManager() {
        if (userProManager == null) {
            userProManager = new UserProfileManager();
        }
        return userProManager;
    }

    /**
     * ????????????????????????
     */
    public void showNotificationPermissionDialog() {
        EMPushType pushType = EMPushHelper.getInstance().getPushType();
        // oppo
//        if(pushType == EMPushType.OPPOPUSH && HeytapPushManager.isSupportPush(mainContext)) {
//            HeytapPushManager.requestNotificationPermission();
//        }
    }

    /**
     * ????????????????????????????????????
     * ????????????true, ????????????api???????????????????????????????????????false.
     * @return
     */
    public boolean isFirstInstall() {
        return getModel().isFirstInstall();
    }

    /**
     * ??????????????????????????????????????????????????????????????????api?????????
     * ?????????????????????????????????????????????true
     */
    public void makeNotFirstInstall() {
        getModel().makeNotFirstInstall();
    }

    /**
     * Determine if it is from the current user account of another device
     * @param username
     * @return
     */
    public boolean isCurrentUserFromOtherDevice(String username) {
        if(TextUtils.isEmpty(username)) {
            return false;
        }
        if(username.contains("/") && username.contains(EMClient.getInstance().getCurrentUser())) {
            return true;
        }
        return false;
    }


    /**
     * ??????EaseCallkit??????
     *
     */
    public void addCallkitListener(){
        callKitListener = new EaseCallKitListener() {
            @Override
            public void onInviteUsers(Context context,String userId[],JSONObject ext) {
                Intent intent = new Intent(context, ConferenceInviteActivity.class).addFlags(FLAG_ACTIVITY_NEW_TASK);
                String groupId = null;
                if(ext != null && ext.length() > 0){
                    try {
                        groupId = ext.getString("groupId");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                intent.putExtra(EaseConstant.EXTRA_CONFERENCE_GROUP_ID, groupId);
                intent.putExtra(EaseConstant.EXTRA_CONFERENCE_GROUP_EXIST_MEMBERS, userId);
                context.startActivity(intent);
            }

            @Override
            public void onEndCallWithReason(EaseCallType callType, String channelName, EaseCallEndReason reason, long callTime) {
                EMLog.d(TAG,"onEndCallWithReason" + (callType != null ? callType.name() : " callType is null ") + " reason:" + reason + " time:"+ callTime);
                SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                String callString = mainContext.getString(R.string.call_duration);
                callString += formatter.format(callTime);

//                Toast.makeText(mainContext,callString,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onGenerateToken(String userId, String channelName, String appKey, EaseCallKitTokenCallback callback){
                EMLog.d(TAG,"onGenerateToken userId:" + userId + " channelName:" + channelName + " appKey:"+ appKey);
//                String url = tokenUrl;
//                url += "?";
//                url += "userAccount=";
//                url += userId;
//                url += "&channelName=";
//                url += channelName;
//                url += "&appkey=";
//                url +=  appKey;
//
//                //????????????Token
//                getRtcToken(url, callback);
                if(isAdmin()){
                    clientRepository.getRtcTokenWithAdmin(userId, channelName,callback);
                } else {
                    clientRepository.getRtcTokenWithCustomer(userId, channelName,callback);
                }
            }

            @Override
            public void onReceivedCall(EaseCallType callType, String fromUserId,JSONObject ext) {
                //??????????????????
                EMLog.d(TAG,"onRecivedCall" + callType.name() + " fromUserId:" + fromUserId);
            }
            @Override
            public  void onCallError(EaseCallKit.EaseCallError type, int errorCode, String description){

            }

            @Override
            public void onInViteCallMessageSent(){
//                LiveDataBus.get().with(EaseConstant.MESSAGE_CHANGE_CHANGE).postValue(new EaseEvent(EaseConstant.MESSAGE_CHANGE_CHANGE, EaseEvent.TYPE.MESSAGE));
            }

            @Override
            public void onRemoteUserJoinChannel(String channelName, String userName, int uid, EaseGetUserAccountCallback callback){
                if(userName == null || userName == ""){
//                    String url = uIdUrl;
//                    url += "?";
//                    url += "channelName=";
//                    url += channelName;
//                    url += "&userAccount=";
//                    url += EMClient.getInstance().getCurrentUser();
//                    url += "&appkey=";
//                    url +=  EMClient.getInstance().getOptions().getAppKey();
//                    getUserIdAgoraUid(uid,url,callback);
                    if(isAdmin()){
                        clientRepository.getAgoraUidWithAdmin(uid, channelName, callback);
                    } else {
                        clientRepository.getAgoraUidWithCustomer(uid, channelName, callback);
                    }

                }else{
                    //?????????????????? ??????
                    setEaseCallKitUserInfo(userName);
                    EaseUserAccount account = new EaseUserAccount(uid,userName);
                    List<EaseUserAccount> accounts = new ArrayList<>();
                    accounts.add(account);
                    callback.onUserAccount(accounts);
                }
            }
        };
        EaseCallKit.getInstance().setCallKitListener(callKitListener);
    }

    /**
     * ??????callKit ??????????????????
     * @param userName
     */
    public void setEaseCallKitUserInfo(String userName){
        EaseUser user = getUserInfo(userName);
        EaseCallUserInfo userInfo = new EaseCallUserInfo();
        if(user != null){
            userInfo.setNickName(user.getNickname());
            userInfo.setHeadImage(user.getAvatar());
        }
        EaseCallKit.getInstance().getCallKitConfig().setUserInfo(userName,userInfo);
    }

    public String getChatPageConId() {
        return chatPageConId;
    }

    public void setChatPageConId(String chatPageConId) {
        this.chatPageConId = chatPageConId;
    }


    /**
     * data sync listener
     */
    public interface DataSyncListener {
        /**
         * sync complete
         * @param success true???data sync successful???false: failed to sync data
         */
        void onSyncComplete(boolean success);
    }

    public boolean isAdmin(){
        return isAdmin;
    }

    //??????
    public void loginChat(String username, String password, EMCallBack callBack){
        if(isAdmin){
            clientRepository.loginWithAdmin(username, password, new EMCallBack() {
                @Override
                public void onSuccess() {
                    if(fetchUserTread != null && fetchUserRunnable != null){
                        fetchUserRunnable.setStop(false);
                    }
                    callBack.onSuccess();
                }

                @Override
                public void onError(int i, String s) {
                    callBack.onError(i,s);
                }
            });
        } else {
            if(getAutoLogin()){
                loginSuccess();
                callBack.onSuccess();
            } else {
                clientRepository.loginWithCustomer(username, password, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        if(fetchUserTread != null && fetchUserRunnable != null){
                            fetchUserRunnable.setStop(false);
                        }
                        callBack.onSuccess();
                    }

                    @Override
                    public void onError(int i, String s) {
                        callBack.onError(i,s);
                    }
                });
            }

        }
    }

    //??????
    public void logoutChat(EMCallBack callBack){
        if(fetchUserTread != null && fetchUserRunnable != null){
            fetchUserRunnable.setStop(true);
        }
        if(EaseCallKit.getInstance().getCallState() == EaseCallState.CALL_ANSWERED){
            CallCancelEvent cancelEvent = new CallCancelEvent();
            EaseCallKit.getInstance().sendCmdMsg(cancelEvent, EaseCallKit.getInstance().getFromUserId(), new EMCallBack() {
                @Override
                public void onSuccess() {
                    clientRepository.logout(callBack);
                }

                @Override
                public void onError(int code, String error) {
                    clientRepository.logout(callBack);
                }

                @Override
                public void onProgress(int progress, String status) {

                }
            });
        } else {
            clientRepository.logout(callBack);
        }
    }

    public void loginSuccess(){
        clientRepository.loginSuccess();
    }

    // ??????????????????????????????
    public void getChatUnread(EMValueCallBack<Map<String, Integer>> callBack){
        int totalUnread = 0;
        int exclusiveUnread = 0;
        int unread = 0;
        Map<String, EMConversation> map = EMClient.getInstance().chatManager().getAllConversations();
        List<String> noPushGroupIds = EaseIMHelper.getInstance().getPushManager().getNoPushGroups();
        List<String> noPushUserIds = EaseIMHelper.getInstance().getPushManager().getNoPushUsers();
        if(isAdmin()){
            for(EMConversation conversation : map.values()){
                if(conversation.getType() == EMConversation.EMConversationType.GroupChat){
                    if(noPushGroupIds != null){
                        if(!noPushGroupIds.contains(conversation.conversationId())){
                            totalUnread += conversation.getUnreadMsgCount();
                        }
                    } else {
                        totalUnread += conversation.getUnreadMsgCount();
                    }
                } else if(conversation.getType() == EMConversation.EMConversationType.Chat){
                    if(noPushUserIds != null) {
                        if(!noPushUserIds.contains(conversation.conversationId())){
                            totalUnread += conversation.getUnreadMsgCount();
                        }
                    } else {
                        totalUnread += conversation.getUnreadMsgCount();
                    }
                }
            }
        } else {
            for(EMConversation conversation : map.values()){
                if(isExclusiveGroup(conversation)){
                    if(noPushGroupIds != null){
                        if(!noPushGroupIds.contains(conversation.conversationId())){
                            exclusiveUnread += conversation.getUnreadMsgCount();
                        }
                    } else {
                        exclusiveUnread += conversation.getUnreadMsgCount();
                    }
                } else {
                    if(conversation.getType() == EMConversation.EMConversationType.GroupChat){
                        if(noPushGroupIds != null) {
                            if(!noPushGroupIds.contains(conversation.conversationId())){
                                unread += conversation.getUnreadMsgCount();
                            }
                        } else {
                            unread += conversation.getUnreadMsgCount();
                        }
                    } else if(conversation.getType() == EMConversation.EMConversationType.Chat){
                        if(noPushUserIds != null) {
                            if(!noPushUserIds.contains(conversation.conversationId())){
                                unread += conversation.getUnreadMsgCount();
                            }
                        } else {
                            unread += conversation.getUnreadMsgCount();
                        }
                    }
                }
            }
        }

        Map<String, Integer> result = new HashMap<>();
        result.put(EaseConstant.UNREAD_TOTAL, totalUnread);
        result.put(EaseConstant.UNREAD_EXCLUSIVE_GROUP, exclusiveUnread);
        result.put(EaseConstant.UNREAD_MY_CHAT, unread);
        callBack.onSuccess(result);
    }

    /**
     * ????????????
     * @param context
     * @param conversationType
     */
    public void startChat(Context context, int conversationType){
        if(isAdmin()){
            ConversationListActivity.actionStart(context, EaseConstant.CON_TYPE_ADMIN);
        } else {
            if(conversationType == EaseConstant.CON_TYPE_EXCLUSIVE){
                List<String> idList = new ArrayList<>();
                String serviceGroupJson = getModel().getServiceGroup();
                if(!TextUtils.isEmpty(serviceGroupJson)){
                    try {
                        JSONObject json = new JSONObject(serviceGroupJson);
                        for (Iterator<String> it = json.keys(); it.hasNext(); ) {
                            String groupId = it.next();
                            idList.add(groupId);
                            EMConversation conversation = EMClient.getInstance().chatManager().getConversation(groupId, EMConversation.EMConversationType.GroupChat, true);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if(idList.size() == 1){
                    ChatActivity.actionStart(context, idList.get(0), EaseConstant.CHATTYPE_GROUP);
                } else {
                    ConversationListActivity.actionStart(context, conversationType);
                }

                clientRepository.getServiceGroups(new EMCallBack() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onError(int i, String s) {

                    }
                });
            } else {
                ConversationListActivity.actionStart(context, conversationType);
            }
        }
    }

    /**
     * ????????????????????????
     * @param conversation
     * @return
     */
    public boolean isExclusiveGroup(EMConversation conversation){
        if(conversation.getType() == EMConversation.EMConversationType.GroupChat){
            String serviceGroup = getModel().getServiceGroup();
            if(!TextUtils.isEmpty(serviceGroup)){
                try {
                    JSONObject json = new JSONObject(serviceGroup);
                    String groupName = json.optString(conversation.conversationId());
                    if(!TextUtils.isEmpty(groupName)){
                        return true;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * ???????????????????????????ext
     * @param message
     */
    public void addMsgAttrsBeforeSend(EMMessage message){
        try {
            JSONObject userInfo = new JSONObject();
            EaseUser user = EaseIMHelper.getInstance().getUserProfileManager().getCurrentUserInfo();
            userInfo.put(EaseConstant.MESSAGE_ATTR_USER_NAME, user.getUsername());
            userInfo.put(EaseConstant.MESSAGE_ATTR_USER_NICK, user.getNickname());
            userInfo.put(EaseConstant.MESSAGE_ATTR_USER_AVATAR, user.getAvatar() != null ? user.getAvatar() : "");
            message.setAttribute(EaseConstant.MESSAGE_ATTR_USER_INFO, userInfo);

            String title = "?????????";
            String content = "???????????????";
            if(message.getChatType() == EMMessage.ChatType.Chat){
                title = user.getNickname();
                content = EaseCommonUtils.getMessageDigest(message, application, false);
            } else if(message.getChatType() == EMMessage.ChatType.GroupChat){
                EMGroup group = getGroupManager().getGroup(message.getTo());
                if(group != null){
                    title = group.getGroupName();
                }
                content = EaseCommonUtils.getMessageDigest(message, application, true);
            }
            JSONObject pushExt = new JSONObject();
            pushExt.put("em_push_title", title);
            pushExt.put("em_push_content", content);
            pushExt.put("em_alert_title", title);
            pushExt.put("em_alert_body", content);
            message.setAttribute("em_apns_ext", pushExt);
            if(EaseAtMessageHelper.get().isAtMessage(message)){
                message.setAttribute("em_force_notification", true);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * ???????????? aid
     * @param aid
     */
    public void setAid(String aid){
        getModel().setAid(aid);
    }

    /**
     * ???????????? token
     * @param token
     */
    public void setAidToken(String token){
        getModel().setAidToken(token);
    }

    /**
     * ??????????????????
     * @param notifierName
     * @param deviceToken
     */
    public void bindDeviceToken(String notifierName, String deviceToken){
        getModel().setDeviceToken(notifierName, deviceToken);
        if(EMClient.getInstance().isSdkInited() && EMClient.getInstance().isLoggedIn()){
            EMClient.getInstance().pushManager().bindDeviceToken(notifierName, deviceToken, new EMCallBack() {
                @Override
                public void onSuccess() {
                    EMLog.e("EMPushManager", "bindDeviceToken success");
                }

                @Override
                public void onError(int i, String s) {
                    EMLog.e("EMPushManager", "bindDeviceToken failed: " + i + ", " +s);
                }
            });
        }
    }
}
