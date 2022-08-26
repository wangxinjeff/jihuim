package com.hyphenate.easeim.section.group.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.model.GroupApplyBean;
import com.hyphenate.easeui.adapter.EaseBaseRecyclerViewAdapter;


public class GroupApplyAdapter extends EaseBaseRecyclerViewAdapter<GroupApplyBean> {

    private OnGroupApplyListener listener;

    @Override
    public ViewHolder getViewHolder(ViewGroup parent, int viewType) {
        return new GroupApplyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.em_group_apply_item, parent, false));
    }

    public void setOnGroupApplyListener(OnGroupApplyListener listener){
        this.listener = listener;
    }

    class GroupApplyViewHolder extends ViewHolder<GroupApplyBean>{
        AppCompatTextView customerName;
        AppCompatTextView groupName;
        AppCompatTextView inviterName;
        AppCompatButton btnRefused;
        AppCompatButton btnAgree;
        AppCompatButton btnOperated;

        public GroupApplyViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        public void initView(View itemView) {
            customerName = itemView.findViewById(R.id.customer_name);
            groupName = itemView.findViewById(R.id.group_name);
            inviterName = itemView.findViewById(R.id.inviter_name);
            btnRefused = itemView.findViewById(R.id.btn_refused);
            btnAgree = itemView.findViewById(R.id.btn_agree);
            btnOperated = itemView.findViewById(R.id.btn_operated);
        }

        @Override
        public void setData(GroupApplyBean item, int position) {
            if(TextUtils.isEmpty(item.getUserNickName()) || TextUtils.equals("null", item.getUserNickName())){
                customerName.setText(item.getUserName());
            } else {
                customerName.setText(item.getUserNickName());
            }

            groupName.setText(item.getGroupName());

            if(TextUtils.isEmpty(item.getInviterNickName()) || TextUtils.equals("null", item.getInviterNickName())){
                inviterName.setText(item.getInviterName());
            } else {
                inviterName.setText(item.getInviterNickName());
            }

            if(item.isOperated()){
                btnRefused.setVisibility(View.GONE);
                btnAgree.setVisibility(View.GONE);
                if(TextUtils.equals(item.getOperatedResult(), "success")){
                    btnOperated.setText(R.string.Has_agreed_to);
                } else {
                    btnOperated.setText(R.string.Has_refused_to);
                }
                btnOperated.setVisibility(View.VISIBLE);
            } else {
                btnRefused.setVisibility(View.VISIBLE);
                btnAgree.setVisibility(View.VISIBLE);
                btnOperated.setVisibility(View.GONE);
            }

            btnRefused.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener != null){
                        listener.onRefused(item);
                    }
                }
            });
            btnAgree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener != null){
                        listener.onAgreed(item);
                    }
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null){
                        listener.onItemClick(item);
                    }
                }
            });
        }
    }

    public interface OnGroupApplyListener{
        void onRefused(GroupApplyBean bean);
        void onAgreed(GroupApplyBean bean);
        void onItemClick(GroupApplyBean bean);
    }
}
