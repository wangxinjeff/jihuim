package com.hyphenate.easeim.section.group.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.OnResourceParseCallback;
import com.hyphenate.easeim.common.model.GroupApplyBean;
import com.hyphenate.easeim.common.utils.ToastUtils;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeim.section.chat.activity.ChatActivity;
import com.hyphenate.easeim.section.group.adapter.GroupApplyAdapter;

import com.hyphenate.easeim.section.group.viewmodels.GroupApplyViewModel;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.widget.EaseTitleBar;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshLoadMoreListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Observable;

public class GroupApplyActivity extends BaseInitActivity implements OnRefreshLoadMoreListener {

    private EaseTitleBar titleBar;
    private RecyclerView applyList;
    private SmartRefreshLayout srlRefresh;
    private GroupApplyAdapter applyAdapter;
    private GroupApplyViewModel viewModel;
    private int page = 0;
    private int size = 20;
    private List<GroupApplyBean> list;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_group_apply;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        titleBar = findViewById(R.id.title_bar);
        srlRefresh = findViewById(R.id.srl_refresh);

        srlRefresh.setEnabled(true);
        applyList = findViewById(R.id.apple_list);
        applyList.setLayoutManager(new LinearLayoutManager(this));
        applyAdapter = new GroupApplyAdapter();
        applyList.setAdapter(applyAdapter);
    }

    @Override
    protected void initData() {
        super.initData();
        list = new ArrayList<>();
        viewModel = new ViewModelProvider(this).get(GroupApplyViewModel.class);
        viewModel.groupApplyObservable().observe(this, response ->{
            parseResource(response, new OnResourceParseCallback<List<GroupApplyBean>>() {
                @Override
                public void onSuccess(@Nullable List<GroupApplyBean> data) {
                    if(data.size() < 1){
                        ToastUtils.showCenterToast("", "没数据了", 0, Toast.LENGTH_SHORT);
                    } else {
                        list.addAll(data);
                        runOnUiThread(() -> applyAdapter.setData(list));
                        page ++;
                    }
                }

                @Override
                public void onLoading(@Nullable List<GroupApplyBean> data) {
                    super.onLoading(data);
                    showLoading();
                }

                @Override
                public void hideLoading() {
                    super.hideLoading();
                    dismissLoading();
                    srlRefresh.finishLoadMore();
                }

                @Override
                public void onError(int code, String message) {
                    super.onError(code, message);
                }
            });
        });

        viewModel.groupOperationObservable().observe(this, response -> {
            parseResource(response, new OnResourceParseCallback<GroupApplyBean>() {
                @Override
                public void onSuccess(@Nullable GroupApplyBean data) {
                    runOnUiThread(() -> applyAdapter.notifyDataSetChanged());
                    if(TextUtils.equals(data.getOperatedResult(), "success")){
                        ToastUtils.showCenterToast("", "已同意", 0, Toast.LENGTH_SHORT);
                    } else {
                        ToastUtils.showCenterToast("", "已拒绝", 0, Toast.LENGTH_SHORT);
                    }
                }

                @Override
                public void onLoading(@Nullable GroupApplyBean data) {
                    super.onLoading(data);
                    showLoading();
                }

                @Override
                public void hideLoading() {
                    super.hideLoading();
                    dismissLoading();
                }

                @Override
                public void onError(int code, String message) {
                    super.onError(code, message);
                }
            });
        });

        viewModel.fetchGroupApply(page, size);
    }

    @Override
    protected void initListener() {
        super.initListener();
        srlRefresh.setOnRefreshLoadMoreListener(this);

        titleBar.setOnBackPressListener(new EaseTitleBar.OnBackPressListener() {
            @Override
            public void onBackPress(View view) {
                onBackPressed();
            }
        });

        applyAdapter.setOnGroupApplyListener(new GroupApplyAdapter.OnGroupApplyListener() {
            @Override
            public void onRefused(GroupApplyBean bean) {
                viewModel.operationGroupApply(bean, "fail");
            }

            @Override
            public void onAgreed(GroupApplyBean bean) {
                viewModel.operationGroupApply(bean, "success");
            }

            @Override
            public void onItemClick(GroupApplyBean bean) {
                ChatActivity.actionStart(mContext, bean.getGroupId(), EaseConstant.CHATTYPE_GROUP);
            }
        });
    }

    @Override
    public void onLoadMore(RefreshLayout refreshLayout) {
        if(applyAdapter.getData().size() >= page * size){
            viewModel.fetchGroupApply(page, size);
        } else {
            ToastUtils.showCenterToast("", "没数据了", 0, Toast.LENGTH_SHORT);
            srlRefresh.finishLoadMore();
        }
    }

    @Override
    public void onRefresh(RefreshLayout refreshLayout) {

    }

    /**
     * 排序
     * @param list
     */
//    private void sortByTimestamp(List<GroupApplyBean> list) {
//        if(list == null || list.isEmpty()) {
//            return;
//        }
//        Collections.sort(list, new Comparator<GroupApplyBean>() {
//            @Override
//            public int compare(GroupApplyBean o1, GroupApplyBean o2) {
//                if(o2.getTimeStamp() > o1.getTimeStamp()) {
//                    return 1;
//                }else if(o2.getTimeStamp() == o1.getTimeStamp()) {
//                    return 0;
//                }else {
//                    return -1;
//                }
//            }
//        });
//    }
}
