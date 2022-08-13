package com.hyphenate.easeim.section.chat.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.arch.lifecycle.ViewModelProvider;
import android.content.Intent;
import android.os.Bundle;

import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.OnResourceParseCallback;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeim.common.model.EMOrder;
import com.hyphenate.easeim.common.repositories.EMChatManagerRepository;
import com.hyphenate.easeim.section.base.BaseInitFragment;
import com.hyphenate.easeim.section.chat.adapter.OrderListAdapter;

import java.util.List;

@SuppressLint("ValidFragment")
public class OrderListFragment extends BaseInitFragment {
    private RecyclerView recyclerView;
    private OrderListAdapter adapter;
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
        showLoading();
        EMChatManagerRepository.getInstance().fetchOrderListFroServer(type, new ResultCallBack<List<EMOrder>>() {
            @Override
            public void onSuccess(List<EMOrder> data) {
                dismissLoading();
                runOnUiThread(() -> adapter.setData(data));
            }

            @Override
            public void onError(int i, String s) {
                dismissLoading();
            }
        });

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
