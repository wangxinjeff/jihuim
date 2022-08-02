package com.hyphenate.easeim.section.group.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.hyphenate.easeim.common.livedatas.SingleSourceLiveData;
import com.hyphenate.easeim.common.model.GroupApplyBean;
import com.hyphenate.easeim.common.net.Resource;
import com.hyphenate.easeim.common.repositories.EMGroupManagerRepository;

import java.util.List;

public class GroupApplyViewModel extends AndroidViewModel {
    private EMGroupManagerRepository repository;
    private SingleSourceLiveData<Resource<List<GroupApplyBean>>> groupApplyObservable;
    private SingleSourceLiveData<Resource<GroupApplyBean>> groupOperationObservable;

    public GroupApplyViewModel(@NonNull Application application) {
        super(application);
        repository = new EMGroupManagerRepository();
        groupApplyObservable = new SingleSourceLiveData<>();
        groupOperationObservable = new SingleSourceLiveData<>();
    }

    public LiveData<Resource<List<GroupApplyBean>>> groupApplyObservable() {
        return groupApplyObservable;
    }

    public void fetchGroupApply(int page, int size) {
        groupApplyObservable.setSource(repository.fetchGroupApply(page, size));
    }

    public LiveData<Resource<GroupApplyBean>> groupOperationObservable() {
        return groupOperationObservable;
    }

    public void operationGroupApply(GroupApplyBean bean, String state) {
        groupOperationObservable.setSource(repository.operationGroupApply(bean, state));
    }
}
