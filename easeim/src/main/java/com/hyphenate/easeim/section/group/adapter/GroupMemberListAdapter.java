package com.hyphenate.easeim.section.group.adapter;


import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeui.adapter.EaseBaseDelegateAdapter;
import com.hyphenate.easeui.domain.EaseUser;

public class GroupMemberListAdapter extends EaseBaseDelegateAdapter<EaseUser> {

    @Override
    public int getEmptyLayoutId() {
        return EaseIMHelper.getInstance().isAdmin() ? R.layout.ease_layout_no_data_admin : R.layout.ease_layout_no_data;
    }


    @Override
    public boolean filterToCompare(String filter, EaseUser data) {
        if(data.getNickname().contains(filter)){
            return true;
        }
        return false;
    }
}

