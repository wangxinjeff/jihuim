package com.hyphenate.easeim.section.chat.views;

import android.content.Context;
import android.text.TextUtils;
import android.widget.TextView;

import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.easeui.widget.chatrow.EaseChatRow;
import com.hyphenate.exceptions.HyphenateException;

import org.json.JSONObject;

public class ChatRowNotice extends EaseChatRow {
    private TextView name;
    private TextView content;

    public ChatRowNotice(Context context, boolean isSender) {
        super(context, isSender);
    }

    @Override
    protected void onInflateView() {
        inflater.inflate(R.layout.demo_row_recall_message, this);
    }

    @Override
    protected void onFindViewById() {
        name = (TextView) findViewById(R.id.user);
        content = findViewById(R.id.content);
    }

    @Override
    protected void onSetUpView() {
        if(message.getBooleanAttribute(EaseConstant.MESSAGE_TYPE_RECALL, false)){
            String user = message.getFrom();
            if(TextUtils.equals(user, EaseIMHelper.getInstance().getCurrentUser())){
                name.setVisibility(GONE);
                content.setText(R.string.msg_recall_by_self);
            } else {
                name.setVisibility(VISIBLE);
                EaseUserUtils.setUserNick(user, name);
                content.setText(R.string.msg_recall_by_user);
            }
        } else if(!message.getStringAttribute(EaseConstant.MESSAGE_ATTR_CALL_STATE, "").equals("")){
            String createCall = message.getStringAttribute(EaseConstant.MESSAGE_ATTR_CALL_STATE, "");
            String user = message.getStringAttribute(EaseConstant.MESSAGE_ATTR_CALL_USER, "");
            if(TextUtils.equals(createCall, EaseConstant.CONFERENCE_STATE_CREATE)){
                name.setVisibility(VISIBLE);
                content.setText(context.getString(R.string.em_initiated_call));
                try {
                    JSONObject userInfo = message.getJSONObjectAttribute(EaseConstant.MESSAGE_ATTR_USER_INFO);
                    if(userInfo != null){
                        String nick = userInfo.optString(EaseConstant.MESSAGE_ATTR_USER_NICK);
                        if(!TextUtils.isEmpty(nick)){
                            name.setText(nick);
                            return;
                        }
                    }
                    EaseUserUtils.setUserNick(user, name);
                } catch (HyphenateException e) {
                    e.printStackTrace();
                }
            } else if(TextUtils.equals(createCall, EaseConstant.CONFERENCE_STATE_END)){
                name.setVisibility(GONE);
                content.setText(context.getString(R.string.em_call_over));
            }
        } else if(message.getBooleanAttribute(EaseConstant.CREATE_GROUP_PROMPT, false)){
            name.setVisibility(GONE);
            String groupName = message.getStringAttribute(EaseConstant.CREATE_GROUP_NAME, "");
            content.setText(String.format(context.getString(R.string.em_group_create_success), groupName));
        }
    }
}
