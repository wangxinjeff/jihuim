package com.hyphenate.easeim.section.group.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.common.livedatas.LiveDataBus;
import com.hyphenate.easeim.common.livedatas.SingleSourceLiveData;
import com.hyphenate.easeim.common.model.SearchResult;
import com.hyphenate.easeim.common.net.Resource;
import com.hyphenate.easeim.common.repositories.EMChatManagerRepository;
import com.hyphenate.easeim.common.repositories.EMGroupManagerRepository;
import com.hyphenate.easeim.common.repositories.EMPushManagerRepository;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.manager.EaseThreadManager;
import com.hyphenate.exceptions.HyphenateException;

import java.security.interfaces.RSAKey;
import java.util.ArrayList;
import java.util.List;

public class GroupDetailViewModel extends AndroidViewModel {
    private EMGroupManagerRepository repository;
    private EMChatManagerRepository chatRepository;
    private SingleSourceLiveData<Resource<EMGroup>> groupObservable;
    private SingleSourceLiveData<Resource<String>> announcementObservable;
    private SingleSourceLiveData<Resource<String>> refreshObservable;
    private SingleSourceLiveData<Resource<Boolean>> leaveGroupObservable;
    private SingleSourceLiveData<Resource<Boolean>> blockGroupMessageObservable;
    private SingleSourceLiveData<Resource<Boolean>> unblockGroupMessage;
    private SingleSourceLiveData<Resource<Boolean>> clearHistoryObservable;
    private SingleSourceLiveData<Boolean> offPushObservable;
    private SingleSourceLiveData<Resource<List<EaseUser>>> groupMemberObservable;
    private SingleSourceLiveData<Resource<String>> serviceNoteObservable;
    private SingleSourceLiveData<Resource<List<String>>> noteObservable;
    private SingleSourceLiveData<Resource<List<SearchResult>>> searchObservable;


    public GroupDetailViewModel(@NonNull Application application) {
        super(application);
        repository = new EMGroupManagerRepository();
        chatRepository = new EMChatManagerRepository();
        groupObservable = new SingleSourceLiveData<>();
        announcementObservable = new SingleSourceLiveData<>();
        refreshObservable = new SingleSourceLiveData<>();
        leaveGroupObservable = new SingleSourceLiveData<>();
        blockGroupMessageObservable = new SingleSourceLiveData<>();
        unblockGroupMessage = new SingleSourceLiveData<>();
        clearHistoryObservable = new SingleSourceLiveData<>();
        offPushObservable = new SingleSourceLiveData<>();
        groupMemberObservable = new SingleSourceLiveData<>();
        serviceNoteObservable = new SingleSourceLiveData<>();
        searchObservable = new SingleSourceLiveData<>();
        noteObservable = new SingleSourceLiveData<>();
    }

    public LiveDataBus getMessageChangeObservable() {
        return LiveDataBus.get();
    }

    public LiveData<Resource<EMGroup>> getGroupObservable() {
        return groupObservable;
    }

    public void getGroup(String groupId) {
        new EMPushManagerRepository().getPushConfigsFromServer();
        groupObservable.setSource(repository.getGroupFromServer(groupId));
    }

    public LiveData<Resource<String>> getAnnouncementObservable() {
        return announcementObservable;
    }

    public void getGroupAnnouncement(String groupId) {
        announcementObservable.setSource(repository.getGroupAnnouncement(groupId));
    }

    public LiveData<Resource<String>> getRefreshObservable() {
        return refreshObservable;
    }

    public void setGroupName(String groupId, String groupName) {
        refreshObservable.setSource(repository.setGroupName(groupId, groupName));
    }

    public void setGroupAnnouncement(String groupId, String announcement) {
        refreshObservable.setSource(repository.setGroupAnnouncement(groupId, announcement));
    }

    public void setGroupDescription(String groupId, String description) {
        refreshObservable.setSource(repository.setGroupDescription(groupId, description));
    }

    public LiveData<Resource<Boolean>> getLeaveGroupObservable() {
        return leaveGroupObservable;
    }

    public void leaveGroup(String groupId) {
        leaveGroupObservable.setSource(repository.leaveGroup(groupId));
    }

    public void destroyGroup(String groupId) {
        leaveGroupObservable.setSource(repository.destroyGroup(groupId));
    }

    public LiveData<Resource<Boolean>> blockGroupMessageObservable() {
        return blockGroupMessageObservable;
    }

    public void blockGroupMessage(String groupId) {
        blockGroupMessageObservable.setSource(repository.blockGroupMessage(groupId));
    }

    public LiveData<Resource<Boolean>> unblockGroupMessage() {
        return unblockGroupMessage;
    }

    public void unblockGroupMessage(String groupId) {
        unblockGroupMessage.setSource(repository.unblockGroupMessage(groupId));
    }

    public LiveData<Boolean> offPushObservable() {
        return offPushObservable;
    }

    public void updatePushServiceForGroup(String groupId, boolean noPush) {
        EaseThreadManager.getInstance().runOnIOThread(()-> {
            List<String> onPushList = new ArrayList<>();
            onPushList.add(groupId);
            try {
                EaseIMHelper.getInstance().getPushManager().updatePushServiceForGroup(onPushList, noPush);
                EMMessage message = EMMessage.createSendMessage(EMMessage.Type.CMD);
                EMCmdMessageBody body = new EMCmdMessageBody("event");
                body.deliverOnlineOnly(true);
                message.addBody(body);
                message.setTo(EaseIMHelper.getInstance().getCurrentUser());
                message.setAttribute(EaseConstant.MESSAGE_ATTR_EVENT_TYPE, EaseConstant.EVENT_TYPE_GROUP_NO_PUSH);
                message.setAttribute(EaseConstant.MESSAGE_ATTR_NO_PUSH, noPush);
                message.setAttribute(EaseConstant.MESSAGE_ATTR_NO_PUSH_ID, groupId);
                EMClient.getInstance().chatManager().sendMessage(message);
            } catch (HyphenateException e) {
                e.printStackTrace();
                offPushObservable.postValue(true);
            }
            offPushObservable.postValue(true);
        });

    }

    public LiveData<Resource<Boolean>> getClearHistoryObservable() {
        return clearHistoryObservable;
    }

    public void clearHistory(String conversationId) {
        clearHistoryObservable.setSource(chatRepository.deleteConversationById(conversationId));
    }

    public LiveData<Resource<List<EaseUser>>> getGroupMember() {
        return groupMemberObservable;
    }

    public void getGroupAllMember(String groupId){
        groupMemberObservable.setSource(repository.getGroupAllMembers(groupId));
    }

    public LiveData<Resource<String>> getServiceNoteObservable(){
        return serviceNoteObservable;
    }

    public LiveData<Resource<List<String>>> getNoteObservable(){
        return noteObservable;
    }

    public void getServiceNote(String groupId){
        noteObservable.setSource(repository.getServiceNote(groupId));
    }

    public void changeServiceNote(String groupId, String note){
        serviceNoteObservable.setSource(repository.changeServiceNote(groupId, note));
    }

    public LiveData<Resource<List<SearchResult>>> getSearchObservable(){
        return searchObservable;
    }

    public void searchGroupChat(String aid, String mobile, String orderId, String vin, String groupType, String groupName, String source){
        searchObservable.setSource(repository.searchGroupChat(aid, mobile, orderId, vin, groupType, groupName, source));
    }
}
