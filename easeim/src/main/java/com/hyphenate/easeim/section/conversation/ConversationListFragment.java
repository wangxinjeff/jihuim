package com.hyphenate.easeim.section.conversation;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMPushConfigs;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.OnResourceParseCallback;
import com.hyphenate.easeim.common.livedatas.LiveDataBus;
import com.hyphenate.easeim.common.net.Resource;
import com.hyphenate.easeim.common.repositories.EMPushManagerRepository;
import com.hyphenate.easeim.common.utils.ToastUtils;
import com.hyphenate.easeim.common.widget.SearchBar;
import com.hyphenate.easeim.section.base.BaseActivity;
import com.hyphenate.easeim.section.chat.activity.ChatActivity;
import com.hyphenate.easeim.section.chat.viewmodel.MessageViewModel;
import com.hyphenate.easeim.section.conversation.viewmodel.ConversationListViewModel;
import com.hyphenate.easeim.section.dialog.DemoDialogFragment;
import com.hyphenate.easeim.section.dialog.SimpleDialogFragment;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.manager.EaseSystemMsgManager;
import com.hyphenate.easeui.manager.EaseThreadManager;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.easeui.modules.conversation.EaseConversationListFragment;
import com.hyphenate.easeui.modules.conversation.model.EaseConversationInfo;
import com.hyphenate.easeui.utils.EaseCommonUtils;

import java.util.List;


public class ConversationListFragment extends EaseConversationListFragment{

    private ConversationListViewModel mViewModel;

    public ConversationListFragment(int conversationsType) {
        super(conversationsType);
    }

    @Override
    public void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        if(conversationsType == EaseConstant.CON_TYPE_MY_CHAT){
            //添加搜索会话布局
            SearchBar searchBar = new SearchBar(getContext());
            searchBar.init(false);
            searchBar.setOnSearchBarListener(new SearchBar.OnSearchBarListener() {
                @Override
                public void onSearchContent(String text) {
                    conversationListLayout.getListAdapter().getFilter().filter(text);
                }
            });
            llRoot.addView(searchBar, 0);
        }

        if(conversationsType == EaseConstant.CON_TYPE_EXCLUSIVE){
            conversationListLayout.getListAdapter().setEmptyLayoutId(R.layout.ease_layout_no_exclusive_service);
        } else if(conversationsType == EaseConstant.CON_TYPE_MY_CHAT){
            conversationListLayout.getListAdapter().setEmptyLayoutId(R.layout.ease_layout_no_data);
        } else if(conversationsType == EaseConstant.CON_TYPE_ADMIN){
            conversationListLayout.getListAdapter().setEmptyLayoutId(R.layout.ease_layout_no_data_admin);
        }

