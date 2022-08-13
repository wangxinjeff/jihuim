package com.hyphenate.easeim.section.chat.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.View;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeim.section.chat.fragment.OrderListFragment;
import com.hyphenate.easeim.section.search.adapter.SectionPagerAdapter;
import com.hyphenate.easeui.widget.EaseTitleBar;

import java.util.ArrayList;
import java.util.List;

public class OrderListActivity extends BaseInitActivity {

    private EaseTitleBar titleBar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private List<Fragment> fragments;
    private OrderListFragment mainFragment;
    private OrderListFragment fineFragment;

    public static void actionStart(Fragment context, int requestCode) {
        Intent intent = new Intent(context.getContext(), OrderListActivity.class);
        context.startActivityForResult(intent, requestCode);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_order_list;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        titleBar = findViewById(R.id.title_bar);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        fragments = new ArrayList<>();
        mainFragment = new OrderListFragment("MAIN");
        fineFragment = new OrderListFragment("FINE");
        fragments.add(mainFragment);
        fragments.add(fineFragment);

        List<String> titleList = new ArrayList<>();
        titleList.add("维保订单");
        titleList.add("服务订单");

        viewPager.setAdapter(new SectionPagerAdapter(getSupportFragmentManager(), fragments, titleList));

        tabLayout.setupWithViewPager(viewPager);

    }

    @Override
    protected void initData() {
        super.initData();
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
}
