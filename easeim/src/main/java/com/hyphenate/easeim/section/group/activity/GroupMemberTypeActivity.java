package com.hyphenate.easeim.section.group.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.hyphenate.chat.EMGroup;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.OnResourceParseCallback;
import com.hyphenate.easeim.common.livedatas.LiveDataBus;
import com.hyphenate.easeim.common.widget.SearchBar;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeim.section.group.adapter.GroupMemberListAdapter;
import com.hyphenate.easeim.section.group.delegate.GroupMemberDelegate;
import com.hyphenate.easeim.section.group.viewmodels.GroupMemberAuthorityViewModel;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.manager.EaseThreadManager;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.easeui.widget.EaseRecyclerView;
import com.hyphenate.easeui.widget.EaseTitleBar;

import java.util.List;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

public class GroupMemberTypeActivity extends BaseInitActivity implements EaseTitleBar.OnBackPressListener{
    private static final int REQUEST_CODE_ADD_USER = 0;
    private EaseTitleBar titleBar;
    private String groupId;
    private EMGroup group;
    private boolean isOwner;
    private SearchBar searchBar;
    private ConcatAdapter adapter;
    private GroupMemberListAdapter listAdapter;
    private EaseRecyclerView memberListView;

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
            titleBar.setLeftImageResource(R.drawable.icon_back_admin);
        }

        searchBar = findViewById(R.id.search_bar);
        searchBar.init(false);

        memberListView = findViewById(R.id.rv_member_list);
        memberListView.setLayoutManager(new LinearLayoutManager(this));

        ConcatAdapter.Config config = new ConcatAdapter.Config.Builder()
                .setStableIdMode(ConcatAdapter.Config.StableIdMode.ISOLATED_STABLE_IDS)
                .build();
        adapter = new ConcatAdapter(config);

        listAdapter = new GroupMemberListAdapter();
        listAdapter.setHasStableIds(true);
        adapter.addAdapter(listAdapter);
        listAdapter.addDelegate(new GroupMemberDelegate());
        memberListView.setAdapter(adapter);
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
        GroupMemberAuthorityViewModel viewModel = new ViewModelProvider(this).get(GroupMemberAuthorityViewModel.class);

        viewModel.getGroupMember().observe(this, response -> {

            parseResource(response, new OnResourceParseCallback<List<EaseUser>>() {
                @Override
                public void onSuccess(List<EaseUser> data) {
                    EaseThreadManager.getInstance().runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            listAdapter.setData(data);
                        }
                    });
                }

                @Override
                public void hideLoading() {
                    super.hideLoading();

                }
            });
        });

        viewModel.getGroupMembers(groupId);

        viewModel.getMessageChangeObservable().with(EaseConstant.GROUP_CHANGE, EaseEvent.class).observe(this, event -> {
            if(event.isGroupChange()) {
                viewModel.getGroupMembers(groupId);
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
}
