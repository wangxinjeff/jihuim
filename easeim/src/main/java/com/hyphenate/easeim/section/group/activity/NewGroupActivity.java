package com.hyphenate.easeim.section.group.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMGroup;

import com.hyphenate.chat.EMGroupOptions;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.OnResourceParseCallback;
import com.hyphenate.easeim.common.utils.ToastUtils;
import com.hyphenate.easeim.common.widget.ArrowItemView;

import com.hyphenate.easeim.section.base.BaseInitActivity;

import com.hyphenate.easeim.section.dialog.SimpleDialogFragment;
import com.hyphenate.easeim.section.group.viewmodels.NewGroupViewModel;

import com.hyphenate.easeim.section.group.adapter.GroupDetailMemberAdapter;
import com.hyphenate.easeim.section.group.fragment.GroupEditFragment;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.widget.EaseTitleBar;


import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import static com.hyphenate.chat.EMGroupManager.EMGroupStyle.EMGroupStylePrivateOnlyOwnerInvite;

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

    public static void actionStart(Context context,  ArrayList<EaseUser> members) {
        Intent intent = new Intent(context, NewGroupActivity.class);
        intent.putParcelableArrayListExtra("members", members);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.demo_activity_new_group;
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

        if(members.size() <  2){
            confirmBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.btn_gray_pressed));
            confirmBtn.setEnabled(false);
            confirmBtn.setText(getString(R.string.em_must_be_no_less_than_2_members));
        } else {
            confirmBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.search_close));
            confirmBtn.setEnabled(true);
            confirmBtn.setText(getString(R.string.em_group_new_save));
        }

        tvGroupMemberNum.setText(members.size() + "人");

        viewModel = new ViewModelProvider(this).get(NewGroupViewModel.class);
        viewModel.groupObservable().observe(this, response -> {
            parseResource(response, new OnResourceParseCallback<String>() {
                @Override
                public void onSuccess(String groupId) {
                    runOnUiThread(NewGroupActivity.this::finish);
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

                }
            });
        });

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
            if(TextUtils.isEmpty(groupName)){
                ToastUtils.showCenterToast("" ,getString(R.string.em_group_name_is_not_empty), 0, Toast.LENGTH_SHORT);
                return;
            }
            EMGroupOptions option = new EMGroupOptions();
            option.style = EMGroupStylePrivateOnlyOwnerInvite;
            List<String> customers = new ArrayList<>();
            List<String> waiters = new ArrayList<>();
            for (EaseUser user : members) {
                if(user.isCustomer()){
                    customers.add(user.getUsername());
                } else {
                    waiters.add(user.getUsername());
                }
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
