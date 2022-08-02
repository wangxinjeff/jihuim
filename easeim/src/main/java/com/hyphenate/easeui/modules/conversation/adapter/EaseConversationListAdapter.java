package com.hyphenate.easeui.modules.conversation.adapter;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeui.EaseIM;
import com.hyphenate.easeui.adapter.EaseBaseDelegateAdapter;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.modules.conversation.model.EaseConversationInfo;
import com.hyphenate.easeui.provider.EaseUserProfileProvider;

public class EaseConversationListAdapter extends EaseBaseDelegateAdapter<EaseConversationInfo> {
    private int emptyLayoutId;

    @Override
    public int getEmptyLayoutId() {
        return emptyLayoutId != 0 ? emptyLayoutId : R.layout.ease_layout_default_no_conversation_data;
    }

    /**
     * set empty layout
     * @param layoutId
     */
    public void setEmptyLayoutId(int layoutId) {
        this.emptyLayoutId = layoutId;
        notifyDataSetChanged();
    }

    @Override
    public boolean filterToCompare(String filter, EaseConversationInfo data) {
        EMConversation conversation = (EMConversation)data.getInfo();
        if(conversation.getType() == EMConversation.EMConversationType.Chat){
            EaseUserProfileProvider userProvider = EaseIM.getInstance().getUserProvider();
            if(userProvider != null) {
                EaseUser user = userProvider.getUser(conversation.conversationId());
                if(user != null) {
                    if(user.getNickname().contains(filter)){
                        return true;
                    }
                }
            }
        } else if(conversation.getType() == EMConversation.EMConversationType.GroupChat){
            EMGroup group = EaseIMHelper.getInstance().getGroupManager().getGroup(conversation.conversationId());
            if(group != null){
                if(group.getGroupName().contains(filter)){
                    return true;
                }
            }
        }

        if(conversation.conversationId().contains(filter)){
            return true;
        }

        return false;
    }
}

