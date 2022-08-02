package com.hyphenate.easeim.section.group.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMGroupOptions;
import com.hyphenate.easeim.common.livedatas.SingleSourceLiveData;
import com.hyphenate.easeim.common.net.Resource;
import com.hyphenate.easeim.common.repositories.EMGroupManagerRepository;

import java.util.List;

public class NewGroupViewModel extends AndroidViewModel {
    private EMGroupManagerRepository repository;
    private SingleSourceLiveData<Resource<String>> groupObservable;

    public NewGroupViewModel(@NonNull Application application) {
        super(application);
        repository = new EMGroupManagerRepository();
        groupObservable = new SingleSourceLiveData<>();
    }

    public LiveData<Resource<String>> groupObservable() {
        return groupObservable;
    }

    public void createGroup(String groupName, String desc, List<String> customers, List<String> waiters) {
        groupObservable.setSource(repository.createGroup(groupName, desc, customers, waiters));
    }
}
