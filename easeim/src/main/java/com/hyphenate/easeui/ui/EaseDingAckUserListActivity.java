package com.hyphenate.easeui.ui;

import android.arch.lifecycle.ViewModelProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.OnResourceParseCallback;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeim.common.repositories.EMGroupManagerRepository;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeim.section.search.adapter.SectionPagerAdapter;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.manager.EaseDingMessageHelper;
import com.hyphenate.easeui.widget.EaseTitleBar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangsong on 18-1-23.
 */

public class EaseDingAckUserListActivity extends BaseInitActivity {
    private EaseTitleBar titleBar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private List<Fragment> fragmentList;
    private EMMessage message;
    private List<String> memberList = new ArrayList<>();
    private List<String> readList = new ArrayList<>();
    private List<String> unReadList = new ArrayList<>();
    private GroupReadAckListFragment readFragment;
    private GroupReadAckListFragment unReadFragment;
    public static void startAction(Context context, EMMessage message){
        Intent intent = new Intent(context, EaseDingAckUserListActivity.class);
        intent.putExtra("message", message);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.ease_activity_ding_ack_user_list;
    }

    @Override
    protected void initIntent(Intent intent) {
        super.initIntent(intent);
        message = intent.getParcelableExtra("message");
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        titleBar = findViewById(R.id.title_bar);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        EaseDingMessageHelper.get().setUserUpdateListener(message, userUpdateListener);
    }

    @Override
    protected void initData() {
        super.initData();
        EaseDingMessageHelper.get().fetchGroupReadAck(message);
        showLoading();
    }

    private void refreshView(){
        fragmentList = new ArrayList<>();
        readFragment = new GroupReadAckListFragment(readList);
        unReadFragment = new GroupReadAckListFragment(unReadList);
        fragmentList.add(readFragment);
        fragmentList.add(unReadFragment);
        List<String> titleList = new ArrayList<>();
        titleList.add(readList.size() + "人已读");
        titleList.add(unReadList.size() + "人未读");

        viewPager.setAdapter(new SectionPagerAdapter(getSupportFragmentManager(), fragmentList, titleList));

        tabLayout.setupWithViewPager(viewPager);
        dismissLoading();
    }

    @Override
    protected void initListener() {
        super.initListener();
        titleBar.setOnBackPressListener(new EaseTitleBar.OnBackPressListener() {
            @Override
            public void onBackPress(View view) {
                onBackPressed();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        EaseDingMessageHelper.get().setUserUpdateListener(message, null);
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    private EaseDingMessageHelper.IAckUserUpdateListener userUpdateListener =
            new EaseDingMessageHelper.IAckUserUpdateListener() {
                @Override
                public void onUpdate(List<String> list) {
                    readList = list;
                    runOnUiThread(() -> {
                        EMGroupManagerRepository.getInstance().getGroupAllMembers(message.getTo(), new ResultCallBack<List<EaseUser>>() {
                            @Override
                            public void onSuccess(List<EaseUser> data) {
                                memberList.clear();
                                unReadList.clear();
                                for(EaseUser user : data){
                                    if(!TextUtils.equals(user.getUsername(), EaseIMHelper.getInstance().getCurrentUser())){
                                        memberList.add(user.getUsername());
                                    }
                                }
                                for(String s : memberList){
                                    if(!readList.contains(s)){
                                        unReadList.add(s);
                                    }
                                }
                                runOnUiThread(() -> refreshView());
                            }

                            @Override
                            public void onError(int i, String s) {

                            }
                        });
                    });
                }
            };

}
