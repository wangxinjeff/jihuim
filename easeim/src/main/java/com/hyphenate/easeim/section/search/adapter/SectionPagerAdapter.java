package com.hyphenate.easeim.section.search.adapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class SectionPagerAdapter extends FragmentPagerAdapter {
    private List<Fragment> fragments;
    private List<String> titleList;

    public SectionPagerAdapter(@NonNull FragmentManager manager, List<Fragment> fragments, List<String> titleList) {
        super(manager);
        this.fragments = fragments;
        this.titleList = titleList;
    }

    @Override
    public Fragment getItem(int i) {
        return fragments.get(i);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return titleList.get(position);
    }
}
