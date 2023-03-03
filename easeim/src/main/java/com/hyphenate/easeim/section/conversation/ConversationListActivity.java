package com.hyphenate.easeim.section.conversation;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easecallkit.base.EaseCallType;

import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.livedatas.LiveDataBus;
import com.hyphenate.easeim.common.permission.PermissionsManager;
import com.hyphenate.easeim.common.permission.PermissionsResultAction;
import com.hyphenate.easeim.common.utils.PushUtils;
import com.hyphenate.easeim.common.utils.ToastUtils;
import com.hyphenate.easeim.section.av.MultipleVideoActivity;
import com.hyphenate.easeim.section.av.VideoCallActivity;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeim.section.chat.ChatPresenter;
import com.hyphenate.easeim.section.group.activity.GroupApplyActivity;
import com.hyphenate.easeim.section.group.activity.NewGroupActivity;
import com.hyphenate.easeim.section.search.SearchGroupChatActivity;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.easeui.widget.EaseTitleBar;
import com.hyphenate.util.EMLog;

import java.util.ArrayList;


import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;


public class ConversationListActivity extends BaseInitActivity implements EaseTitleBar.OnBackPressListener, View.OnClickListener{
    private EaseTitleBar mTitleBar;
    private ConversationListFragment mConversationListFragment;
    private boolean showMenu = true;//是否显示菜单项
    private int conversationsType = EaseConstant.CON_TYPE_EXCLUSIVE;

    private View popView;
    private LinearLayout notifyView;
    private LinearLayout searchView;
    private LinearLayout createView;
    private LinearLayout applyView;
    private PopupWindow popupWindow;
    private AppCompatImageView notifyIcon;
    private AppCompatImageView titleBarUnread;
    private AppCompatImageView applyUnread;
    private int xoff;

    private boolean isNotify;

    private LinearLayout disconnectView;

    public static void actionStart(Context context, int conversationsType){
        Intent intent = new Intent(context, ConversationListActivity.class);
        intent.putExtra(EaseConstant.EXTRA_CONVERSATIONS_TYPE, conversationsType);
        context.startActivity(intent);
    }

    @Override
    protected void initIntent(Intent intent) {
        super.initIntent(intent);
        conversationsType = intent.getIntExtra(EaseConstant.EXTRA_CONVERSATIONS_TYPE, EaseConstant.CON_TYPE_EXCLUSIVE);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.em_activity_conversation_list;
    }

