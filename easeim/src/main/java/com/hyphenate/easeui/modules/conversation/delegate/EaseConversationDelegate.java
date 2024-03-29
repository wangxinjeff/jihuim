package com.hyphenate.easeui.modules.conversation.delegate;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;

import com.bumptech.glide.Glide;
import com.hyphenate.chat.EMChatRoom;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeui.EaseIM;
import com.hyphenate.easeim.R;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.manager.EaseAtMessageHelper;
import com.hyphenate.easeui.manager.EasePreferenceManager;
import com.hyphenate.easeui.modules.conversation.model.EaseConversationInfo;
import com.hyphenate.easeui.modules.conversation.model.EaseConversationSetStyle;
import com.hyphenate.easeui.provider.EaseConversationInfoProvider;
import com.hyphenate.easeui.provider.EaseUserProfileProvider;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.easeui.utils.EaseDateUtils;
import com.hyphenate.easeui.utils.EaseSmileUtils;

import java.util.Date;
import java.util.List;

public class EaseConversationDelegate extends EaseDefaultConversationDelegate {

    public EaseConversationDelegate(EaseConversationSetStyle setModel) {
        super(setModel);
    }

    @Override
    public boolean isForViewType(EaseConversationInfo item, int position) {
        return item != null && item.getInfo() instanceof EMConversation;
    }

    @Override
    protected void onBindConViewHolder(ViewHolder holder, int position, EaseConversationInfo bean) {

        if(EaseIMHelper.getInstance().isAdmin()){
            holder.mUnreadMsgNumber.setBackgroundResource(R.drawable.em_unread_bg_admin);
        } else {
            holder.mUnreadMsgNumber.setBackgroundResource(R.drawable.em_unread_bg);
        }
        
        EMConversation item = (EMConversation) bean.getInfo();
        Context context = holder.itemView.getContext();
        String username = item.conversationId();
        holder.mentioned.setVisibility(View.GONE);
        holder.groupId.setVisibility(View.GONE);
        int defaultAvatar = 0;
        String showName = null;
        if(!setModel.isHideUnreadDot()) {
            showUnreadNum(holder, item.getUnreadMsgCount());
        }

        if(item.getType() == EMConversation.EMConversationType.GroupChat) {
            if(EaseIMHelper.getInstance().isAdmin()){
                holder.groupId.setVisibility(View.VISIBLE);
                holder.groupId.setText("群组ID："+username);
            }
            if(EaseAtMessageHelper.get().hasAtMeMsg(username)) {
                holder.mentioned.setText(R.string.were_mentioned);
                holder.mentioned.setVisibility(View.VISIBLE);
            }
            defaultAvatar = R.drawable.em_group_icon;
            EMGroup group = EMClient.getInstance().groupManager().getGroup(username);
            showName = group != null ? group.getGroupName() : username;
            List<String> noPushGroups = EMClient.getInstance().pushManager().getNoPushGroups();
            if(noPushGroups != null && noPushGroups.contains(username)){
                holder.noPush.setVisibility(View.VISIBLE);
                if(!setModel.isHideUnreadDot()) {
                    showUnread(holder, item.getUnreadMsgCount());
                }
            } else {
                holder.noPush.setVisibility(View.GONE);
            }
        }else if(item.getType() == EMConversation.EMConversationType.ChatRoom) {
            defaultAvatar = R.drawable.em_chat_room_icon;
            EMChatRoom chatRoom = EMClient.getInstance().chatroomManager().getChatRoom(username);
            showName = chatRoom != null && !TextUtils.isEmpty(chatRoom.getName()) ? chatRoom.getName() : username;
        }else {
            defaultAvatar = R.drawable.em_default_avatar;
            showName = username;
            List<String> noPushUsers = EMClient.getInstance().pushManager().getNoPushUsers();
            if(noPushUsers != null && noPushUsers.contains(username)){
                holder.noPush.setVisibility(View.VISIBLE);
                if(!setModel.isHideUnreadDot()) {
                    showUnread(holder, item.getUnreadMsgCount());
                }
            } else {
                holder.noPush.setVisibility(View.GONE);
            }
        }
        holder.avatar.setImageResource(defaultAvatar);
        holder.name.setText(showName);
        EaseConversationInfoProvider infoProvider = EaseIM.getInstance().getConversationInfoProvider();
        if(infoProvider != null) {
            Drawable avatarResource = infoProvider.getDefaultTypeAvatar(item.getType().name());
            if(avatarResource != null) {
                Glide.with(holder.mContext).load(avatarResource).error(defaultAvatar).into(holder.avatar);
            }
        }
        // add judgement for conversation type
        if(item.getType() == EMConversation.EMConversationType.Chat) {
            EaseUserProfileProvider userProvider = EaseIM.getInstance().getUserProvider();
            if(userProvider != null) {
                EaseUser user = userProvider.getUser(username);
                if(user != null) {
                    if(!TextUtils.isEmpty(user.getNickname())) {
                        holder.name.setText(user.getNickname());
                    }
                    if(!TextUtils.isEmpty(user.getAvatar())) {
                        Drawable drawable = holder.avatar.getDrawable();
                        Glide.with(holder.mContext)
                                .load(user.getAvatar())
                                .error(drawable)
                                .into(holder.avatar);
                    }
                }
            }
        }

        if(item.getAllMessages().size() > 0) {
            EMMessage lastMessage = item.getLastMessage();
            if(lastMessage.getChatType() == EMMessage.ChatType.Chat){
                holder.message.setText(EaseSmileUtils.getSmiledText(context, EaseCommonUtils.getMessageDigest(lastMessage, context, false)));

            } else if (lastMessage.getChatType() == EMMessage.ChatType.GroupChat){
                holder.message.setText(EaseSmileUtils.getSmiledText(context, EaseCommonUtils.getMessageDigest(lastMessage, context, true)));

            }
            holder.time.setText(EaseDateUtils.getTimestampString(context, new Date(lastMessage.getMsgTime())));
            if (lastMessage.direct() == EMMessage.Direct.SEND && lastMessage.status() == EMMessage.Status.FAIL) {
                holder.mMsgState.setVisibility(View.VISIBLE);
            } else {
                holder.mMsgState.setVisibility(View.GONE);
            }
        } else {
            holder.message.setText("");
            holder.time.setText("");
            holder.mMsgState.setVisibility(View.GONE);
        }

        if(holder.mentioned.getVisibility() != View.VISIBLE) {
            String unSendMsg = EasePreferenceManager.getInstance().getUnSendMsgInfo(username);
            if(!TextUtils.isEmpty(unSendMsg)) {
                holder.mentioned.setText(R.string.were_not_send_msg);
                holder.message.setText(unSendMsg);
                holder.mentioned.setVisibility(View.VISIBLE);
            }
        }
    }
}

