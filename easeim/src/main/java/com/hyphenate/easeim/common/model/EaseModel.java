package com.hyphenate.easeim.common.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.hyphenate.chat.EMChatRoom;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.common.db.EaseDbHelper;
import com.hyphenate.easeim.common.db.dao.AppKeyDao;
import com.hyphenate.easeim.common.db.dao.EmUserDao;
import com.hyphenate.easeim.common.db.entity.AppKeyEntity;
import com.hyphenate.easeim.common.db.entity.EmUserEntity;
import com.hyphenate.easeim.common.db.entity.InviteMessage;
import com.hyphenate.easeim.common.db.entity.MsgTypeManageEntity;
import com.hyphenate.easeim.common.manager.OptionsHelper;
import com.hyphenate.easeim.common.utils.PreferenceManager;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.manager.EasePreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DemoModel主要用于SP存取及一些数据库的存取
 */
public class EaseModel {
    EmUserDao dao = null;
    protected Context context = null;
    protected Map<Key,Object> valueCache = new HashMap<Key,Object>();
    public List<EMChatRoom> chatRooms;

    //用户属性数据过期时间设置
    public static long userInfoTimeOut =  7 * 24 * 60 * 60 * 1000;
    
    public EaseModel(Context ctx){
        context = ctx;
        PreferenceManager.init(context);
    }

    public long getUserInfoTimeOut() {
        return userInfoTimeOut;
    }

    public void setUserInfoTimeOut(long userInfoTimeOut) {
        if(userInfoTimeOut > 0){
            this.userInfoTimeOut = userInfoTimeOut;
        }
    }


    public boolean updateContactList(List<EaseUser> contactList) {
        List<EmUserEntity> userEntities = EmUserEntity.parseList(contactList);
        EmUserDao dao = EaseDbHelper.getInstance(EaseIMHelper.getInstance().getApplication()).getUserDao();
        if(dao != null) {
            dao.insert(userEntities);
            return true;
        }
        return false;
    }

    public Map<String, EaseUser> getContactList() {
        EmUserDao dao = EaseDbHelper.getInstance(EaseIMHelper.getInstance().getApplication()).getUserDao();
        if(dao == null) {
            return new HashMap<>();
        }
        Map<String, EaseUser> map = new HashMap<>();
        List<EaseUser> users = dao.loadAllContactUsers();
        if(users != null && !users.isEmpty()) {
            for (EaseUser user : users) {
                map.put(user.getUsername(), user);
            }
        }
        return map;
    }


    public Map<String, EaseUser> getAllUserList() {
        EmUserDao dao = EaseDbHelper.getInstance(EaseIMHelper.getInstance().getApplication()).getUserDao();
        if(dao == null) {
            return new HashMap<>();
        }
        Map<String, EaseUser> map = new HashMap<>();
        List<EaseUser> users = dao.loadAllEaseUsers();
        if(users != null && !users.isEmpty()) {
            for (EaseUser user : users) {
                map.put(user.getUsername(), user);
            }
        }
        return map;
    }

    /**
     * get DemoDbHelper
     * @return
     */
    public EaseDbHelper getDbHelper() {
        return EaseDbHelper.getInstance(EaseIMHelper.getInstance().getApplication());
    }

    /**
     * 向数据库中插入数据
     * @param object
     */
    public void insert(Object object) {
        EaseDbHelper dbHelper = getDbHelper();
        if(object instanceof InviteMessage) {
            if(dbHelper.getInviteMessageDao() != null) {
                dbHelper.getInviteMessageDao().insert((InviteMessage) object);
            }
        }else if(object instanceof MsgTypeManageEntity) {
            if(dbHelper.getMsgTypeManageDao() != null) {
                dbHelper.getMsgTypeManageDao().insert((MsgTypeManageEntity) object);
            }
        }else if(object instanceof EmUserEntity) {
            if(dbHelper.getUserDao() != null) {
                dbHelper.getUserDao().insert((EmUserEntity) object);
            }
        }
    }

    /**
     * update
     * @param object
     */
    public void update(Object object) {
        EaseDbHelper dbHelper = getDbHelper();
        if(object instanceof InviteMessage) {
            if(dbHelper.getInviteMessageDao() != null) {
                dbHelper.getInviteMessageDao().update((InviteMessage) object);
            }
        }else if(object instanceof MsgTypeManageEntity) {
            if(dbHelper.getMsgTypeManageDao() != null) {
                dbHelper.getMsgTypeManageDao().update((MsgTypeManageEntity) object);
            }
        }else if(object instanceof EmUserEntity) {
            if(dbHelper.getUserDao() != null) {
                dbHelper.getUserDao().insert((EmUserEntity) object);
            }
        }
    }


    /**
     * 查找有关用户用户属性过期的用户ID
     *
     */
    public List<String> selectTimeOutUsers() {
        EaseDbHelper dbHelper = getDbHelper();
        List<String> users = null;
        if(dbHelper.getUserDao() != null) {
            users = dbHelper.getUserDao().loadTimeOutEaseUsers(userInfoTimeOut,System.currentTimeMillis());
        }
        return users;
    }
    
    /**
     * save current username
     * @param username
     */
    public void setCurrentUserName(String username){
        PreferenceManager.getInstance().setCurrentUserName(username);
    }

