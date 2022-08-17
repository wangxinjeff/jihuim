package com.hyphenate.easeim.section.search;

import android.arch.lifecycle.ViewModelProvider;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.PopupWindow;
import android.widget.TextView;

import android.support.annotation.Nullable;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.OnResourceParseCallback;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeim.common.model.SearchResult;
import com.hyphenate.easeim.common.repositories.EMGroupManagerRepository;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeim.section.chat.activity.ChatActivity;
import com.hyphenate.easeim.section.search.adapter.SearchGroupChatAdapter;
import com.hyphenate.easeui.constants.EaseConstant;

import java.util.ArrayList;
import java.util.List;

public class SearchGroupChatActivity extends BaseInitActivity implements View.OnClickListener{

    private AppCompatImageView backIcon;
    private AppCompatTextView searchName;
    private AppCompatEditText searchContent;
    private AppCompatTextView searchStart;

    private View popView;
    private AppCompatTextView item1;
    private AppCompatTextView item2;
    private AppCompatTextView item3;
    private AppCompatTextView item4;
    private PopupWindow popupWindow;

    private RecyclerView recyclerView;
    private SearchGroupChatAdapter adapter;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_search_group_chat;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        backIcon = findViewById(R.id.icon_back);
        searchName = findViewById(R.id.search_name);
        searchContent = findViewById(R.id.search_content);
        searchStart = findViewById(R.id.search_start);

        popView = LayoutInflater.from(this).inflate(R.layout.pop_search_group_chat, null, false);
        item1 = popView.findViewById(R.id.item1);
        item2 = popView.findViewById(R.id.item2);
        item3 = popView.findViewById(R.id.item3);
        item4 = popView.findViewById(R.id.item4);
        popupWindow = new PopupWindow(popView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setTouchable(true);
        // 如果不设置PopupWindow的背景，有些版本就会出现一个问题：无论是点击外部区域还是Back键都无法dismiss弹框
        // 这里单独写一篇文章来分析
        popupWindow.setBackgroundDrawable(new ColorDrawable());

        recyclerView = findViewById(R.id.result_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchGroupChatAdapter();
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void initData() {
        super.initData();
    }

    @Override
    protected void initListener() {
        super.initListener();
        backIcon.setOnClickListener(this);
        searchName.setOnClickListener(this);
        searchStart.setOnClickListener(this);
        searchContent.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        event != null && KeyEvent.KEYCODE_ENTER == event.getKeyCode() &&
                                KeyEvent.ACTION_DOWN == event.getAction()){
                    searchGroupChat(searchName.getText().toString(), searchContent.getText().toString());
                    return true;
                } else {
                    return false;
                }
            }
        });

        item1.setOnClickListener(this);
        item2.setOnClickListener(this);
        item3.setOnClickListener(this);
        item4.setOnClickListener(this);

        adapter.setItemClickListener(new SearchGroupChatAdapter.onItemClickListener() {
            @Override
            public void onClick(String groupId) {
                ChatActivity.actionStart(SearchGroupChatActivity.this, groupId, EaseConstant.CHATTYPE_GROUP);
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.icon_back) {
            onBackPressed();
        } else if (id == R.id.search_name) {
            popupWindow.showAsDropDown(searchName);
        } else if (id == R.id.search_start) {
            searchGroupChat(searchName.getText().toString(), searchContent.getText().toString());
        } else if (id == R.id.item1) {
            searchName.setText(item1.getText().toString());
            popupWindow.dismiss();
        } else if (id == R.id.item2) {
            searchName.setText(item2.getText().toString());
            popupWindow.dismiss();
        } else if (id == R.id.item3) {
            searchName.setText(item3.getText().toString());
            popupWindow.dismiss();
        } else if (id == R.id.item4) {
            searchName.setText(item4.getText().toString());
            popupWindow.dismiss();
        }
    }

    private void searchGroupChat(String name, String content){
        if(TextUtils.equals(name, getString(R.string.group_name))){
            searchGroupChat("", "", "", "", content, "MANAGE");
        } else if(TextUtils.equals(name, getString(R.string.em_order_id))){
            searchGroupChat("", "", content, "", "", "MANAGE");
        } else if(TextUtils.equals(name, getString(R.string.em_phone_number))){
            searchGroupChat("", content, "", "", "", "MANAGE");
        } else if(TextUtils.equals(name, getString(R.string.em_win_code))){
            searchGroupChat("", "", "", content, "", "MANAGE");
        }
    }

    private void searchGroupChat(String aid, String mobile, String orderId, String vin, String groupName, String source){
        showLoading();
        EMGroupManagerRepository.getInstance().searchGroupChat(aid, mobile, orderId, vin, groupName, source, new ResultCallBack<List<SearchResult>>() {
            @Override
            public void onSuccess(List<SearchResult> data) {
                dismissLoading();
                runOnUiThread(() -> {
                    adapter.setData(data);
                });
            }

            @Override
            public void onError(int i, String s) {
                dismissLoading();
                runOnUiThread(() -> {
                    List<SearchResult> data = new ArrayList<>();
                    adapter.setData(data);
                });
            }
        });
    }
}
