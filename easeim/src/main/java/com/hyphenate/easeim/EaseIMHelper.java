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
import com.hyphenate.chat.EMGroupManager;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.chat.EMPushManager;
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
import com.hyphenate.easeui.manager.EaseMessageTypeSetManager;
import com.hyphenate.easeui.model.EaseNotifier;
import com.hyphenate.easeui.provider.EaseSettingsProvider;
import com.hyphenate.easeui.provider.EaseUserProfileProvider;
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
 * 作为hyphenate-sdk的入口控制类，获取sdk下的基础类均通过此类
 */
public class EaseIMHelper {
    private static final String TAG = EaseIMHelper.class.getSimpleName();

    public boolean isSDKInit;//SDK是否初始化
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
    private String serverHost = "http://182.92.236.214:12005";

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

    public void init(Application application){
        this.application = application;
        easeModel = new EaseModel(application);
        Properties props = new Properties();
        try {
            InputStream inputStream = application.getAssets().open("config.properties");
            props.load(inputStream);
            miAppkey = props.getProperty("MI_PUSH_APPKEY");
            miAppId = props.getProperty("MI_PUSH_APPID");

            mzAppkey = props.getProperty("MEIZU_PUSH_APPKEY");
            mzAppId = props.getProperty("MEIZU_PUSH_APPID");

//            oppoAppkey = props.getProperty("OPPO_PUSH_APPKEY");
//            oppoAppSecret = props.getProperty("OPPO_PUSH_APPSECRET");

//            fcmSenderId = props.getProperty("FCM_SENDERID");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initChat(boolean isAdmin){
        if(application != null){
            this.isAdmin = isAdmin;
            //初始化IM SDK
            if(initSDK(application)) {
                clientRepository = new EMClientRepository();
                // debug mode, you'd better set it to false, if you want release your App officially.
                EMClient.getInstance().setDebugMode(true);
                // set Call options
                setCallOptions(application);
                //初始化推送
//                initPush(application);
                //初始化ease ui相关
                initEaseUI(application);
                //注册对话类型
                registerConversationType();

                //callKit初始化
                InitCallKit(application);

                //启动获取用户信息线程
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
     * callKit初始化
     * @param context
     */
    private void InitCallKit(Context context){
        EaseCallKitConfig callKitConfig = new EaseCallKitConfig();
        //设置呼叫超时时间
        callKitConfig.setCallTimeOut(30 * 1000);
        //设置声网AgoraAppId
        callKitConfig.setAgoraAppId("e305be556e0f4846b8ad2c3e21bc78ec");
        callKitConfig.setEnableRTCToken(true);
        EaseCallKit.getInstance().init(context,callKitConfig);
        // Register the activities which you have registered in manifest
        EaseCallKit.getInstance().registerVideoCallClass(VideoCallActivity.class);
        EaseCallKit.getInstance().registerMultipleVideoClass(MultipleVideoActivity.class);
        addCallkitListener();
    }

    /**
     * 初始化SDK
     * @param context
     * @return
     */
    private boolean initSDK(Context context) {
        // 根据项目需求对SDK进行配置
        EMOptions options = initChatOptions(context);
        // 初始化SDK
        isSDKInit = EaseIM.getInstance().init(context, options);
        //设置删除用户属性数据超时时间
        easeModel.setUserInfoTimeOut(30 * 60 * 1000);
        //更新过期用户属性列表
        updateTimeoutUsers();
        mainContext = context;
        return isSDKInit();
    }


    /**
     *注册对话类型
     */
    private void registerConversationType() {
        EaseMessageTypeSetManager.getInstance()
                .addMessageType(EaseFileAdapterDelegate.class)             //文件
                .addMessageType(EaseImageAdapterDelegate.class)            //图片
                .addMessageType(EaseLocationAdapterDelegate.class)         //定位
                .addMessageType(EaseVideoAdapterDelegate.class)            //视频
                .addMessageType(EaseVoiceAdapterDelegate.class)            //声音
                .addMessageType(ChatNoticeAdapterDelegate.class)           //提示
                .setDefaultMessageType(EaseTextAdapterDelegate.class);       //文本
    }

    /**
     * 判断是否之前登录过
     * @return
     */
    public boolean isLoggedIn() {
        return getEMClient().isLoggedInBefore();
    }

    /**
     * 获取IM SDK的入口类
     * @return
     */
    public EMClient getEMClient() {
        return EMClient.getInstance();
    }

    /**
     * 获取contact manager
     * @return
     */
    public EMContactManager getContactManager() {
        return getEMClient().contactManager();
    }

    /**
     * 获取group manager
     * @return
     */
    public EMGroupManager getGroupManager() {
        return getEMClient().groupManager();
    }

    /**
     * 获取chatroom manager
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
     * ChatPresenter中添加了网络连接状态监听，多端登录监听，群组监听，联系人监听，聊天室监听
     * @param context
     */
    private void initEaseUI(Context context) {
        //添加ChatPresenter,ChatPresenter中添加了网络连接状态监听，
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
     * 统一配置头像
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
            //找不到更新会话列表 继续查找
            updateContactList();
            user = getContactList().get(username);
            //如果还找不到从服务端异步拉取 然后通知UI刷新列表
            if(user == null){
                if(fetchUserInfoList != null){
                    fetchUserInfoList.addUserId(username);
                }
            }
        }
        return user;
    }


    /**
     * 根据自己的需要进行配置
     * @param context
     * @return
     */
    private EMOptions initChatOptions(Context context){
        Log.d(TAG, "init HuanXin Options");

        EMOptions options = new EMOptions();
        // 设置是否自动接受加好友邀请,默认是true
        options.setAcceptInvitationAlways(false);
        // 设置是否需要接受方已读确认
        options.setRequireAck(true);
        // 设置是否需要接受方送达确认,默认false
        options.setRequireDeliveryAck(false);
        //设置fpa开关，默认false
        options.setFpaEnable(true);

        /**
         * NOTE:你需要设置自己申请的账号来使用三方推送功能，详见集成文档
         */
        EMPushConfig.Builder builder = new EMPushConfig.Builder(context);

        builder.enableVivoPush(); // 需要在AndroidManifest.xml中配置appId和appKey
        if(miAppId != null && miAppkey != null && TextUtils.isEmpty(miAppId) && TextUtils.isEmpty(miAppkey)){
            builder.enableMiPush(miAppId, miAppkey);
        }
        if(mzAppId != null && mzAppkey != null && TextUtils.isEmpty(mzAppId) && TextUtils.isEmpty(mzAppkey)){
            builder.enableMeiZuPush(mzAppId, mzAppkey);
        }
        if(oppoAppkey != null && oppoAppSecret != null && TextUtils.isEmpty(oppoAppkey) && TextUtils.isEmpty(oppoAppSecret)){
            builder.enableOppoPush(oppoAppkey,
                    oppoAppSecret);
        }

        builder.enableHWPush(); // 需要在AndroidManifest.xml中配置appId
        options.setPushConfig(builder.build());

        return options;
    }

    private void setCallOptions(Context context) {
        HeadsetReceiver headsetReceiver = new HeadsetReceiver();
        IntentFilter headsetFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        context.registerReceiver(headsetReceiver, headsetFilter);
    }

    public void initPush(Context context) {
        if(EaseIM.getInstance().isMainProcess(context)) {
            //OPPO SDK升级到2.1.0后需要进行初始化
//            HeytapPushManager.init(context, true);
        }
    }

    /**
     * 退出登录后，需要处理的业务逻辑
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
     * 设置本地标记，是否自动登录
     * @param autoLogin
     */
    public void setAutoLogin(boolean autoLogin) {
        PreferenceManager.getInstance().setAutoLogin(autoLogin);
    }

    /**
     * 获取本地标记，是否自动登录
     * @return
     */
    public boolean getAutoLogin() {
        return PreferenceManager.getInstance().getAutoLogin();
    }

    /**
     * 设置SDK是否初始化
     * @param init
     */
    public void setSDKInit(boolean init) {
        isSDKInit = init;
    }

    public boolean isSDKInit() {
        return isSDKInit;
    }

    /**
     * 向数据库中插入数据
     * @param object
     */
    public void insert(Object object) {
        easeModel.insert(object);
    }

    /**
     * update
     * @param object
     */
    public void update(Object object) {
        easeModel.update(object);
    }

    /**
     * update user list
     * @param users
     */
    public void updateUserList(List<EaseUser> users){
        easeModel.updateContactList(users);
    }

    /**
     * 更新过期的用户属性数据
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
     * 展示通知设置页面
     */
    public void showNotificationPermissionDialog() {
        EMPushType pushType = EMPushHelper.getInstance().getPushType();
        // oppo
//        if(pushType == EMPushType.OPPOPUSH && HeytapPushManager.isSupportPush(mainContext)) {
//            HeytapPushManager.requestNotificationPermission();
//        }
    }

    /**
     * 删除联系人
     * @param username
     * @return
     */
    public synchronized int deleteContact(String username) {
        if(TextUtils.isEmpty(username)) {
            return 0;
        }
        EaseDbHelper helper = EaseDbHelper.getInstance(application);
        if(helper.getUserDao() == null) {
            return 0;
        }
        int num = helper.getUserDao().deleteUser(username);
        if(helper.getInviteMessageDao() != null) {
            helper.getInviteMessageDao().deleteByFrom(username);
        }
        EMClient.getInstance().chatManager().deleteConversation(username, false);
        getModel().deleteUsername(username, false);
        Log.e(TAG, "delete num = "+num);
        return num;
    }

    /**
     * 检查是否是第一次安装登录
     * 默认值是true, 需要在用api拉取完会话列表后，就其置为false.
     * @return
     */
    public boolean isFirstInstall() {
        return getModel().isFirstInstall();
    }

    /**
     * 将状态置为非第一次安装，在调用获取会话列表的api后调用
     * 并将会话列表是否来自服务器置为true
     */
    public void makeNotFirstInstall() {
        getModel().makeNotFirstInstall();
    }

    /**
     * 检查会话列表是否从服务器返回数据
     * @return
     */
    public boolean isConComeFromServer() {
        return getModel().isConComeFromServer();
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
     * 增加EaseCallkit监听
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
//                //获取声网Token
//                getRtcToken(url, callback);
                if(isAdmin()){
                    clientRepository.getRtcTokenWithAdmin(userId, channelName,callback);
                } else {
                    clientRepository.getRtcTokenWithCustomer(userId, channelName,callback);
                }
            }

            @Override
            public void onReceivedCall(EaseCallType callType, String fromUserId,JSONObject ext) {
                //收到接听电话
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
                    //设置用户昵称 头像
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
     * 获取声网Token
     *
     */
    private void getRtcToken(String tokenUrl,EaseCallKitTokenCallback callback){
        new AsyncTask<String, Void, Pair<Integer, String>>(){
            @Override
            protected Pair<Integer, String> doInBackground(String... str) {
                try {
                    Pair<Integer, String> response = EMHttpClient.getInstance().sendRequestWithToken(tokenUrl, null,EMHttpClient.GET);
                    return response;
                }catch (HyphenateException exception) {
                    exception.printStackTrace();
                }
                return  null;
            }
            @Override
            protected void onPostExecute(Pair<Integer, String> response) {
                if(response != null) {
                    try {
                          int resCode = response.first;
                          if(resCode == 200){
                              String responseInfo = response.second;
                              if(responseInfo != null && responseInfo.length() > 0){
                                  try {
                                      JSONObject object = new JSONObject(responseInfo);
                                      String token = object.getString("accessToken");
                                      int uId = object.getInt("agoraUserId");

                                      //设置自己头像昵称
                                      setEaseCallKitUserInfo(EMClient.getInstance().getCurrentUser());
                                      callback.onSetToken(token,uId);
                                  }catch (Exception e){
                                      e.getStackTrace();
                                  }
                              }else{
                                  callback.onGetTokenError(response.first,response.second);
                              }
                          }else{
                              callback.onGetTokenError(response.first,response.second);
                          }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else{
                    callback.onSetToken(null,0);
                }
            }
        }.execute(tokenUrl);
    }

    /**
     * 根据channelName和声网uId获取频道内所有人的UserId
     * @param uId
     * @param url
     * @param callback
     */
    private void getUserIdAgoraUid(int uId, String url, EaseGetUserAccountCallback callback){
        new AsyncTask<String, Void, Pair<Integer, String>>(){
            @Override
            protected Pair<Integer, String> doInBackground(String... str) {
                try {
                    Pair<Integer, String> response = EMHttpClient.getInstance().sendRequestWithToken(url, null,EMHttpClient.GET);
                    return response;
                }catch (HyphenateException exception) {
                    exception.printStackTrace();
                }
                return  null;
            }
            @Override
            protected void onPostExecute(Pair<Integer, String> response) {
                if(response != null) {
                    try {
                        int resCode = response.first;
                        if(resCode == 200){
                            String responseInfo = response.second;
                            List<EaseUserAccount> userAccounts = new ArrayList<>();
                            if(responseInfo != null && responseInfo.length() > 0){
                                try {
                                    JSONObject object = new JSONObject(responseInfo);
                                    JSONObject resToken = object.getJSONObject("result");
                                    Iterator it = resToken.keys();
                                    while(it.hasNext()) {
                                        String uIdStr = it.next().toString();
                                        int uid = 0;
                                        uid = Integer.valueOf(uIdStr).intValue();
                                        String username = resToken.optString(uIdStr);
                                        if(uid == uId){
                                            //获取到当前用户的userName 设置头像昵称等信息
                                            setEaseCallKitUserInfo(username);
                                        }
                                        userAccounts.add(new EaseUserAccount(uid, username));
                                    }
                                    callback.onUserAccount(userAccounts);
                                }catch (Exception e){
                                    e.getStackTrace();
                                }
                            }else{
                                callback.onSetUserAccountError(response.first,response.second);
                            }
                        }else{
                            callback.onSetUserAccountError(response.first,response.second);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else{
                    callback.onSetUserAccountError(100,"response is null");
                }
            }
        }.execute(url);
    }


    /**
     * 设置callKit 用户头像昵称
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
         * @param success true：data sync successful，false: failed to sync data
         */
        void onSyncComplete(boolean success);
    }

    public boolean isAdmin(){
        return isAdmin;
    }

    //登录
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

    //登出
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

    // 获取所有列表的未读数
    public void getChatUnread(EMValueCallBack<Map<String, Integer>> callBack){
        int totalUnread = 0;
        int exclusiveUnread = 0;
        int unread = 0;
        Map<String, EMConversation> map = EMClient.getInstance().chatManager().getAllConversations();
        if(isAdmin()){
            for(EMConversation conversation : map.values()){
                if(conversation.getType() == EMConversation.EMConversationType.GroupChat){
                    if(!getPushManager().getNoPushGroups().contains(conversation.conversationId())) {
                        totalUnread += conversation.getUnreadMsgCount();
                    }
                } else if(conversation.getType() == EMConversation.EMConversationType.Chat){
                    if(!getPushManager().getNoPushUsers().contains(conversation.conversationId())) {
                        totalUnread += conversation.getUnreadMsgCount();
                    }
                }
            }
        } else {
            for(EMConversation conversation : map.values()){
                if(isExclusiveGroup(conversation)){
                    if(!getPushManager().getNoPushGroups().contains(conversation.conversationId())){
                        exclusiveUnread += conversation.getUnreadMsgCount();
                        EMLog.e("testapi:", conversation.conversationId() + " = " + conversation.getUnreadMsgCount());
                    }
                } else {
                    if(conversation.getType() == EMConversation.EMConversationType.GroupChat){
                        if(!getPushManager().getNoPushGroups().contains(conversation.conversationId())) {
                            unread += conversation.getUnreadMsgCount();
                        }
                    } else if(conversation.getType() == EMConversation.EMConversationType.Chat){
                        if(!getPushManager().getNoPushUsers().contains(conversation.conversationId())) {
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
     * 启动聊天
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
     * 判断是否是专属群
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
     * 消息发送前统一设置ext
     * @param message
     */
    public void addMsgAttrsBeforeSend(EMMessage message){
        try {
            JSONObject userInfo = new JSONObject();
            EaseUser user = EaseIMHelper.getInstance().getUserProfileManager().getCurrentUserInfo();
            userInfo.put(EaseConstant.MESSAGE_ATTR_USER_NAME, user.getUsername());
            userInfo.put(EaseConstant.MESSAGE_ATTR_USER_NICK, user.getNickname());
            userInfo.put(EaseConstant.MESSAGE_ATTR_USER_AVATAR, user.getAvatar());
            message.setAttribute(EaseConstant.MESSAGE_ATTR_USER_INFO, userInfo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setServerHost(String host){
        serverHost = host;
    }

    /**
     * 设置极狐 aid
     * @param aid
     */
    public void setAid(String aid){
        getModel().setAid(aid);
    }

    /**
     * 设置极狐 token
     * @param token
     */
    public void setAidToken(String token){
        getModel().setAidToken(token);
    }
}
