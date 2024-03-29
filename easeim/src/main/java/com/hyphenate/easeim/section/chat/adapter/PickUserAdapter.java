package com.hyphenate.easeim.section.chat.adapter;


import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeui.adapter.EaseBaseRecyclerViewAdapter;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.easeui.widget.EaseImageView;

import androidx.annotation.NonNull;

public class PickUserAdapter extends EaseBaseRecyclerViewAdapter<EaseUser> {

    @Override
    public int getEmptyLayoutId() {
        return EaseIMHelper.getInstance().isAdmin() ? R.layout.ease_layout_no_data_admin : R.layout.ease_layout_no_data;
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent, int viewType) {
        return new PickUserViewHolder(LayoutInflater.from(mContext).inflate(R.layout.em_widget_contact_item, parent, false));
    }

    private class PickUserViewHolder extends ViewHolder<EaseUser> {
        private TextView mHeader;
        private EaseImageView mAvatar;
        private TextView mName;
        private TextView mSignature;
        private TextView mUnreadMsgNumber;

        public PickUserViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        public void initView(View itemView) {
            mHeader = findViewById(R.id.header);
            mAvatar = findViewById(R.id.avatar);
            mName = findViewById(R.id.name);
            mSignature = findViewById(R.id.signature);
            mUnreadMsgNumber = findViewById(R.id.unread_msg_number);
        }

        @Override
        public void setData(EaseUser item, int position) {
            String header = EaseCommonUtils.getLetter(item.getNickname());
            Log.e("TAG", "GroupContactAdapter header = "+header);
            mHeader.setVisibility(View.GONE);
            // 是否显示字母
//            if(position == 0 || (header != null && !header.equals(EaseCommonUtils.getLetter(getItem(position - 1).getNickname())))) {
//                if(!TextUtils.isEmpty(header)) {
//                    mHeader.setVisibility(View.VISIBLE);
//                    mHeader.setText(header);
//                }
//            }
//            EaseUserProfileProvider provider = EaseIM.getInstance().getUserProvider();
//            if (provider != null){
//                EaseUser easeUser = provider.getUser(item.getUsername());
//                if(easeUser != null){
//                    item.setAvatar(easeUser.getAvatar());
//                    item.setNickname(easeUser.getNickname());
//                }
//            }
//            mName.setText(item.getNickname());
            EaseUserUtils.setUserAvatar(mContext, item.getUsername(), mAvatar);
            EaseUserUtils.setUserNick(item.getUsername(), mName);
        }
    }


    @Override
    public boolean filterToCompare(String filter, EaseUser data) {
        if(data.getNickname().contains(filter)){
            return true;
        }
        return false;
    }
}
