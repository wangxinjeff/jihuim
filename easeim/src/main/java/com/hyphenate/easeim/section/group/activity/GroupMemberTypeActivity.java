package com.hyphenate.easeim.section.group.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.View;

import com.hyphenate.chat.EMGroup;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeim.common.livedatas.LiveDataBus;
import com.hyphenate.easeim.common.repositories.EMGroupManagerRepository;
import com.hyphenate.easeim.common.widget.SearchBar;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeim.section.group.adapter.GroupMemberListAdapter;
import com.hyphenate.easeim.section.group.delegate.GroupMemberDelegate;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.easeui.widget.EaseRecyclerView;
import com.hyphenate.easeui.widget.EaseTitleBar;

import java.util.List;

public class GroupMemberTypeActivity extends BaseInitActivity implements EaseTitleBar.OnBackPressListener{
    private static final int REQUEST_CODE_ADD_USER = 0;
    private EaseTitleBar titleBar;
    private String groupId;
    private EMGroup group;
    private boolean isOwner;
    private SearchBar searchBar;
    private GroupMemberListAdapter listAdapter;
    private EaseRecyclerView memberListView;
    private EMGroupManagerRepository repository = new EMGroupManagerRepository();

    @Override
    protected int getLayoutId() {
        return R.layout.demo_activity_chat_group_member_type;
    }

    public static void actionStart(Context context, String groupId, boolean owner) {
        Intent starter = new Intent(context, GroupMemberTypeActivity.class);
        starter.putExtra("groupId", groupId);
        starter.putExtra("isOwner", owner);
        context.startActivity(starter);
    }

    @Override
    protected void initIntent(Intent intent) {
        super.initIntent(intent);
        groupId = intent.getStringExtra("groupId");
        isOwner = intent.getBooleanExtra("isOwner", false);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);

        titleBar = findViewById(R.id.title_bar);

        if(EaseIMHelper.getInstance().isAdmin()){
            titleBar.setLeftImageResource(R.drawable.em_icon_back_admin);
        }

        searchBar = findViewById(R.id.search_bar);
        searchBar.init(false);

        memberListView = findViewById(R.id.rv_member_list);
        memberListView.setLayoutManager(new LinearLayoutManager(this));

        listAdapter = new GroupMemberListAdapter();
        listAdapter.setHasStableIds(true);
        listAdapter.addDelegate(new GroupMemberDelegate());
        memberListView.setAdapter(listAdapter);
    }

    @Override
    protected void initListener() {
        super.initListener();
        titleBar.setOnBackPressListener(this);
        searchBar.setOnSearchBarListener(new SearchBar.OnSearchBarListener() {
            @Override
            public void onSearchContent(String text) {
                listAdapter.getFilter().filter(text);
            }
        });
        titleBar.setRightLayoutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GroupPickContactsActivity.actionStart(mContext, groupId, isOwner, false);
            }
        });
    }

    @Override
    protected void initData() {
        super.initData();
        group = EaseIMHelper.getInstance().getGroupManager().getGroup(groupId);
        getGroupMembers();

        LiveDataBus.get().with(EaseConstant.GROUP_CHANGE, EaseEvent.class).observe(this, event -> {
            if(event.isGroupChange()) {
                getGroupMembers();
            }else if(event.isGroupLeave() && TextUtils.equals(groupId, event.message)) {
                finish();
            }
        });

        LiveDataBus.get().with(EaseConstant.CONTACT_UPDATE, EaseEvent.class).observe(this, event -> {
            if(event == null) {
                return;
            }
            if(event.isContactChange()) {
                if(listAdapter != null){
                    listAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onBackPress(View view) {
        onBackPressed();
    }

    private void getGroupMembers(){
        showLoading();
        repository.getGroupAllMembers(groupId, new ResultCallBack<List<EaseUser>>() {
            @Override
            public void onSuccess(List<EaseUser> easeUsers) {
                dismissLoading();
                runOnUiThread(() -> listAdapter.setData(easeUsers));
            }

            @Override
            public void onError(int i, String s) {
                dismissLoading();
            }
        });
    }
}
