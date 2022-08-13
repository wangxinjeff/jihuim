package com.hyphenate.easeim.section.chat.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
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
    private ViewPager2 viewPager;
    private List<Fragment> fragments;
    private OrderListFragment mainFragment;
    private OrderListFragment pickCarFragment;
    private OrderListFragment fineFragment;
    private OrderListFragment packageFragment;

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
        pickCarFragment = new OrderListFragment("PICKCAR");
        fineFragment = new OrderListFragment("FINE");
        packageFragment = new OrderListFragment("PACKAGE");
        fragments.add(mainFragment);
        fragments.add(pickCarFragment);
        fragments.add(fineFragment);
        fragments.add(packageFragment);

        List<String> titleList = new ArrayList<>();
        titleList.add("维保订单");
        titleList.add("取送订单");
        titleList.add("精品订单");
        titleList.add("服务订单");

        viewPager.setAdapter(new SectionPagerAdapter(this, fragments));
        new TabLayoutMediator(tabLayout, viewPager, new TabLayoutMediator.TabConfigurationStrategy(){
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(titleList.get(position));
            }
        }).attach();

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
