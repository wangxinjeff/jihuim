package com.hyphenate.easeim.section.group.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.support.annotation.Nullable;

import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeim.common.livedatas.LiveDataBus;
import com.hyphenate.easeim.common.repositories.EMGroupManagerRepository;
import com.hyphenate.easeim.common.repositories.EMPushManagerRepository;
import com.hyphenate.easeim.common.widget.ArrowItemView;
import com.hyphenate.easeim.common.widget.SwitchItemView;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeim.section.group.GroupHelper;
import com.hyphenate.easeim.section.group.adapter.GroupDetailMemberAdapter;
import com.hyphenate.easeim.section.search.SearchHistoryChatActivity;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.manager.EaseThreadManager;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.easeui.widget.EaseImageView;
import com.hyphenate.easeui.widget.EaseTitleBar;
import com.hyphenate.exceptions.HyphenateException;

import java.util.ArrayList;
import java.util.List;

public class GroupDetailActivity extends BaseInitActivity implements EaseTitleBar.OnBackPressListener, View.OnClickListener, SwitchItemView.OnCheckedChangeListener {
    private static final int REQUEST_CODE_GROUP_NAME = 0;
    private static final int REQUEST_CODE_GROUP_NOTICE = 1;
    private static final int REQUEST_CODE_GROUP_INTRO = 2;
    private static final int REQUEST_CODE_GROUP_MUTE = 3;
    private static final int REQUEST_CODE_GROUP_NOTE = 4;
    private EaseTitleBar titleBar;
    private View groupInfo;
    private EaseImageView ivGroupAvatar;
    private ImageView iconNext;
    private TextView tvGroupMemberTitle;
    private TextView tvGroupMemberNum;
    private ArrowItemView itemGroupName;
    private ArrowItemView itemGroupOwner;
    private ArrowItemView itemGroupNotice;
    private ArrowItemView itemGroupIntroduction;
    private ArrowItemView itemGroupMute;
    private ArrowItemView itemGroupNote;
    private ArrowItemView itemGroupHistory;
    private SwitchItemView itemGroupNotDisturb;
    private String groupId;
    private EMGroup group;
    private EMConversation conversation;
    private GroupDetailMemberAdapter memberAdapter;
    private RecyclerView memberList;
    private LinearLayout showMore;
    private EMGroupManagerRepository repository = EMGroupManagerRepository.getInstance();

    public static void actionStart(Context context, String groupId) {
        Intent intent = new Intent(context, GroupDetailActivity.class);
        intent.putExtra("groupId", groupId);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.demo_activity_chat_group_detail;
    }

    @Override
    protected void initIntent(Intent intent) {
        super.initIntent(intent);
        groupId = intent.getStringExtra("groupId");
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        titleBar = findViewById(R.id.title_bar);
        groupInfo = findViewById(R.id.cl_group_info);
        ivGroupAvatar = findViewById(R.id.iv_group_avatar);
        iconNext = findViewById(R.id.icon_next);
        tvGroupMemberTitle = findViewById(R.id.tv_group_member_title);
        tvGroupMemberNum = findViewById(R.id.tv_group_member_num);
        itemGroupName = findViewById(R.id.item_group_name);
        itemGroupOwner = findViewById(R.id.item_group_owner);
        itemGroupNotice = findViewById(R.id.item_group_notice);
        itemGroupIntroduction = findViewById(R.id.item_group_introduction);
        itemGroupMute = findViewById(R.id.item_group_mute);
        itemGroupNote = findViewById(R.id.item_group_note);
        itemGroupHistory = findViewById(R.id.item_group_history);
        itemGroupNotDisturb = findViewById(R.id.item_group_not_disturb);
        memberList = findViewById(R.id.rl_member_list);
        memberAdapter = new GroupDetailMemberAdapter();
        memberList.setLayoutManager(new GridLayoutManager(this, 6));
        memberList.setAdapter(memberAdapter);
        showMore = findViewById(R.id.show_more_member);

        group = EaseIMHelper.getInstance().getGroupManager().getGroup(groupId);

        if(EaseIMHelper.getInstance().isAdmin()){
            titleBar.setLeftImageResource(R.drawable.em_icon_back_admin);
            itemGroupNote.setVisibility(View.VISIBLE);
            itemGroupIntroduction.setItemShowDivider(true);
            if(isOwner()){
//                iconNext.setVisibility(View.VISIBLE);
                itemGroupName.setItemShowArrow(true);
                itemGroupMute.setVisibility(View.VISIBLE);
            }
        }

        initGroupView();
    }

