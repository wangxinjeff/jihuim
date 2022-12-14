package com.hyphenate.easeim.section.group;

import android.text.TextUtils;

import androidx.lifecycle.LiveData;

import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMChatRoom;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.common.livedatas.LiveDataBus;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.model.EaseEvent;

import java.util.List;

public class GroupHelper {

    /**
     * 是否是群主
     * @return
     */
    public static boolean isOwner(EMGroup group) {
        if(group == null ||
                TextUtils.isEmpty(group.getOwner())) {
            return false;
        }
        return TextUtils.equals(group.getOwner(), EaseIMHelper.getInstance().getCurrentUser());
    }

    /**
     * 是否是聊天室创建者
     * @return
     */
    public static boolean isOwner(EMChatRoom room) {
        if(room == null ||
                TextUtils.isEmpty(room.getOwner())) {
            return false;
        }
        return TextUtils.equals(room.getOwner(), EaseIMHelper.getInstance().getCurrentUser());
    }

    /**
     * 是否是管理员
     * @return
     */
    public synchronized static boolean isAdmin(EMGroup group) {
        List<String> adminList = group.getAdminList();
        if(adminList != null && !adminList.isEmpty()) {
            return adminList.contains(EaseIMHelper.getInstance().getCurrentUser());
        }
        return false;
    }

    /**
     * 是否是管理员
     * @return
     */
    public synchronized static boolean isAdmin(EMChatRoom group) {
        List<String> adminList = group.getAdminList();
        if(adminList != null && !adminList.isEmpty()) {
            return adminList.contains(EaseIMHelper.getInstance().getCurrentUser());
        }
        return false;
    }

    /**
     * 是否有邀请权限
     * @return
     */
    public static boolean isCanInvite(EMGroup group) {
        return group != null && (group.isMemberAllowToInvite() || isOwner(group) || isAdmin(group));
    }

    /**
     * 在黑名单中
     * @param username
     * @return
     */
    public static boolean isInAdminList(String username, List<String> adminList) {
        return isInList(username, adminList);
    }

    /**
     * 在黑名单中
     * @param username
     * @return
     */
    public static boolean isInBlackList(String username, List<String> blackMembers) {
        return isInList(username, blackMembers);
    }

    /**
     * 在禁言名单中
     * @param username
     * @return
     */
    public static boolean isInMuteList(String username, List<String> muteMembers) {
        return isInList(username, muteMembers);
    }

    /**
     * 是否在列表中
     * @param name
     * @return
     */
    public static boolean isInList(String name, List<String> list) {
        if(list == null) {
            return false;
        }
        synchronized (GroupHelper.class) {
            for (String item : list) {
                if (TextUtils.equals(name, item)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取群名称
     * @param groupId
     * @return
     */
    public static String getGroupName(String groupId) {
        EMGroup group = EMClient.getInstance().groupManager().getGroup(groupId);
        if(group == null) {
            EaseIMHelper.getInstance().getGroupManager().asyncGetGroupFromServer(groupId, new EMValueCallBack<EMGroup>() {
                @Override
                public void onSuccess(EMGroup emGroup) {
                    LiveDataBus.get().with(EaseConstant.GROUP_CHANGE).postValue(EaseEvent.create(EaseConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP_NAME, groupId));
                }

                @Override
                public void onError(int i, String s) {

                }
            });
            return groupId;
        }
        return TextUtils.isEmpty(group.getGroupName()) ? groupId : group.getGroupName();
    }

    public static String getGroupCount(String groupId) {
        EMGroup group = EMClient.getInstance().groupManager().getGroup(groupId);
        if(group == null) {
            return String.valueOf(1);
        }
        return String.valueOf(group.getMemberCount());
    }

    /**
     * 判断是否加入了群组
     * @param allJoinGroups 所有加入的群组
     * @param groupId
     * @return
     */
    public static boolean isJoinedGroup(List<EMGroup> allJoinGroups, String groupId) {
        if(allJoinGroups == null || allJoinGroups.isEmpty()) {
            return false;
        }
        for (EMGroup group : allJoinGroups) {
            if(TextUtils.equals(group.getGroupId(), groupId)) {
                return true;
            }
        }
        return false;
    }
}
