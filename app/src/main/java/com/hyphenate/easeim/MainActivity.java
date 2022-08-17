package com.hyphenate.easeim;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;

import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.easeim.common.livedatas.LiveDataBus;
import com.hyphenate.easeim.common.permission.PermissionsManager;
import com.hyphenate.easeim.common.permission.PermissionsResultAction;
import com.hyphenate.easeim.common.widget.InAppNotification;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.model.EaseEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;


public class MainActivity extends BaseInitActivity implements View.OnClickListener {
    private AppCompatTextView groupChat;
    private AppCompatTextView groupUnread;
    private AppCompatTextView userChat;
    private AppCompatTextView chatUnread;
    private AppCompatEditText chatId;


    @Override
    protected int getLayoutId() {
        return R.layout.demo_activity_main;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        groupChat = findViewById(R.id.group_chat);
        groupUnread = findViewById(R.id.group_unread);
        userChat = findViewById(R.id.user_chat);
        chatUnread = findViewById(R.id.chat_unread);
        chatId = findViewById(R.id.chat_id);
//        requestPermissions();
    }

    @Override
    protected void initData() {
        super.initData();
        LiveDataBus.get().with(EaseConstant.MESSAGE_CHANGE_CHANGE, EaseEvent.class).observe(this, event -> {
            if(event == null) {
                return;
            }
            if(event.isMessageChange()) {
                refreshUI();
            }
        });

        refreshUI();
    }

    private void refreshUI(){
        EaseIMHelper.getInstance().getChatUnread(new EMValueCallBack<Map<String, Integer>>() {
            @Override
            public void onSuccess(Map<String, Integer> stringIntegerMap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        groupUnread.setText(stringIntegerMap.get(EaseConstant.UNREAD_EXCLUSIVE_GROUP).toString());
                        chatUnread.setText(stringIntegerMap.get(EaseConstant.UNREAD_MY_CHAT)+"");
                    }
                });
            }

            @Override
            public void onError(int i, String s) {

            }
        });
    }

    @Override
    protected void initListener() {
        super.initListener();
        groupChat.setOnClickListener(this);
        userChat.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.group_chat:
                EaseIMHelper.getInstance().startChat(MainActivity.this, EaseConstant.CON_TYPE_EXCLUSIVE);
                break;
            case R.id.user_chat:
                if(chatId.getText().toString().isEmpty()){
                    return;
                }
                EMConversation conversation1 = EMClient.getInstance().chatManager().getConversation(chatId.getText().toString(), EMConversation.EMConversationType.Chat, true);
//                EMMessage message = EMMessage.createTextSendMessage("聊天测试", chatId.getText().toString());
//                message.setStatus(EMMessage.Status.SUCCESS);
//                conversation1.insertMessage(message);
                EaseIMHelper.getInstance().startChat(MainActivity.this, EaseConstant.CON_TYPE_MY_CHAT);
                break;
        }
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

    @Override
    protected void onResume() {
        super.onResume();
//        InAppNotification.getInstance().init(this);
    }


    @Override
    protected void onPause() {
        super.onPause();
//        InAppNotification.getInstance().hideNotification();
    }

}