    public String getCurrentUsername(){
        return PreferenceManager.getInstance().getCurrentUsername();
    }

    /**
     * 保存是否删除联系人的状态
     * @param username
     * @param isDelete
     */
    public void deleteUsername(String username, boolean isDelete) {
        SharedPreferences sp = context.getSharedPreferences("save_delete_username_status", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putBoolean(username, isDelete);
        edit.commit();
    }

    /**
     * 查看联系人是否删除
     * @param username
     * @return
     */
    public boolean isDeleteUsername(String username) {
        SharedPreferences sp = context.getSharedPreferences("save_delete_username_status", Context.MODE_PRIVATE);
        return sp.getBoolean(username, false);
    }
    
    public void setSettingMsgNotification(boolean paramBoolean) {
        PreferenceManager.getInstance().setSettingMsgNotification(paramBoolean);
        valueCache.put(Key.VibrateAndPlayToneOn, paramBoolean);
    }

    public boolean getSettingMsgNotification() {
        Object val = valueCache.get(Key.VibrateAndPlayToneOn);

        if(val == null){
            val = PreferenceManager.getInstance().getSettingMsgNotification();
            valueCache.put(Key.VibrateAndPlayToneOn, val);
        }
       
        return (Boolean) (val != null?val:true);
    }

    public boolean isShowMsgTyping() {
        return PreferenceManager.getInstance().isShowMsgTyping();
    }

    public void showMsgTyping(boolean show) {
        PreferenceManager.getInstance().showMsgTyping(show);
    }

    /**
     * 保存未发送的文本消息内容
     * @param toChatUsername
     * @param content
     */
    public void saveUnSendMsg(String toChatUsername, String content) {
        EasePreferenceManager.getInstance().saveUnSendMsgInfo(toChatUsername, content);
    }

    public String getUnSendMsg(String toChatUsername) {
        return EasePreferenceManager.getInstance().getUnSendMsgInfo(toChatUsername);
    }

    /**
     * 检查是否是第一次安装登录
     * 默认值是true, 需要在用api拉取完会话列表后，就其置为false.
     * @return
     */
    public boolean isFirstInstall() {
        SharedPreferences preferences = context.getSharedPreferences("first_install", Context.MODE_PRIVATE);
        return preferences.getBoolean("is_first_install", true);
    }

    /**
     * 将状态置为非第一次安装，在调用获取会话列表的api后调用
     * 并将会话列表是否来自服务器置为true
     */
    public void makeNotFirstInstall() {
        SharedPreferences preferences = context.getSharedPreferences("first_install", Context.MODE_PRIVATE);
        preferences.edit().putBoolean("is_first_install", false).apply();
        preferences.edit().putBoolean("is_conversation_come_from_server", true).apply();
    }

    /**
     * 检查会话列表是否从服务器返回数据
     * @return
     */
    public boolean isConComeFromServer() {
        SharedPreferences preferences = context.getSharedPreferences("first_install", Context.MODE_PRIVATE);
        return preferences.getBoolean("is_conversation_come_from_server", false);
    }

    /**
     * 将会话列表从服务器取数据的状态置为false，即后面应该采用本地数据库数据。
     */
    public void modifyConComeFromStatus() {
        SharedPreferences preferences = context.getSharedPreferences("first_install", Context.MODE_PRIVATE);
        preferences.edit().putBoolean("is_conversation_come_from_server", false).apply();
    }

    public Boolean getAppMode() {
        return PreferenceManager.getInstance().getAppMode();
    }

    public void setAppMode(Boolean isAdmin) {
        PreferenceManager.getInstance().setAppMode(isAdmin);
    }

    public String getAppToken() {
        return PreferenceManager.getInstance().getAppToken();
    }

    public void setAppToken(String token) {
        PreferenceManager.getInstance().setAppToken(token);
    }

    public void setAid(String aid){
        PreferenceManager.getInstance().setAid(aid);
    }

    public String getAid(){
        return PreferenceManager.getInstance().getAid();
    }

    public void setAidToken(String token){
        PreferenceManager.getInstance().setAidToken(token);
    }

    public String getAidToken(){
        return PreferenceManager.getInstance().getAidToken();
    }

    public void setServiceGroup(String json){
        PreferenceManager.getInstance().setServiceGroupJson(json);
    }

    public String getServiceGroup(){
        return PreferenceManager.getInstance().getServiceGroupJson();
    }

    public void setCurrentUserNick(String nickname) {
        PreferenceManager.getInstance().setCurrentUserNick(nickname);
    }

    public void setCurrentUserAvatar(String avatar) {
        PreferenceManager.getInstance().setCurrentUserAvatar(avatar);
    }

    public String getCurrentUserNick() {
        return PreferenceManager.getInstance().getCurrentUserNick();
    }

    public String getCurrentUserAvatar() {
        return PreferenceManager.getInstance().getCurrentUserAvatar();
    }

    enum Key{
        VibrateAndPlayToneOn,
        VibrateOn,
        PlayToneOn,
        SpakerOn,
        DisabledGroups,
        DisabledIds
    }

    public void reset(){
        setAppToken("");
        setServiceGroup("");
        setAid("");
        setAidToken("");
    }
}