    @Override
    protected void initListener() {
        super.initListener();
        titleBar.setOnBackPressListener(this);
        if(EaseIMHelper.getInstance().isAdmin() && isOwner()){
            groupInfo.setOnClickListener(this);
            itemGroupName.setOnClickListener(this);
        }
        itemGroupNotice.setOnClickListener(this);
        itemGroupIntroduction.setOnClickListener(this);
        itemGroupMute.setOnClickListener(this);
        itemGroupNote.setOnClickListener(this);
        itemGroupHistory.setOnClickListener(this);
        itemGroupNotDisturb.setOnCheckedChangeListener(this);
        showMore.setOnClickListener(this);
        memberAdapter.setOnAddClickListener(new GroupDetailMemberAdapter.GroupMemberAddClickListener() {
            @Override
            public void onAddClick() {
                GroupPickContactsActivity.actionStart(mContext, groupId, isOwner(), false);
            }

            @Override
            public void onRemoveClick() {
            }
        });
    }

    private void initGroupView() {
        if(group == null) {
            finish();
            return;
        }
        itemGroupName.getTvContent().setText(group.getGroupName());
        EaseUserUtils.setUserNick(group.getOwner(), itemGroupOwner.getTvContent());
        tvGroupMemberNum.setText(getString(R.string.em_chat_group_detail_member_num, group.getMemberCount()));
        conversation = EaseIMHelper.getInstance().getConversation(groupId, EMConversation.EMConversationType.GroupChat, true);

//        itemGroupIntroduction.getTvContent().setText(group.getDescription());

//        makeTextSingleLine(itemGroupNotice.getTvContent());
//        makeTextSingleLine(itemGroupIntroduction.getTvContent());

        if(!TextUtils.isEmpty(group.getDescription())){
            itemGroupIntroduction.getTvBContent().setText(group.getDescription());
        }


        List<String> disabledIds = EaseIMHelper.getInstance().getPushManager().getNoPushGroups();
        itemGroupNotDisturb.getSwitch().setChecked(disabledIds != null && disabledIds.contains(groupId));
    }

    @Override
    protected void initData() {
        super.initData();
        LiveDataBus.get().with(EaseConstant.GROUP_CHANGE, EaseEvent.class).observe(this, event -> {
            if(event.isGroupLeave() && TextUtils.equals(groupId, event.message)) {
                finish();
                return;
            }
            if(event.isGroupChange() && TextUtils.equals(groupId, event.message)) {
                loadGroup();
            }
        });
        loadGroup();

        LiveDataBus.get().with(EaseConstant.CONTACT_UPDATE, EaseEvent.class).observe(this, event -> {
            if(event == null) {
                return;
            }
            if(event.isContactChange()) {
                EaseUserUtils.setUserNick(group.getOwner(), itemGroupOwner.getTvContent());
                if(memberAdapter != null){
                    memberAdapter.notifyDataSetChanged();
                }
            }
        });

    }

    private void loadGroup() {
        showLoading();
        getGroupFromServer();
        getGroupAnnouncement();
        getGroupAllMember();

    }

    private void getGroupFromServer(){
        new EMPushManagerRepository().getPushConfigsFromServer();
        EaseIMHelper.getInstance().getGroupManager().asyncGetGroupFromServer(groupId, new EMValueCallBack<EMGroup>() {
            @Override
            public void onSuccess(EMGroup value) {
                runOnUiThread(() -> {
                    group = value;
                    initGroupView();
                });
            }

            @Override
            public void onError(int error, String errorMsg) {
            }
        });
    }

    private void getGroupAnnouncement(){
        EaseIMHelper.getInstance().getGroupManager().asyncFetchGroupAnnouncement(groupId, new EMValueCallBack<String>() {
            @Override
            public void onSuccess(String value) {
                runOnUiThread(() -> {
                    itemGroupNotice.getTvBContent().setText(value);
                });
            }

            @Override
            public void onError(int error, String errorMsg) {
            }
        });
    }