    @Override
    protected void initSystemFit() {
        setFitSystemForTheme(false);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        mTitleBar = findViewById(R.id.title_bar_main);
        titleBarUnread = findViewById(R.id.apply_unread);
        if(conversationsType == EaseConstant.CON_TYPE_EXCLUSIVE){
            mTitleBar.setTitle(getString(R.string.my_exclusive_service));
        } else if(conversationsType == EaseConstant.CON_TYPE_MY_CHAT){
            mTitleBar.setTitle(getString(R.string.my_chat));
        } else if(conversationsType == EaseConstant.CON_TYPE_ADMIN){
            mTitleBar.setTitle(getString(R.string.my_conversations));
            mTitleBar.setLeftImageResource(R.drawable.em_icon_back_admin);
            mTitleBar.setRightImageResource(R.drawable.em_home_menu_add);
        }
        disconnectView = findViewById(R.id.disconnect_view);
        switchToHome();

        popView = LayoutInflater.from(this).inflate(R.layout.em_pop_conversation_list, null, false);
        notifyView = popView.findViewById(R.id.notify_view);
        searchView = popView.findViewById(R.id.search_view);
        createView = popView.findViewById(R.id.create_view);
        applyView = popView.findViewById(R.id.apply_view);
        notifyIcon = popView.findViewById(R.id.notify_icon);
        applyUnread = popView.findViewById(R.id.apply_unread_icon);

        showNotifyIcon();

        popupWindow = new PopupWindow(popView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setTouchable(true);
        // 如果不设置PopupWindow的背景，有些版本就会出现一个问题：无论是点击外部区域还是Back键都无法dismiss弹框
        // 这里单独写一篇文章来分析
        popupWindow.setBackgroundDrawable(new ColorDrawable());
        popView.measure(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int popupWidth = popView.getMeasuredWidth();
        int mScreenWidth  = this.getResources().getDisplayMetrics().widthPixels;
        xoff = mScreenWidth - popupWidth - (int)EaseCommonUtils.dip2px(mContext, 40);

    }

    @Override
    protected void initListener() {
        super.initListener();
        mTitleBar.setOnBackPressListener(this);
        mTitleBar.setRightLayoutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int yoff = (int)EaseCommonUtils.dip2px(mContext, 9);
                popupWindow.showAsDropDown(mTitleBar, xoff, -yoff);
            }
        });
        notifyView.setOnClickListener(this);
        searchView.setOnClickListener(this);
        createView.setOnClickListener(this);
        applyView.setOnClickListener(this);
    }

    @Override
    protected void initData() {
        super.initData();
        initViewModel();
        ChatPresenter.getInstance().init();
        if(EaseIMHelper.getInstance().isAdmin()){
            if(!TextUtils.isEmpty(EaseIMHelper.getInstance().getModel().getDeviceToken())){
                EMClient.getInstance().pushManager().bindDeviceToken(EaseIMHelper.getInstance().getModel().getNotifierName(), EaseIMHelper.getInstance().getModel().getDeviceToken(), new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        EMLog.e("EMPushManager", "bindDeviceToken success");
                    }

                    @Override
                    public void onError(int i, String s) {
                        EMLog.e("EMPushManager", "bindDeviceToken failed: " + i + ", " +s);
                    }
                });
            }
        }

        //判断是否为来电推送
        if(PushUtils.isRtcCall){
            if (EaseCallType.getfrom(PushUtils.type) != EaseCallType.CONFERENCE_CALL) {
                    Intent intent = new Intent(getApplicationContext(), VideoCallActivity.class).addFlags(FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(intent);
                } else {
                    Intent intent = new Intent(getApplication().getApplicationContext(), MultipleVideoActivity.class).addFlags(FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(intent);
            }
            PushUtils.isRtcCall  = false;
        }

        LiveDataBus.get().with(EaseConstant.MESSAGE_CHANGE_CHANGE, EaseEvent.class).observe(this, event -> {
            if(event == null) {
                return;
            }
            if(event.isMessageChange()){
                if(TextUtils.equals(EaseConstant.MESSAGE_CHANGE_CMD_RECEIVE, event.event)){
                    if(TextUtils.equals(EaseConstant.NEW_GROUP_APPLY, event.message)){
                        showRedIcon(View.VISIBLE);
                    }
                }
            }
        });
        LiveDataBus.get().with(EaseConstant.ACCOUNT_CHANGE, EaseEvent.class).observe(this, event -> {
            if(event == null) {
                return;
            }
            if(!event.isAccountChange()) {
                return;
            }

            String accountEvent = event.event;
            if(TextUtils.equals(accountEvent, EaseConstant.ACCOUNT_DIS)){
                if(!EMClient.getInstance().isConnected()){
                    disconnectView.setVisibility(View.VISIBLE);
                }
            } else if (TextUtils.equals(accountEvent, EaseConstant.ACCOUNT_CONNECT)){
                disconnectView.setVisibility(View.GONE);
            } else {
                if(!EMClient.getInstance().isConnected()){
                    disconnectView.setVisibility(View.VISIBLE);
                }
            }
        });

    }

    private void initViewModel() {

    }

    private void switchToHome() {
        if(mConversationListFragment == null) {
            mConversationListFragment = new ConversationListFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("conversationsType", conversationsType);
            mConversationListFragment.setArguments(bundle);
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.fl_main_fragment, mConversationListFragment, "conversation").commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        EaseIMHelper.getInstance().showNotificationPermissionDialog();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPress(View view) {
        onBackPressed();

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.notify_view) {
            EaseIMHelper.getInstance().getModel().setSettingMsgNotification(!isNotify);
            showNotifyIcon();
            if (isNotify) {
                ToastUtils.showCenterToast("", getString(R.string.em_open_notify), 0, Toast.LENGTH_SHORT);
            } else {
                ToastUtils.showCenterToast("", getString(R.string.em_close_notify), 0, Toast.LENGTH_SHORT);
            }
        } else if (id == R.id.search_view) {
            startActivity(new Intent(this, SearchGroupChatActivity.class));
        } else if (id == R.id.create_view) {
            NewGroupActivity.actionStart(mContext, new ArrayList<EaseUser>());
//            GroupPickContactsActivity.actionStart(mContext, "", true, true);
        } else if (id == R.id.apply_view) {
            startActivity(new Intent(ConversationListActivity.this, GroupApplyActivity.class));
            showRedIcon(View.GONE);
        }
        popupWindow.dismiss();
    }

    private void showNotifyIcon(){
        isNotify = EaseIMHelper.getInstance().getModel().getSettingMsgNotification();
        if(isNotify){
            notifyIcon.setImageResource(R.drawable.em_icon_message_notify);
        } else {
            notifyIcon.setImageResource(R.drawable.em_icon_message_unnotify);
        }
    }

    private void showRedIcon(int visible){
        titleBarUnread.setVisibility(visible);
        applyUnread.setVisibility(visible);
    }

    /**
     * 申请权限
     */
    // TODO: 2019/12/19 0019 有必要修改一下
    private void requestPermissions() {
        PermissionsManager.getInstance()
                .requestAllManifestPermissionsIfNecessary(mContext, new PermissionsResultAction() {
                    @Override
                    public void onGranted() {

                    }

                    @Override
                    public void onDenied(String permission) {

                    }
                });
    }
}