        initViewModel();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item, int position) {
        EaseConversationInfo info = conversationListLayout.getItem(position);
        Object object = info.getInfo();

        if(object instanceof EMConversation) {
            int itemId = item.getItemId();
            if (itemId == R.id.action_con_make_top) {
                conversationListLayout.makeConversationTop(position, info);
                return true;
            } else if (itemId == R.id.action_con_cancel_top) {
                conversationListLayout.cancelConversationTop(position, info);
                return true;
            } else if (itemId == R.id.action_con_delete) {
                showDeleteDialog(position, info);
                return true;
            }
        }
        return super.onMenuItemClick(item, position);
    }

    private void showDeleteDialog(int position, EaseConversationInfo info) {
        new SimpleDialogFragment.Builder((BaseActivity) mContext)
                .setTitle(R.string.delete_conversation)
                .setOnConfirmClickListener(R.string.delete, new DemoDialogFragment.OnConfirmClickListener() {
                    @Override
                    public void onConfirmClick(View view) {
                        conversationListLayout.deleteConversation(position, info);
                        LiveDataBus.get().with(EaseConstant.CONVERSATION_DELETE).postValue(new EaseEvent(EaseConstant.CONVERSATION_DELETE, EaseEvent.TYPE.MESSAGE));
                    }
                })
                .showCancelButton(true)
                .show();
    }

    @Override
    public void initListener() {
        super.initListener();
    }

    @Override
    public void initData() {
        super.initData();
        //需要两个条件，判断是否触发从服务器拉取会话列表的时机，一是第一次安装，二则本地数据库没有会话列表数据
//        if(EaseIMHelper.getInstance().isFirstInstall()) {
//            mViewModel.fetchConversationsFromServer();
//        }else {
//            super.initData();
//        }
    }

    private void initViewModel() {
        mViewModel = new ViewModelProvider(this).get(ConversationListViewModel.class);

        mViewModel.getDeleteObservable().observe(getViewLifecycleOwner(), response -> {
            parseResource(response, new OnResourceParseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    LiveDataBus.get().with(EaseConstant.MESSAGE_CHANGE_CHANGE).postValue(new EaseEvent(EaseConstant.MESSAGE_CHANGE_CHANGE, EaseEvent.TYPE.MESSAGE));
                    //mViewModel.loadConversationList();
                    conversationListLayout.loadDefaultData();
                }
            });
        });

        mViewModel.getReadObservable().observe(getViewLifecycleOwner(), response -> {
            parseResource(response, new OnResourceParseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    LiveDataBus.get().with(EaseConstant.MESSAGE_CHANGE_CHANGE).postValue(new EaseEvent(EaseConstant.MESSAGE_CHANGE_CHANGE, EaseEvent.TYPE.MESSAGE));
                    conversationListLayout.loadDefaultData();
                }
            });
        });

        mViewModel.getConversationInfoObservable().observe(getViewLifecycleOwner(), response -> {
            parseResource(response, new OnResourceParseCallback<List<EaseConversationInfo>>(true) {
                @Override
                public void onSuccess(@Nullable List<EaseConversationInfo> data) {
                    conversationListLayout.setData(data);
                    // 拉取服务器的会话列表之后设置为不是初次登录
                    EaseIMHelper.getInstance().makeNotFirstInstall();
                }
            });
        });

        MessageViewModel messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        LiveDataBus messageChange = messageViewModel.getMessageChange();
        messageChange.with(EaseConstant.NOTIFY_CHANGE, EaseEvent.class).observe(getViewLifecycleOwner(), this::loadList);
        messageChange.with(EaseConstant.MESSAGE_CHANGE_CHANGE, EaseEvent.class).observe(getViewLifecycleOwner(), this::loadList);
        messageChange.with(EaseConstant.MESSAGE_CHANGE_CHANGE, EaseEvent.class).observe(getViewLifecycleOwner(), event -> {
            if(event == null) {
                return;
            }
            if(event.isMessageChange()){
                if(TextUtils.equals(EaseConstant.MESSAGE_CHANGE_CMD_RECEIVE, event.event)){
                    if(TextUtils.equals(EaseConstant.NO_PUSH_CHANGE, event.message)){
                        EaseThreadManager.getInstance().runOnIOThread(() -> {
                                EMPushConfigs emPushConfigs = new EMPushManagerRepository().fetchPushConfigsFromServer();
                                EaseThreadManager.getInstance().runOnMainThread(() -> loadList(event));
                        });
                    }
                } else if(TextUtils.equals(EaseConstant.MESSAGE_UNREAD_CHANGE, event.event)) {

                } else {
                    loadList(event);
                }
            }
        });

        messageChange.with(EaseConstant.FETCH_CONFIG, EaseEvent.class).observe(getViewLifecycleOwner(), event -> {
            if(event == null) {
                return;
            }
            if(event.isConfigChange()){
                loadList(event);
            }
        });

        messageChange.with(EaseConstant.GROUP_CHANGE, EaseEvent.class).observe(getViewLifecycleOwner(), this::loadList);
        messageChange.with(EaseConstant.CHAT_ROOM_CHANGE, EaseEvent.class).observe(getViewLifecycleOwner(), this::loadList);
        messageChange.with(EaseConstant.CONVERSATION_DELETE, EaseEvent.class).observe(getViewLifecycleOwner(), this::loadList);
        messageChange.with(EaseConstant.CONVERSATION_READ, EaseEvent.class).observe(getViewLifecycleOwner(), this::loadList);
        messageChange.with(EaseConstant.CONTACT_CHANGE, EaseEvent.class).observe(getViewLifecycleOwner(), this::loadList);
        messageChange.with(EaseConstant.CONTACT_ADD, EaseEvent.class).observe(getViewLifecycleOwner(), this::loadList);
        messageChange.with(EaseConstant.CONTACT_UPDATE, EaseEvent.class).observe(getViewLifecycleOwner(), this::loadList);
        messageChange.with(EaseConstant.MESSAGE_CALL_SAVE, Boolean.class).observe(getViewLifecycleOwner(), this::refreshList);
        messageChange.with(EaseConstant.MESSAGE_NOT_SEND, Boolean.class).observe(getViewLifecycleOwner(), this::refreshList);
    }

    private void refreshList(Boolean event) {
        if(event == null) {
            return;
        }
        if(event) {
            conversationListLayout.loadDefaultData();
        }
    }

    private void loadList(EaseEvent change) {
        if(change == null) {
            return;
        }
        if(change.isMessageChange() || change.isNotifyChange()
                || change.isGroupLeave() || change.isChatRoomLeave()
                || change.isContactChange()
                || change.type == EaseEvent.TYPE.CHAT_ROOM || change.isGroupChange()) {
            conversationListLayout.loadDefaultData();
        }
    }

    /**
     * 解析Resource<T>
     * @param response
     * @param callback
     * @param <T>
     */
    public <T> void parseResource(Resource<T> response, @NonNull OnResourceParseCallback<T> callback) {
        if(mContext instanceof BaseActivity) {
            ((BaseActivity) mContext).parseResource(response, callback);
        }
    }

    /**
     * toast by string
     * @param message
     */
    public void showToast(String message) {
        ToastUtils.showToast(message);
    }

    @Override
    public void onItemClick(View view, int position) {
        super.onItemClick(view, position);
        Object item = conversationListLayout.getItem(position).getInfo();
        if(item instanceof EMConversation) {
            if(EaseSystemMsgManager.getInstance().isSystemConversation((EMConversation) item)) {

            }else {
                ChatActivity.actionStart(mContext, ((EMConversation)item).conversationId(), EaseCommonUtils.getChatType((EMConversation) item));
            }
        }
    }

    @Override
    public void notifyItemChange(int position) {
        super.notifyItemChange(position);
        LiveDataBus.get().with(EaseConstant.MESSAGE_CHANGE_CHANGE).postValue(new EaseEvent(EaseConstant.MESSAGE_CHANGE_CHANGE, EaseEvent.TYPE.MESSAGE));
    }

    @Override
    public void notifyAllChange() {
        super.notifyAllChange();
    }
}
