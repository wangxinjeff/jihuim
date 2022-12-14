package com.hyphenate.easeim.section.group.viewmodels;

import android.app.Application;

import com.hyphenate.easeim.common.livedatas.SingleSourceLiveData;
import com.hyphenate.easeim.common.net.Resource;
import com.hyphenate.easeim.common.repositories.EMContactManagerRepository;
import com.hyphenate.easeim.common.repositories.EMGroupManagerRepository;
import com.hyphenate.easeui.domain.EaseUser;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class GroupPickContactsViewModel extends AndroidViewModel {
    private EMGroupManagerRepository repository;
    private EMContactManagerRepository contactRepository;
    private SingleSourceLiveData<Resource<List<String>>> groupMembers;
    private SingleSourceLiveData<Resource<List<EaseUser>>> contacts;
    private SingleSourceLiveData<Resource<Boolean>> addMembersObservable;
    private SingleSourceLiveData<Resource<List<EaseUser>>> searchContactsObservable;

    public GroupPickContactsViewModel(@NonNull Application application) {
        super(application);
        repository = new EMGroupManagerRepository();
        contactRepository = new EMContactManagerRepository();
        groupMembers = new SingleSourceLiveData<>();
        contacts = new SingleSourceLiveData<>();
        addMembersObservable = new SingleSourceLiveData<>();
        searchContactsObservable = new SingleSourceLiveData<>();
    }

    public LiveData<Resource<List<String>>> getGroupMembersObservable() {
        return groupMembers;
    }

    public void getGroupMembers(String groupId) {
        groupMembers.setSource(repository.getGroupMembersByName(groupId));
    }

    public LiveData<Resource<List<EaseUser>>> getContacts() {
        return contacts;
    }

    public LiveData<Resource<Boolean>> getAddMembersObservable() {
        return addMembersObservable;
    }

    public void addGroupMembersWithAdmin(boolean isOwner, String groupId, List<String> customers, List<String> waiters) {
        addMembersObservable.setSource(repository.addMembersWithAdmin(groupId, customers, waiters));
    }

    public void addGroupMembersWithCustomer(boolean isOwner, String groupId, List<String> customers, List<String> waiters) {
        addMembersObservable.setSource(repository.addMembersWithCustomer(groupId, customers, waiters));
    }

    public LiveData<Resource<List<EaseUser>>> getSearchContactsObservable() {
        return searchContactsObservable;
    }

    public void  searchUserWithCustomer(String keyword) {
        searchContactsObservable.setSource(contactRepository.searchUserWithCustomer(keyword));
    }

    public void  searchUserWithAdmin(String keyword) {
        searchContactsObservable.setSource(contactRepository.searchUserWithAdmin(keyword));
    }

}
