package com.hyphenate.easeim.section.conference.adapter;


import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeui.EaseIM;
import com.hyphenate.easeui.adapter.EaseBaseRecyclerViewAdapter;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.provider.EaseUserProfileProvider;
import com.hyphenate.easeui.widget.EaseImageView;

public class ConferenceInviteAdapter extends EaseBaseRecyclerViewAdapter<EaseUser> {

    private OnItemCheckedListener listener;

    @Override
    public int getEmptyLayoutId() {
        return EaseIMHelper.getInstance().isAdmin() ? R.layout.ease_layout_no_data_admin : R.layout.ease_layout_no_data;
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent, int viewType) {
        return new InviteViewHolder(LayoutInflater.from(mContext).inflate(R.layout.em_widget_contact_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

    }

    private class InviteViewHolder extends ViewHolder<EaseUser> {
        private EaseImageView mAvatar;
        private TextView mName;
        private CheckBox checkBox;

        public InviteViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        public void initView(View itemView) {
            mAvatar = findViewById(R.id.avatar);
            mName = findViewById(R.id.name);
            checkBox = findViewById(R.id.checkbox);
        }

        @Override
        public void setData(EaseUser item, int position) {
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(item.isChecked());

            if(item.isNotUse()){
                checkBox.setChecked(true);
                checkBox.setEnabled(false);
            } else {
                checkBox.setEnabled(true);
            }

            EaseUserProfileProvider provider = EaseIM.getInstance().getUserProvider();
            if(provider != null){
                EaseUser easeUser = provider.getUser(item.getUsername());
                if(easeUser != null){
                    item.setNickname(easeUser.getNickname());
                    item.setAvatar(easeUser.getAvatar());
                }
            }
            String avatarUrl = item.getAvatar();
            String nickname = item.getNickname();
            if(!TextUtils.isEmpty(avatarUrl)){
                Glide.with(mContext).load(avatarUrl).apply(RequestOptions.bitmapTransform(new CircleCrop())).error(R.drawable.em_default_avatar).into(mAvatar);
            }
            mName.setText(nickname);

            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    item.setChecked(isChecked);
                    if(listener != null){
                        listener.onCheckedChanged(item);
                    }
                }
            });
        }
    }


    @Override
    public boolean filterToCompare(String filter, EaseUser data) {
        if(data.getNickname().contains(filter)){
            return true;
        }
        return false;
    }

    public void setOnItemCheckedListener(OnItemCheckedListener listener){
        this.listener = listener;
    }

    public interface OnItemCheckedListener{
        void onCheckedChanged(EaseUser user);
    }
}
