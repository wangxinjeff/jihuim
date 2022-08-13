package com.hyphenate.easeim.section.search.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.widget.SearchBar;
import com.hyphenate.easeim.section.base.BaseInitFragment;
import com.hyphenate.easeim.section.search.ShowChatHistoryActivity;
import com.hyphenate.easeim.section.search.adapter.SearchAllAdapter;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.interfaces.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("ValidFragment")
public class SearchAllFragment extends BaseInitFragment {

    private SearchBar searchBar;
    private RecyclerView recyclerView;
    private SearchAllAdapter allAdapter;
    private EMConversation conversation;
    private String conversationId;
    private int chatType;

    public SearchAllFragment(String conversationId, int chatType) {
        this.conversationId = conversationId;
        this.chatType = chatType;
        conversation = EMClient.getInstance().chatManager().getConversation(conversationId);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_search_all;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        searchBar = findViewById(R.id.search_bar);
        searchBar.init(true);
        recyclerView = findViewById(R.id.rl_msg_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        allAdapter = new SearchAllAdapter();
        recyclerView.setAdapter(allAdapter);
    }

    @Override
    protected void initData() {
        super.initData();
    }

    @Override
    protected void initListener() {
        super.initListener();
        searchBar.setOnSearchBarListener(new SearchBar.OnSearchBarListener() {
            @Override
            public void onSearchContent(String text) {
                searchMessages(text);
            }
        });

        allAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                ShowChatHistoryActivity.actionStart(getContext(), conversationId, chatType, allAdapter.getData().get(position).getMsgId());
            }
        });
    }

    private void searchMessages(String key){
        if(conversation != null){
            List<EMMessage> mData = conversation.searchMsgFromDB(key, System.currentTimeMillis(), 100, null, EMConversation.EMSearchDirection.UP);
            List<EMMessage> data = new ArrayList<>();
            for(EMMessage message : mData){
                if(!(message.getBooleanAttribute(EaseConstant.MESSAGE_TYPE_RECALL, false)
                        || !message.getStringAttribute(EaseConstant.MESSAGE_ATTR_CALL_STATE, "").equals("")
                        || message.getBooleanAttribute(EaseConstant.CREATE_GROUP_PROMPT, false)
                        || message.getBooleanAttribute(EaseConstant.JOIN_GROUP_PROMPT, false))){
                    if(message.getType() == EMMessage.Type.TXT){
                        EMTextMessageBody body = (EMTextMessageBody) message.getBody();
                        if(body.getMessage().contains(key)){
                            data.add(message);
                        }
                    }
                }
            }

            allAdapter.setKeyword(key);
            allAdapter.setData(data);
        }
    }
}
