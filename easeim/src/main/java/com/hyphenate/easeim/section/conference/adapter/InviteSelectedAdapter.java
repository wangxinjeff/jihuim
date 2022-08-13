package com.hyphenate.easeim.section.conference.adapter;


import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hyphenate.easeim.R;
import com.hyphenate.easeui.adapter.EaseBaseRecyclerViewAdapter;
import com.hyphenate.easeui.utils.EaseUserUtils;

public class InviteSelectedAdapter extends EaseBaseRecyclerViewAdapter<String> {

    @Override
    public ViewHolder getViewHolder(ViewGroup parent, int viewType) {
        return new InviteViewHolder(LayoutInflater.from(mContext).inflate(R.layout.invite_selected_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

    }

    private class InviteViewHolder extends ViewHolder<String> {
        private AppCompatImageView mAvatar;
        private AppCompatTextView mName;

        public InviteViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        public void initView(View itemView) {
            mAvatar = findViewById(R.id.item_avatar);
            mName = findViewById(R.id.item_name);
        }

        @Override
        public void setData(String item, int position) {
//            EaseUserProfileProvider provider = EaseIM.getInstance().getUserProvider();
//            if(provider != null){
//                EaseUser user = provider.getUser(item);
//                if(user != null){
//                    Glide.with(mContext).load(user.getAvatar()).apply(RequestOptions.bitmapTransform(new CircleCrop())).error(R.drawable.ease_default_avatar).into(mAvatar);
//                    mName.setText(user.getNickname());
//                }
//            } else {
//                mName.setText(item);
//            }
            EaseUserUtils.setUserAvatar(mContext, item, mAvatar);
            EaseUserUtils.setUserNick(item, mName);
        }
    }
}