    private void getGroupAllMember() {
        repository.getGroupAllMembers(groupId, new ResultCallBack<List<EaseUser>>() {
            @Override
            public void onSuccess(List<EaseUser> easeUsers) {
                dismissLoading();
                runOnUiThread(() ->
                {
                    EaseUser addUser = new EaseUser("em_addUser");
                    addUser.setNickname(getString(R.string.em_add_member));
                    easeUsers.add(0, addUser);
                    memberAdapter.setData(easeUsers);
                });
            }

            @Override
            public void onError(int i, String s) {
                dismissLoading();
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();// 群成员
        if (id == R.id.cl_group_info) {
        } else if (id == R.id.show_more_member) {
            GroupMemberTypeActivity.actionStart(mContext, groupId, isOwner());
        } else if (id == R.id.item_group_name) {//群名称
//                showGroupNameDialog();
            GroupEditActivity.startEditForResult(mContext, getString(R.string.em_chat_group_detail_name),
                    group.getGroupName(),
                    GroupHelper.isOwner(group) ? getString(R.string.em_chat_group_detail_name_hint) : "",
                    EaseIMHelper.getInstance().isAdmin() && GroupHelper.isOwner(group), groupId, REQUEST_CODE_GROUP_NAME);
        } else if (id == R.id.item_group_notice) {//群公告
//                showAnnouncementDialog();
            GroupEditActivity.startEditForResult(mContext, getString(R.string.em_chat_group_detail_announcement),
                    group.getAnnouncement(),
                    GroupHelper.isOwner(group) ? getString(R.string.em_chat_group_detail_announcement_hint) : "",
                    EaseIMHelper.getInstance().isAdmin() && GroupHelper.isOwner(group),groupId,  REQUEST_CODE_GROUP_NOTICE);
        } else if (id == R.id.item_group_introduction) {//群介绍
//                showIntroductionDialog();
            GroupEditActivity.startEditForResult(mContext, getString(R.string.em_chat_group_detail_introduction),
                    group.getDescription(),
                    GroupHelper.isOwner(group) ? getString(R.string.em_chat_group_detail_introduction_hint) : "",
                    EaseIMHelper.getInstance().isAdmin() && GroupHelper.isOwner(group), groupId, REQUEST_CODE_GROUP_INTRO);
        } else if (id == R.id.item_group_mute) {
            GroupMuteActivity.startAction(mContext, groupId);
        } else if (id == R.id.item_group_note) {
            GroupEditActivity.startNoteForResult(mContext, getString(R.string.em_group_note),
                    "",
                    "",
                    getString(R.string.em_group_note_hint),
                    groupId,
                    REQUEST_CODE_GROUP_NOTE
            );
        } else if (id == R.id.item_group_history) {//查找聊天记录
            SearchHistoryChatActivity.actionStart(mContext, groupId, EaseConstant.CHATTYPE_GROUP);
        }
    }

    @Override
    public void onCheckedChanged(SwitchItemView buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.item_group_not_disturb) {//消息免打扰
            updatePushServiceForGroup(isChecked);
        }
    }

    private void updatePushServiceForGroup(boolean isChecked){
        EaseThreadManager.getInstance().runOnIOThread(()-> {
            List<String> onPushList = new ArrayList<>();
            onPushList.add(groupId);
            try {
                EaseIMHelper.getInstance().getPushManager().updatePushServiceForGroup(onPushList, isChecked);
                EMMessage message = EMMessage.createSendMessage(EMMessage.Type.CMD);
                EMCmdMessageBody body = new EMCmdMessageBody("event");
                body.deliverOnlineOnly(true);
                message.addBody(body);
                message.setTo(EaseIMHelper.getInstance().getCurrentUser());
                message.setAttribute(EaseConstant.MESSAGE_ATTR_EVENT_TYPE, EaseConstant.EVENT_TYPE_GROUP_NO_PUSH);
                message.setAttribute(EaseConstant.MESSAGE_ATTR_NO_PUSH, isChecked);
                message.setAttribute(EaseConstant.MESSAGE_ATTR_NO_PUSH_ID, groupId);
                EMClient.getInstance().chatManager().sendMessage(message);
            } catch (HyphenateException e) {
                e.printStackTrace();
            }
            getGroupFromServer();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            String content = data.getStringExtra("content");
            switch (requestCode) {
                case REQUEST_CODE_GROUP_NAME:
//                    //修改群名称
//                    viewModel.setGroupName(groupId, content);
                    itemGroupName.getTvContent().setText(content);
                    break;
                case REQUEST_CODE_GROUP_NOTICE:
//                    //修改群公告
//                    viewModel.setGroupAnnouncement(groupId, content);
                    itemGroupNotice.getTvBContent().setText(content);
                    break;
                case REQUEST_CODE_GROUP_INTRO:
//                    //修改群介绍
//                    viewModel.setGroupDescription(groupId, content);
                    itemGroupIntroduction.getTvBContent().setText(content);
                    break;
                case REQUEST_CODE_GROUP_NOTE:
//                    //修改运营备注
//                    viewModel.changeServiceNote(groupId, content);
                    break;

            }
        }
    }

    private void makeTextSingleLine(TextView tv) {
        tv.setMaxLines(1);
        tv.setEllipsize(TextUtils.TruncateAt.END);
    }

    @Override
    public void onBackPress(View view) {
        onBackPressed();
    }

    /**
     * 是否有邀请权限
     * @return
     */
    private boolean isCanInvite() {
        return GroupHelper.isCanInvite(group);
    }

    /**
     * 是否是管理员
     * @return
     */
    private boolean isAdmin() {
        return GroupHelper.isAdmin(group);
    }

    /**
     * 是否是群主
     * @return
     */
    private boolean isOwner() {
        return GroupHelper.isOwner(group);
    }
}
