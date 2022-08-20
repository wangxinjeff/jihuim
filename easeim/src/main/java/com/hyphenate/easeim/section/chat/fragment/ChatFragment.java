package com.hyphenate.easeim.section.chat.fragment;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import android.support.annotation.Nullable;
import android.widget.Toast;

import com.hyphenate.chat.EMCustomMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.livedatas.LiveDataBus;
import com.hyphenate.easeim.common.model.EMOrder;
import com.hyphenate.easeim.section.chat.activity.PickAtUserActivity;
import com.hyphenate.easeim.section.conference.ConferenceInviteActivity;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.easeui.modules.chat.EaseChatFragment;
import com.hyphenate.easeui.modules.chat.interfaces.IChatExtendMenu;
import com.hyphenate.easeui.modules.chat.interfaces.OnRecallMessageResultListener;
import com.hyphenate.easeui.modules.menu.EasePopupWindowHelper;
import com.hyphenate.easeui.modules.menu.MenuItemBean;
import com.hyphenate.easeui.ui.EaseDingAckUserListActivity;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.HashMap;
import java.util.Map;


public class ChatFragment extends EaseChatFragment implements OnRecallMessageResultListener {
    private static final String TAG = ChatFragment.class.getSimpleName();
    private static final int REQUEST_CODE_ORDER = 21;
    protected ClipboardManager clipboard;

    private static final int REQUEST_CODE_SELECT_AT_USER = 15;
    private OnFragmentInfoListener infoListener;
    private Dialog dialog;

    @Override
    public void initView() {
        super.initView();
        clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

    }

    private void resetChatExtendMenu() {
        IChatExtendMenu chatExtendMenu = chatLayout.getChatInputMenu().getChatExtendMenu();
        chatExtendMenu.clear();
            chatExtendMenu.registerMenuItem(R.string.attach_take_pic, R.drawable.em_icon_chat_camera, R.id.extend_item_take_picture);
            chatExtendMenu.registerMenuItem(R.string.attach_picture, R.drawable.em_icon_chat_image, R.id.extend_item_picture);
            chatExtendMenu.registerMenuItem(R.string.attach_location, R.drawable.em_icon_chat_location, R.id.extend_item_location);

            if (chatType == EaseConstant.CHATTYPE_GROUP) { // 音视频
                chatExtendMenu.registerMenuItem(R.string.attach_media_call, R.drawable.em_icon_chat_video_call, R.id.extend_item_call);
                chatExtendMenu.registerMenuItem(R.string.attach_file, R.drawable.em_icon_chat_file, R.id.extend_item_file);
        }
    }

    @Override
    public void initListener() {
        super.initListener();
        chatLayout.setOnRecallMessageResultListener(this);
    }

    @Override
    public void initData() {
        super.initData();
        resetChatExtendMenu();
//        addItemMenuAction();

        chatLayout.getChatInputMenu().getPrimaryMenu().getEditText().setText(getUnSendMsg());
        chatLayout.turnOnTypingMonitor(EaseIMHelper.getInstance().getModel().isShowMsgTyping());

        LiveDataBus.get().with(EaseConstant.MESSAGE_CHANGE_CHANGE).postValue(new EaseEvent(EaseConstant.MESSAGE_CHANGE_CHANGE, EaseEvent.TYPE.MESSAGE));

        LiveDataBus.get().with(EaseConstant.MESSAGE_CALL_SAVE, Boolean.class).observe(getViewLifecycleOwner(), event -> {
            if(event == null) {
                return;
            }
            if(event) {
                chatLayout.getChatMessageListLayout().refreshToLatest();
            }
        });

        LiveDataBus.get().with(EaseConstant.MESSAGE_CHANGE_CHANGE, EaseEvent.class).observe(getViewLifecycleOwner(), event -> {
            if(event == null) {
                return;
            }
            if(event.isMessageChange() && !TextUtils.equals(EaseConstant.MESSAGE_UNREAD_CHANGE, event.event)) {
                chatLayout.getChatMessageListLayout().refreshToLatest();
            }
        });
        LiveDataBus.get().with(EaseConstant.CONTACT_UPDATE, EaseEvent.class).observe(this, event -> {
            if(event == null) {
                return;
            }
            if(event.isContactChange()) {
                chatLayout.getChatMessageListLayout().refreshMessages();
            }
        });
    }


    @Override
    public void onUserAvatarClick(String username) {

    }

    @Override
    public void onUserAvatarLongClick(String username) {

    }

    @Override
    public boolean onBubbleLongClick(View v, EMMessage message) {
        return false;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if(!chatLayout.getChatMessageListLayout().isGroupChat()) {
            return;
        }
        if(count == 1 && "@".equals(String.valueOf(s.charAt(start)))){
            PickAtUserActivity.actionStartForResult(ChatFragment.this, conversationId, REQUEST_CODE_SELECT_AT_USER);
        }
    }

    @Override
    public boolean onBubbleClick(EMMessage message) {
        return false;
    }

