package com.hyphenate.easeim.section.group.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.OnResourceParseCallback;
import com.hyphenate.easeim.common.utils.ToastUtils;
import com.hyphenate.easeim.common.widget.ArrowItemView;

import com.hyphenate.easeim.section.base.BaseInitActivity;

import com.hyphenate.easeim.section.chat.activity.ChatActivity;
import com.hyphenate.easeim.section.group.viewmodels.NewGroupViewModel;

import com.hyphenate.easeim.section.group.adapter.GroupDetailMemberAdapter;
import com.hyphenate.easeim.section.group.fragment.GroupEditFragment;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.widget.EaseTitleBar;


import androidx.appcompat.widget.AppCompatButton;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NewGroupActivity extends BaseInitActivity implements EaseTitleBar.OnBackPressListener, View.OnClickListener{
    private static final int ADD_NEW_MEMBERS = 10;
    private EaseTitleBar titleBar;
    private TextView tvGroupMemberTitle;
    private TextView tvGroupMemberNum;
    private ArrowItemView itemGroupName;
    private ArrowItemView itemGroupIntroduction;

    private int maxUsers = 200;
    private static final int MAX_GROUP_USERS = 3000;
    private static final int MIN_GROUP_USERS = 3;
    private NewGroupViewModel viewModel;
    private List<EaseUser> members;

    private GroupDetailMemberAdapter memberAdapter;
    private RecyclerView memberList;
    private AppCompatButton confirmBtn;

    private String groupName = "";
    private String groupIntroduction = "";
    List<String> customers;
    List<String> waiters;

    public static void actionStart(Context context,  ArrayList<EaseUser> members) {
        Intent intent = new Intent(context, NewGroupActivity.class);
        intent.putParcelableArrayListExtra("members", members);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.em_activity_new_group;
    }

    @Override
    protected void initIntent(Intent intent) {
        super.initIntent(intent);
        members = intent.getParcelableArrayListExtra("members");
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent != null) {
            initIntent(intent);
            initData();
        }
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        titleBar = findViewById(R.id.title_bar);
        tvGroupMemberTitle = findViewById(R.id.tv_group_member_title);
        tvGroupMemberNum = findViewById(R.id.tv_group_member_num);
        itemGroupName = findViewById(R.id.item_group_name);
        itemGroupIntroduction = findViewById(R.id.item_group_introduction);

        itemGroupName.getTvContent().setHint(getString(R.string.group_name));

        memberList = findViewById(R.id.rl_member_list);
        memberAdapter = new GroupDetailMemberAdapter();
        memberAdapter.setShowAll(true);
        memberList.setLayoutManager(new GridLayoutManager(this, 6));
        memberList.setAdapter(memberAdapter);
        confirmBtn = findViewById(R.id.done);
    }

    @Override
    protected void initListener() {
        super.initListener();
        titleBar.setOnBackPressListener(this);

        itemGroupName.setOnClickListener(this);
        itemGroupIntroduction.setOnClickListener(this);

        memberAdapter.setOnAddClickListener(new GroupDetailMemberAdapter.GroupMemberAddClickListener() {
            @Override
            public void onAddClick() {
                GroupPickContactsActivity.actionStart(mContext, (ArrayList<EaseUser>) members);
            }

            @Override
            public void onRemoveClick() {

            }
        });

        confirmBtn.setOnClickListener(this);

        viewModel = new ViewModelProvider(this).get(NewGroupViewModel.class);
        viewModel.groupObservable().observe(this, response -> {
            parseResource(response, new OnResourceParseCallback<String>() {
                @Override
                public void onSuccess(String groupId) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.showCenterToast("", "创建成功", 0, Toast.LENGTH_SHORT);
                            ChatActivity.actionStart(mContext, groupId, EaseConstant.CHATTYPE_GROUP);
                            finish();
                        }
                    });
                }

                @Override
                public void onLoading(String data) {
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
                    ToastUtils.showCenterToast("", "创建群组失败:" + code + ":" + message, 0, Toast.LENGTH_SHORT);
                }
            });
        });

    }

    @Override
    protected void initData() {
        super.initData();
        List<EaseUser> data = new ArrayList<>();
        EaseUser user = new EaseUser("em_editUser");
        user.setNickname(getString(R.string.em_action_edit));
        data.add(user);
        data.addAll(members);
        memberAdapter.setData(data);

        customers = new ArrayList<>();
        waiters = new ArrayList<>();

        for (EaseUser easeUser : members) {
            if(easeUser.isCustomer()){
                customers.add(easeUser.getUsername());
            } else {
                waiters.add(easeUser.getUsername());
            }
        }

//        if(customers.size() >  0){
//            confirmBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.search_close));
//            confirmBtn.setEnabled(true);
//            confirmBtn.setText(getString(R.string.em_group_new_save));
//        } else {
//            confirmBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.btn_gray_pressed));
//            confirmBtn.setEnabled(false);
//            confirmBtn.setText(getString(R.string.em_must_be_no_less_than_2_members));
//        }

        tvGroupMemberNum.setText(members.size() + "人");
    }

    @Override
    public void onBackPress(View view) {
        onBackPressed();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.item_group_name) {//群名称
            showGroupNameDialog();
        } else if (id == R.id.item_group_introduction) {//群介绍
            showIntroductionDialog();
        } else if (id == R.id.done) {
            if(customers.size() < 1){
                ToastUtils.showCenterToast("" ,getString(R.string.em_new_group_includ_customer), 0, Toast.LENGTH_SHORT);
                return;
            }

            if(members.size() < 1){
                ToastUtils.showCenterToast("" ,getString(R.string.em_must_be_no_less_than_2_members), 0, Toast.LENGTH_SHORT);
                return;
            }

            if(TextUtils.isEmpty(groupName)){
                ToastUtils.showCenterToast("" ,getString(R.string.em_group_name_is_not_empty), 0, Toast.LENGTH_SHORT);
                return;
            }

            if(TextUtils.isEmpty(groupIntroduction)){
                ToastUtils.showCenterToast("" ,getString(R.string.em_group_intro_is_not_empty), 0, Toast.LENGTH_SHORT);
                return;
            }

            viewModel.createGroup(groupName, groupIntroduction, customers, waiters);
        }
    }

    private void showIntroductionDialog() {
        GroupEditFragment.showDialog(mContext,
                getString(R.string.em_chat_group_detail_introduction),
                groupIntroduction,
                getString(R.string.em_chat_group_detail_introduction_hint),
                true,
                new GroupEditFragment.OnSaveClickListener() {
                    @Override
                    public void onSaveClick(View view, String content) {
                        groupIntroduction = content;
                        if(!TextUtils.isEmpty(groupIntroduction)){
                            itemGroupIntroduction.getTvBContent().setText(groupIntroduction);
                        }
                    }
                });
    }

    private void showGroupNameDialog() {
        GroupEditFragment.showDialog(mContext,
                getString(R.string.em_chat_group_detail_name),
                groupName,
                getString(R.string.em_chat_group_detail_name_hint),
                true,
                new GroupEditFragment.OnSaveClickListener() {
                    @Override
                    public void onSaveClick(View view, String content) {
                        groupName = content;
                        if(TextUtils.isEmpty(groupName)){
                            itemGroupName.getTvContent().setHint(getString(R.string.group_name));
                        } else {
                            itemGroupName.getTvContent().setText(groupName);
                        }

                    }
                });
    }
}
