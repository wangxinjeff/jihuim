package com.hyphenate.easeim.section.chat.activity;

import android.arch.lifecycle.ViewModelProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.OnResourceParseCallback;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeim.common.livedatas.LiveDataBus;
import com.hyphenate.easeim.common.repositories.EMChatManagerRepository;
import com.hyphenate.easeim.common.widget.ArrowItemView;
import com.hyphenate.easeim.common.widget.SwitchItemView;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeim.section.search.SearchHistoryChatActivity;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.easeui.widget.EaseTitleBar;

import java.util.List;

public class SingleChatSetActivity extends BaseInitActivity implements EaseTitleBar.OnBackPressListener, View.OnClickListener, SwitchItemView.OnCheckedChangeListener {
    private EaseTitleBar titleBar;
    private ArrowItemView itemUserInfo;
    private ArrowItemView itemSearchHistory;
    private ArrowItemView itemClearHistory;
    private SwitchItemView itemSwitchTop;
    private SwitchItemView itemUserNotDisturb;

    private String toChatUsername;
    private EMConversation conversation;

    public static void actionStart(Context context, String toChatUsername) {
        Intent intent = new Intent(context, SingleChatSetActivity.class);
        intent.putExtra("toChatUsername", toChatUsername);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.demo_activity_single_chat_set;
    }

    @Override
    protected void initIntent(Intent intent) {
        super.initIntent(intent);
        toChatUsername = getIntent().getStringExtra("toChatUsername");
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        titleBar = findViewById(R.id.title_bar);
        if(EaseIMHelper.getInstance().isAdmin()){
            titleBar.setLeftImageResource(R.drawable.icon_back_admin);
        }
        itemUserInfo = findViewById(R.id.item_user_info);
        itemSearchHistory = findViewById(R.id.item_search_history);
        itemClearHistory = findViewById(R.id.item_clear_history);
        itemSwitchTop = findViewById(R.id.item_switch_top);
        itemUserNotDisturb = findViewById(R.id.item_user_not_disturb);
    }

    @Override
    protected void initListener() {
        super.initListener();
        titleBar.setOnBackPressListener(this);
        itemSearchHistory.setOnClickListener(this);
        itemClearHistory.setOnClickListener(this);
        itemSwitchTop.setOnCheckedChangeListener(this);
        itemUserNotDisturb.setOnCheckedChangeListener(this);
    }

    @Override
    protected void initData() {
        super.initData();
        conversation = EMClient.getInstance().chatManager().getConversation(toChatUsername, EaseCommonUtils.getConversationType(EaseConstant.CHATTYPE_SINGLE), true);
        itemUserInfo.getAvatar().setShapeType(1);
        itemUserInfo.getTvTitle().setText(toChatUsername);

//        EaseUserProfileProvider provider = EaseIM.getInstance().getUserProvider();
//        if(provider != null){
//            EaseUser user = provider.getUser(toChatUsername);
//            if(user != null){
//                itemUserInfo.getTvTitle().setText(user.getNickname());
//                Glide.with(mContext).load(user.getAvatar())
//                        .apply(RequestOptions.placeholderOf(R.drawable.ease_default_avatar)
//                                .diskCacheStrategy(DiskCacheStrategy.ALL))
//                        .into(itemUserInfo.getAvatar());
//            }
//        }
        EaseUserUtils.setUserAvatar(this, toChatUsername, itemUserInfo.getAvatar());
        EaseUserUtils.setUserNick(toChatUsername, itemUserInfo.getTvTitle());

        itemSwitchTop.getSwitch().setChecked(!TextUtils.isEmpty(conversation.getExtField()));
        EMChatManagerRepository.getInstance().getNoPushUsers(new ResultCallBack<List<String>>() {
            @Override
            public void onSuccess(List<String> noPushUsers) {
                if (noPushUsers.contains(toChatUsername)) {
                    itemUserNotDisturb.getSwitch().setChecked(true);
                }else{
                    itemUserNotDisturb.getSwitch().setChecked(false);
                }
            }

            @Override
            public void onError(int i, String s) {

            }
        });
    }

    @Override
    public void onBackPress(View view) {
        onBackPressed();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.item_user_info) {

        } else if (id == R.id.item_search_history) {
            SearchHistoryChatActivity.actionStart(mContext, toChatUsername, EaseConstant.CHATTYPE_SINGLE);
        } else if (id == R.id.item_clear_history) {
            clearHistory();
        }
    }

    private void clearHistory() {

    }

    @Override
    public void onCheckedChanged(SwitchItemView buttonView, boolean isChecked) {
        int id = buttonView.getId();
        if (id == R.id.item_switch_top) {
            conversation.setExtField(isChecked ? (System.currentTimeMillis() + "") : "");
            LiveDataBus.get().with(EaseConstant.MESSAGE_CHANGE_CHANGE).postValue(new EaseEvent(EaseConstant.MESSAGE_CHANGE_CHANGE, EaseEvent.TYPE.MESSAGE));
        } else if (id == R.id.item_user_not_disturb) {
            EMChatManagerRepository.getInstance().setUserNotDisturb(toChatUsername, isChecked, new EMCallBack() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onError(int i, String s) {

                }
            });
        }
    }
}