    @Override
    public void onChatExtendMenuItemClick(View view, int itemId) {
        super.onChatExtendMenuItemClick(view, itemId);
        //            case R.id.extend_item_video_call:
        //                showSelectDialog();
        //                break;
        if (itemId == R.id.extend_item_call) {
            RxPermissions rxPermissions = new RxPermissions(getActivity());
            rxPermissions.request(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
            ).subscribe(granted -> {
                if(granted){
                    Intent intent = new Intent(getContext(), ConferenceInviteActivity.class).addFlags(FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(EaseConstant.EXTRA_CONFERENCE_GROUP_ID, conversationId);
                    getContext().startActivity(intent);
                }else{
                    Toast.makeText(getActivity(), "请确认开启录音，相机权限", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onChatError(int code, String errorMsg) {
        if(infoListener != null) {
            infoListener.onChatError(code, errorMsg);
        }
    }

    @Override
    public void onOtherTyping(String action) {
        if(infoListener != null) {
            infoListener.onOtherTyping(action);
        }
    }

    @Override
    public void onReadNumClick(EMMessage message) {
        super.onReadNumClick(message);
        EaseDingAckUserListActivity.startAction(getContext(), message);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_SELECT_AT_USER :
                    if(data != null){
                        String username = data.getStringExtra("username");
                        chatLayout.inputAtUsername(username, false);
                    }
                    break;

                case REQUEST_CODE_ORDER:
                    EMOrder order = (EMOrder) data.getParcelableExtra("order");
                    String content = order.getId() + "订单\n订单类型：" + order.getType() + "\n商品名称：" + order.getName() + "\n下单日期：" + order.getDate();
                    chatLayout.sendTextMessage(content);
                    break;
            }
        }
    }

    /**
     * Send user card message
     * @param user
     */
    private void sendUserCardMessage(EaseUser user) {
        EMMessage message = EMMessage.createSendMessage(EMMessage.Type.CUSTOM);
        EMCustomMessageBody body = new EMCustomMessageBody(EaseConstant.USER_CARD_EVENT);
        Map<String,String> params = new HashMap<>();
        params.put(EaseConstant.USER_CARD_ID,user.getUsername());
        params.put(EaseConstant.USER_CARD_NICK,user.getNickname());
        params.put(EaseConstant.USER_CARD_AVATAR,user.getAvatar());
        body.setParams(params);
        message.setBody(body);
        message.setTo(conversationId);
        chatLayout.sendMessage(message);
    }

    @Override
    public void onStop() {
        super.onStop();
        //保存未发送的文本消息内容
        if(mContext != null && mContext.isFinishing()) {
            if(chatLayout.getChatInputMenu() != null) {
                saveUnSendMsg(chatLayout.getInputContent());
                LiveDataBus.get().with(EaseConstant.MESSAGE_NOT_SEND).postValue(true);
            }
        }
    }

    //================================== for video and voice start ====================================

    /**
     * 保存未发送的文本消息内容
     * @param content
     */
    private void saveUnSendMsg(String content) {
        EaseIMHelper.getInstance().getModel().saveUnSendMsg(conversationId, content);
    }

    private String getUnSendMsg() {
        return EaseIMHelper.getInstance().getModel().getUnSendMsg(conversationId);
    }

    @Override
    public void onPreMenu(EasePopupWindowHelper helper, EMMessage message, View v) {
        //默认两分钟后，即不可撤回
        if(System.currentTimeMillis() - message.getMsgTime() > 2 * 60 * 1000) {
            helper.findItemVisible(R.id.action_chat_recall, false);
        }
        EMMessage.Type type = message.getType();
        helper.findItemVisible(R.id.action_chat_forward, false);
        switch (type) {
            case TXT:
                if(!message.getBooleanAttribute(EaseConstant.MESSAGE_ATTR_IS_VIDEO_CALL, false)
                        && !message.getBooleanAttribute(EaseConstant.MESSAGE_ATTR_IS_VOICE_CALL, false)) {
                    helper.findItemVisible(R.id.action_chat_forward, true);
                }
                break;
            case IMAGE:
                helper.findItemVisible(R.id.action_chat_forward, true);
                break;
        }

        if(chatType == EaseConstant.CHATTYPE_CHATROOM) {
            helper.findItemVisible(R.id.action_chat_forward, true);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItemBean item, EMMessage message) {
        if (item.getItemId() == R.id.action_chat_delete) {
            showDeleteDialog(message);
            return true;
        } else if (item.getItemId() == R.id.action_chat_recall) {
            showProgressBar();
            chatLayout.recallMessage(message);
            return true;
        } else if (item.getItemId() == R.id.action_chat_roam) {
            chatLayout.getChatMessageListLayout().loadBeforeMsgFromServer(message.getMsgId());
            return true;
        }
        return false;
    }

    private void showProgressBar() {
        View view = View.inflate(mContext, R.layout.em_layout_progress_recall, null);
        dialog = new Dialog(mContext,R.style.dialog_recall);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setContentView(view, layoutParams);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private void showDeleteDialog(EMMessage message) {
        chatLayout.deleteMessage(message);
//        new SimpleDialogFragment.Builder((BaseActivity) mContext)
//                .setTitle(getString(R.string.em_chat_delete_title))
//                .setConfirmColor(R.color.red)
//                .setOnConfirmClickListener(getString(R.string.delete), new DemoDialogFragment.OnConfirmClickListener() {
//                    @Override
//                    public void onConfirmClick(View view) {
//                        chatLayout.deleteMessage(message);
//                    }
//                })
//                .showCancelButton(true)
//                .show();
    }

    public void setOnFragmentInfoListener(OnFragmentInfoListener listener) {
        this.infoListener = listener;
    }

    @Override
    public void recallSuccess(EMMessage message) {
        if(dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    public void recallFail(int code, String errorMsg) {
        if(dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public interface OnFragmentInfoListener {
        void onChatError(int code, String errorMsg);

        void onOtherTyping(String action);
    }

    @Override
    public void translateMessageFail(EMMessage message, int code, String error) {
        new AlertDialog.Builder(getContext())
                .setTitle(mContext.getString(R.string.unable_translate))
                .setMessage(error+".")
                .setPositiveButton(mContext.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }
}