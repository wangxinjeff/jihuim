package com.hyphenate.easeim.section.chat.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.OnResourceParseCallback;
import com.hyphenate.easeim.common.livedatas.LiveDataBus;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeim.section.chat.fragment.ChatFragment;
import com.hyphenate.easeim.section.chat.viewmodel.ChatViewModel;
import com.hyphenate.easeim.section.chat.viewmodel.MessageViewModel;
import com.hyphenate.easeim.section.conference.ConferenceInviteActivity;
import com.hyphenate.easeim.section.group.GroupHelper;
import com.hyphenate.easeim.section.group.activity.GroupDetailActivity;
import com.hyphenate.easeui.EaseIM;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.easeui.provider.EaseUserProfileProvider;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.easeui.widget.EaseTitleBar;
import com.hyphenate.util.EMLog;
import com.tbruyelle.rxpermissions2.RxPermissions;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class ChatActivity extends BaseInitActivity implements EaseTitleBar.OnBackPressListener, EaseTitleBar.OnRightClickListener, ChatFragment.OnFragmentInfoListener {
    private EaseTitleBar titleBarMessage;
    private String conversationId;
    private int chatType;
    private ChatFragment fragment;
    private String historyMsgId;
    private ChatViewModel viewModel;

    public static void actionStart(Context context, String conversationId, int chatType) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EaseConstant.EXTRA_CONVERSATION_ID, conversationId);
        intent.putExtra(EaseConstant.EXTRA_CHAT_TYPE, chatType);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {
        if(EaseIMHelper.getInstance().isAdmin()){
            setTheme(R.style.AdminChatTheme);
        } else {
            setTheme(R.style.CustomerChatTheme);
        }
        return R.layout.em_activity_chat;
    }

    @Override
    protected void initIntent(Intent intent) {
        super.initIntent(intent);
        conversationId = intent.getStringExtra(EaseConstant.EXTRA_CONVERSATION_ID);
        chatType = intent.getIntExtra(EaseConstant.EXTRA_CHAT_TYPE, EaseConstant.CHATTYPE_SINGLE);
        historyMsgId = intent.getStringExtra(EaseConstant.HISTORY_MSG_ID);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        titleBarMessage = findViewById(R.id.title_bar_message);
        if(EaseIMHelper.getInstance().isAdmin()){
            titleBarMessage.setLeftImageResource(R.drawable.em_icon_back_admin);
            titleBarMessage.setRightImageResource(R.drawable.em_icon_more_admin);
        }
        initChatFragment();
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(
                Manifest.permission.RECORD_AUDIO
        ).subscribe(granted -> {
            if(granted){
            }else{
                Toast.makeText(this, "请确认开启权限", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initChatFragment() {
        fragment = new ChatFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EaseConstant.EXTRA_CONVERSATION_ID, conversationId);
        bundle.putInt(EaseConstant.EXTRA_CHAT_TYPE, chatType);
        bundle.putString(EaseConstant.HISTORY_MSG_ID, historyMsgId);
        bundle.putBoolean(EaseConstant.EXTRA_IS_ROAM, false);
        fragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction().replace(R.id.fl_fragment, fragment, "chat").commit();
    }

    @Override
    protected void initListener() {
        super.initListener();
        titleBarMessage.setOnBackPressListener(this);
        titleBarMessage.setOnRightClickListener(this);
        fragment.setOnFragmentInfoListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent != null) {
            initIntent(intent);
            initChatFragment();
            initData();
        }
    }

    @Override
    protected void initData() {
        super.initData();
        if(EaseIMHelper.getInstance().isAdmin() && chatType == EaseConstant.CHATTYPE_GROUP){
            titleBarMessage.getConIdView().setText("群组ID：" + conversationId);
            titleBarMessage.getConIdView().setVisibility(View.VISIBLE);
        }
        EMConversation conversation = EMClient.getInstance().chatManager().getConversation(conversationId);
        MessageViewModel messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        viewModel.getDeleteObservable().observe(this, response -> {
            parseResource(response, new OnResourceParseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    finish();
                    EaseEvent event = EaseEvent.create(EaseConstant.CONVERSATION_DELETE, EaseEvent.TYPE.MESSAGE);
                    messageViewModel.setMessageChange(event);
                }
            });
        });
        messageViewModel.getMessageChange().with(EaseConstant.GROUP_CHANGE, EaseEvent.class).observe(this, event -> {
            if(event == null) {
                return;
            }
            if(event.isGroupLeave() && TextUtils.equals(conversationId, event.message)) {
                finish();
            } else if(event.isGroupNameChange() && TextUtils.equals(conversationId, event.message)){
                titleBarMessage.setTitle(GroupHelper.getGroupName(conversationId));
            }
        });
        messageViewModel.getMessageChange().with(EaseConstant.CHAT_ROOM_CHANGE, EaseEvent.class).observe(this, event -> {
            if(event == null) {
                return;
            }
            if(event.isChatRoomLeave() && TextUtils.equals(conversationId,  event.message)) {
                finish();
            }
        });
        messageViewModel.getMessageChange().with(EaseConstant.MESSAGE_FORWARD, EaseEvent.class).observe(this, event -> {
            if(event == null) {
                return;
            }
            if(event.isMessageChange()) {
                showSnackBar(event.event);
            }
        });
        messageViewModel.getMessageChange().with(EaseConstant.CONTACT_UPDATE, EaseEvent.class).observe(this, event -> {
            if(event == null) {
                return;
            }

            if(event.isContactChange() && chatType == EaseConstant.CHATTYPE_SINGLE) {
                setDefaultTitle();
            }
        });

        setDefaultTitle();
    }

    private void showSnackBar(String event) {
        Snackbar.make(titleBarMessage, event, Snackbar.LENGTH_SHORT).show();
    }

    private void setDefaultTitle() {
        String title;
        if(chatType == EaseConstant.CHATTYPE_GROUP) {
            title = GroupHelper.getGroupName(conversationId);
        }else {
            EaseUserProfileProvider userProvider = EaseIM.getInstance().getUserProvider();
            if(userProvider != null) {
                EaseUser user = userProvider.getUser(conversationId);
                if(user != null) {
                    title = user.getNickname();
                }else {
                    title = conversationId;
                }
            }else {
                title = conversationId;
            }
        }
        titleBarMessage.setTitle(title);
    }

    @Override
    public void onBackPress(View view) {
        onBackPressed();
    }

    @Override
    public void onRightClick(View view) {
        if(chatType == EaseConstant.CHATTYPE_SINGLE) {
            //跳转到单聊设置页面
            SingleChatSetActivity.actionStart(mContext, conversationId);
        }else {
            // 跳转到群组设置
            if(chatType == EaseConstant.CHATTYPE_GROUP) {
                GroupDetailActivity.actionStart(mContext, conversationId);
            }
        }
    }

    @Override
    public void onChatError(int code, String errorMsg) {
        if(code == EMError.USER_MUTED){
            showToast(getString(R.string.em_group_yourself_mute));
        } else {
            showToast(getString(R.string.send_failure_please));
        }
    }

    @Override
    public void onOtherTyping(String action) {
        if (TextUtils.equals(action, "TypingBegin")) {
            titleBarMessage.setTitle(getString(R.string.alert_during_typing));
        }else if(TextUtils.equals(action, "TypingEnd")) {
            setDefaultTitle();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        EaseIMHelper.getInstance().setChatPageConId(conversationId);
        setDefaultTitle();
    }

    @Override
    protected void onPause() {
        super.onPause();
        EaseIMHelper.getInstance().setChatPageConId("");
    }
}
