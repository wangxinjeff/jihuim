package com.hyphenate.easeim.section.search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.View;

import android.support.v4.app.Fragment;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeim.section.search.adapter.SectionPagerAdapter;
import com.hyphenate.easeim.section.search.fragment.SearchAllFragment;
import com.hyphenate.easeim.section.search.fragment.SearchFileFragment;
import com.hyphenate.easeim.section.search.fragment.SearchMultiMediaFragment;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.widget.EaseTitleBar;

import java.util.ArrayList;
import java.util.List;

public class SearchHistoryChatActivity extends BaseInitActivity {
    private String toUsername;
    private EaseTitleBar titleBar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private List<Fragment> fragmentList;
    private SearchAllFragment  searchAllFragment;
    private SearchFileFragment searchFileFragment;
    private SearchMultiMediaFragment searchMultiMediaFragment;
    private EMConversation conversation;
    private int chatType;

    public static void actionStart(Context context, String toUsername, int chatType) {
        Intent intent = new Intent(context, SearchHistoryChatActivity.class);
        intent.putExtra(EaseConstant.EXTRA_CONVERSATION_ID, toUsername);
        intent.putExtra(EaseConstant.EXTRA_CHAT_TYPE, chatType);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.em_activity_search_history;
    }

    @Override
    protected void initIntent(Intent intent) {
        super.initIntent(intent);
        toUsername = intent.getStringExtra(EaseConstant.EXTRA_CONVERSATION_ID);
        chatType = intent.getIntExtra(EaseConstant.EXTRA_CHAT_TYPE, EaseConstant.CHATTYPE_SINGLE);
        conversation = EMClient.getInstance().chatManager().getConversation(toUsername);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        titleBar = findViewById(R.id.title_bar);
        titleBar.setLeftImageResource(R.drawable.em_icon_back_admin);

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        fragmentList = new ArrayList<>();
        Bundle bundle = new Bundle();
        bundle.putString("conversationId", toUsername);
        bundle.putInt("chatType", chatType);
        searchAllFragment = new SearchAllFragment();
        searchAllFragment.setArguments(bundle);
        searchFileFragment = new SearchFileFragment();
        searchFileFragment.setArguments(bundle);
        searchMultiMediaFragment = new SearchMultiMediaFragment();
        searchMultiMediaFragment.setArguments(bundle);

        fragmentList.add(searchAllFragment);
        if(conversation.getType() == EMConversation.EMConversationType.GroupChat){
            fragmentList.add(searchFileFragment);
        }
        fragmentList.add(searchMultiMediaFragment);

        List<String> titleList = new ArrayList<>();
        titleList.add(getString(R.string.em_search_message));
        if(conversation.getType() == EMConversation.EMConversationType.GroupChat) {
            titleList.add(getString(R.string.em_search_file));
        }
        titleList.add(getString(R.string.em_image_and_video));

        viewPager.setAdapter(new SectionPagerAdapter(getSupportFragmentManager(), fragmentList, titleList));
        tabLayout.setupWithViewPager(viewPager);

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
    protected void initData() {
        super.initData();
    }
}
