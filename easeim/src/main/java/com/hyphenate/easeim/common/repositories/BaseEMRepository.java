package com.hyphenate.easeim.common.repositories;

import android.content.Context;

import com.hyphenate.chat.EMChatManager;
import com.hyphenate.chat.EMChatRoomManager;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMContactManager;
import com.hyphenate.chat.EMGroupManager;
import com.hyphenate.chat.EMPushManager;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.common.db.EaseDbHelper;
import com.hyphenate.easeui.manager.EaseThreadManager;

public class BaseEMRepository {

    /**
     * login before
     * @return
     */
    public boolean isLoggedIn() {
        return EMClient.getInstance().isLoggedInBefore();
    }

    /**
     * 获取本地标记，是否自动登录
     * @return
     */
    public boolean isAutoLogin() {
        return EaseIMHelper.getInstance().getAutoLogin();
    }

    /**
     * 获取当前用户
     * @return
     */
    public String getCurrentUser() {
        return EaseIMHelper.getInstance().getCurrentUser();
    }

    /**
     * EMChatManager
     * @return
     */
    public EMChatManager getChatManager() {
        return EaseIMHelper.getInstance().getEMClient().chatManager();
    }

    /**
     * EMContactManager
     * @return
     */
    public EMContactManager getContactManager() {
        return EaseIMHelper.getInstance().getContactManager();
    }

    /**
     * EMGroupManager
     * @return
     */
    public EMGroupManager getGroupManager() {
        return EaseIMHelper.getInstance().getEMClient().groupManager();
    }

    /**
     * EMChatRoomManager
     * @return
     */
    public EMChatRoomManager getChatRoomManager() {
        return EaseIMHelper.getInstance().getChatroomManager();
    }


    /**
     * EMPushManager
     * @return
     */
    public EMPushManager getPushManager() {
        return EaseIMHelper.getInstance().getPushManager();
    }

    /**
     * init room
     */
    public void initDb() {
        EaseDbHelper.getInstance(getContext()).initDb(getCurrentUser());
    }

    /**
     * 在主线程执行
     * @param runnable
     */
    public void runOnMainThread(Runnable runnable) {
        EaseThreadManager.getInstance().runOnMainThread(runnable);
    }

    /**
     * 在异步线程
     * @param runnable
     */
    public void runOnIOThread(Runnable runnable) {
        EaseThreadManager.getInstance().runOnIOThread(runnable);
    }

    public Context getContext() {
        return EaseIMHelper.getInstance().getApplication();
    }

}
