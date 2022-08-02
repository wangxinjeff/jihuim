package com.hyphenate.easeim.section.chat.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.OnResourceParseCallback;
import com.hyphenate.easeim.common.model.EMOrder;
import com.hyphenate.easeim.section.base.BaseInitFragment;
import com.hyphenate.easeim.section.chat.adapter.OrderListAdapter;
import com.hyphenate.easeim.section.chat.viewmodel.ChatViewModel;

import java.util.List;

public class OrderListFragment extends BaseInitFragment {
    private RecyclerView recyclerView;
    private OrderListAdapter adapter;
    private ChatViewModel chatViewModel;
    private String type;

    public OrderListFragment(String type) {
        this.type = type;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_order_list;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        adapter = new OrderListAdapter();
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void initData() {
        super.initData();
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        chatViewModel.getOrderObservable().observe(this, response -> {
            parseResource(response, new OnResourceParseCallback<List<EMOrder>>() {
                @Override
                public void onSuccess(@Nullable List<EMOrder> data) {
                    runOnUiThread(() -> adapter.setData(data));
                }

                @Override
                public void onLoading(@Nullable List<EMOrder> data) {
                    super.onLoading(data);
                    showLoading();
                }

                @Override
                public void hideLoading() {
                    super.hideLoading();
                    dismissLoading();
                }
            });
        });

        chatViewModel.getOrderList(type);

    }

    @Override
    protected void initListener() {
        super.initListener();
        adapter.setListener(new OrderListAdapter.OnOrderClickSendListener() {
            @Override
            public void onClick(EMOrder order) {
                Intent intent = new Intent();
                intent.putExtra("order", order);
                getActivity().setResult(Activity.RESULT_OK, intent);
                getActivity().finish();
            }
        });
    }
}
