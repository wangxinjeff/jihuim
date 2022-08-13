package com.hyphenate.easeim.section.group.activity;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeim.common.model.GroupApplyBean;
import com.hyphenate.easeim.common.repositories.EMGroupManagerRepository;
import com.hyphenate.easeim.common.utils.ToastUtils;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeim.section.chat.activity.ChatActivity;
import com.hyphenate.easeim.section.group.adapter.GroupApplyAdapter;

import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.widget.EaseTitleBar;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshLoadMoreListener;

import java.util.ArrayList;
import java.util.List;

public class GroupApplyActivity extends BaseInitActivity implements OnRefreshLoadMoreListener {

    private EaseTitleBar titleBar;
    private RecyclerView applyList;
    private SmartRefreshLayout srlRefresh;
    private GroupApplyAdapter applyAdapter;
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
                operationGroupApply(bean, "fail");
            }

            @Override
            public void onAgreed(GroupApplyBean bean) {
                operationGroupApply(bean, "success");
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
            fetchGroupApply();
        } else {
            ToastUtils.showCenterToast("", "没数据了", 0, Toast.LENGTH_SHORT);
            srlRefresh.finishLoadMore();
        }
    }

    private void fetchGroupApply(){
        showLoading();
        EMGroupManagerRepository.getInstance().fetchGroupApply(page, size, new ResultCallBack<List<GroupApplyBean>>() {
            @Override
            public void onSuccess(List<GroupApplyBean> data) {
                dismissLoading();
                if(data.size() < 1){
                    ToastUtils.showCenterToast("", "没数据了", 0, Toast.LENGTH_SHORT);
                } else {
                    list.addAll(data);
                    runOnUiThread(() -> applyAdapter.setData(list));
                    page ++;
                }
            }

            @Override
            public void onError(int i, String s) {
                dismissLoading();
            }
        });
    }

    private void operationGroupApply(GroupApplyBean bean, String str){
        showLoading();
        EMGroupManagerRepository.getInstance().operationGroupApply(bean, str, new ResultCallBack<GroupApplyBean>() {
            @Override
            public void onSuccess(GroupApplyBean bean) {
                dismissLoading();
                runOnUiThread(() -> applyAdapter.notifyDataSetChanged());
                if(TextUtils.equals(bean.getOperatedResult(), "success")){
                    ToastUtils.showCenterToast("", "已同意", 0, Toast.LENGTH_SHORT);
                } else {
                    ToastUtils.showCenterToast("", "已拒绝", 0, Toast.LENGTH_SHORT);
                }
            }

            @Override
            public void onError(int i, String s) {
                dismissLoading();
            }
        });
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
