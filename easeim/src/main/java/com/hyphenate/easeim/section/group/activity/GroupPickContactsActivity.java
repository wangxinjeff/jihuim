package com.hyphenate.easeim.section.group.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.OnResourceParseCallback;
import com.hyphenate.easeim.common.livedatas.LiveDataBus;
import com.hyphenate.easeim.common.utils.ToastUtils;
import com.hyphenate.easeim.common.widget.SearchBar;
import com.hyphenate.easeim.section.group.delegate.PickContactDelegate;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeim.section.group.adapter.GroupPickContactsAdapter;
import com.hyphenate.easeim.section.group.viewmodels.GroupPickContactsViewModel;
import com.hyphenate.easeui.EaseIM;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.easeui.provider.EaseUserProfileProvider;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.easeui.widget.EaseTitleBar;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

public class GroupPickContactsActivity extends BaseInitActivity implements EaseTitleBar.OnRightClickListener, EaseTitleBar.OnBackPressListener, PickContactDelegate.onCloseClickListener {
    private EaseTitleBar titleBar;
    private RecyclerView rvList;
    protected GroupPickContactsAdapter adapter;
    private GroupPickContactsViewModel viewModel;
    private String groupId;
    private boolean isCreate;
    private List<EaseUser> members;
    private AppCompatImageView userAvatar;
    private AppCompatTextView userName;
    private RelativeLayout resultView;
    private RadioGroup radioGroup;
    private RadioButton rbService;
    private RadioButton rbCustomer;
    private EaseUser result;
    private boolean isOwner;

    private SearchBar searchBar;

    private AppCompatTextView selectedTitle;
    private AppCompatTextView resultTitle;
    private List<EaseUser> selectedList = new ArrayList<>();

    public static void actionStart(Activity context, String groupId, boolean isOwner, boolean isCreate) {
        Intent starter = new Intent(context, GroupPickContactsActivity.class);
        starter.putExtra("groupId", groupId);
        starter.putExtra("isCreate", isCreate);
        starter.putExtra("isOwner", isOwner);
        context.startActivity(starter);
    }

    public static void actionStart(Activity context, ArrayList<EaseUser> members) {
        Intent starter = new Intent(context, GroupPickContactsActivity.class);
        starter.putParcelableArrayListExtra("members", members);
        starter.putExtra("isCreate", true);
        context.startActivity(starter);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.em_activity_chat_group_pick_contacts;
    }

    @Override
    protected void initIntent(Intent intent) {
        super.initIntent(intent);
        groupId = intent.getStringExtra("groupId");
        isCreate = intent.getBooleanExtra("isCreate", false);
        members = intent.getParcelableArrayListExtra("members");
        isOwner = intent.getBooleanExtra("isOwner", false);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        titleBar = findViewById(R.id.title_bar);
        if(EaseIMHelper.getInstance().isAdmin()){
            titleBar.setLeftImageResource(R.drawable.em_icon_back_admin);
        }

        rvList = findViewById(R.id.rl_user_list);
        userAvatar = findViewById(R.id.user_avatar);
        userName = findViewById(R.id.user_name);
        resultView = findViewById(R.id.result_view);
        radioGroup = findViewById(R.id.rb_group);
        rbService = findViewById(R.id.rb_service);
        rbCustomer = findViewById(R.id.rb_customer);

        titleBar.getRightText().setTextColor(ContextCompat.getColor(mContext, R.color.search_close_color));

        searchBar = findViewById(R.id.search_bar);
        searchBar.init(true);
        searchBar.setSearchShowText(getString(R.string.em_input_phone_search));

        selectedTitle = findViewById(R.id.selected_title);
        resultTitle = findViewById(R.id.result_title);

        /**
         * FlexboxLayout参考：https://blog.csdn.net/weixin_39397471/article/details/90212231
         */
        FlexboxLayoutManager manager = new FlexboxLayoutManager(this);
        manager.setFlexDirection(FlexDirection.ROW);
        manager.setFlexWrap(FlexWrap.WRAP);
        manager.setAlignItems(AlignItems.CENTER);
        manager.setJustifyContent(JustifyContent.FLEX_START);
        rvList.setLayoutManager(manager);
        adapter = new GroupPickContactsAdapter();
        PickContactDelegate delegate = new PickContactDelegate();
        delegate.setCloseClickListener(this);
        adapter.addDelegate(delegate);
        rvList.setAdapter(adapter);

        if(members != null && members.size() > 0){
            selectedList = members;
            adapter.setData(selectedList);
        }

        refreshSelectedView();
    }

    @Override
    protected void initListener() {
        super.initListener();
        titleBar.setOnBackPressListener(this);
        titleBar.setOnRightClickListener(this);
        addRadioGroupListener();

        searchBar.setOnSearchBarListener(new SearchBar.OnSearchBarListener() {
            @Override
            public void onSearchContent(String text) {
                radioGroup.setOnCheckedChangeListener(null);
                radioGroup.clearCheck();
                addRadioGroupListener();
                if(EaseIMHelper.getInstance().isAdmin()){
                    viewModel.searchUserWithAdmin(text);
                } else {
                    viewModel.searchUserWithCustomer(text);
                }

            }
        });
    }

    private void addRadioGroupListener(){
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = findViewById(checkedId);
                if(TextUtils.equals(radioButton.getText(), getString(R.string.em_service_personnel))){
                    result.setCustomer(false);
                } else {
                    result.setCustomer(true);
                }
                for(EaseUser item : selectedList){
                    if(TextUtils.equals(item.getUsername(), result.getUsername())){
                        int index = selectedList.indexOf(item);
                        selectedList.remove(item);
                        selectedList.add(index, result);
                        adapter.setData(selectedList);
                        refreshSelectedView();
                        return;
                    }
                }
                selectedList.add(result);
                adapter.setData(selectedList);
                refreshSelectedView();
            }
        });
    }

    @Override
    protected void initData() {
        super.initData();
        viewModel = new ViewModelProvider(this).get(GroupPickContactsViewModel.class);

        viewModel.getAddMembersObservable().observe(this, response -> {
            parseResource(response, new OnResourceParseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    if(!isOwner){
                        ToastUtils.showCenterToast("", getString(R.string.em_invite_user_toast), 0, Toast.LENGTH_SHORT);
                    } else {
                        ToastUtils.showCenterToast("", getString(R.string.em_add_user_toast), 0, Toast.LENGTH_SHORT);
                    }
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onLoading(@Nullable Boolean data) {
                    super.onLoading(data);
                    showLoading();
                }

                @Override
                public void hideLoading() {
                    super.hideLoading();
                    dismissLoading();
                }

                @Override
                public void onError(int code, String message) {
                    super.onError(code, message);

                }
            });
        });
        viewModel.getSearchContactsObservable().observe(this, response -> {
            parseResource(response, new OnResourceParseCallback<List<EaseUser>>() {
                @Override
                public void onSuccess(List<EaseUser> data) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(data.size() > 0){
                                result = data.get(0);
                                resultView.setVisibility(View.VISIBLE);
                                userName.setText(result.getNickname());
                                Glide.with(mContext).load(result.getAvatar())
                                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                        .error(R.drawable.em_default_avatar)
                                        .into(userAvatar);
                            } else {
                                resultView.setVisibility(View.GONE);
                                result = null;
                                ToastUtils.showCenterToast("", "搜索无结果", 0, Toast.LENGTH_SHORT);
                            }
                        }
                    });
                }

                @Override
                public void onLoading(@Nullable List<EaseUser> data) {
                    super.onLoading(data);
                    showLoading();
                }

                @Override
                public void hideLoading() {
                    super.hideLoading();
                    dismissLoading();
                }

                @Override
                public void onError(int code, String message) {
                    super.onError(code, message);
                    ToastUtils.showCenterToast("", "搜索失败:" + code + ":" + message, 0, Toast.LENGTH_SHORT);
                }
            });
        });
    }

    @Override
    public void onRightClick(View view) {
        if(selectedList.size() <= 0 && resultView.getVisibility() == View.VISIBLE){
            ToastUtils.showCenterToast("", "请选择用户身份", 0, Toast.LENGTH_SHORT);
            return;
        }
        if(isCreate){
            NewGroupActivity.actionStart(mContext, (ArrayList<EaseUser>) selectedList);
            finish();
        } else {
            if(selectedList.size() > 0){
                List<String> customers = new ArrayList<>();
                List<String> waiters = new ArrayList<>();
                for (EaseUser user : selectedList) {
                    if(user.isCustomer()){
                        customers.add(user.getUsername());
                    } else {
                        waiters.add(user.getUsername());
                    }
                }
                if(EaseIMHelper.getInstance().isAdmin()){
                    viewModel.addGroupMembersWithAdmin(isOwner, groupId, customers, waiters);
                } else {
                    viewModel.addGroupMembersWithCustomer(isOwner, groupId, customers, waiters);
                }
            } else {
                onBackPressed();
            }
        }
    }

    @Override
    public void onBackPress(View view) {
        onBackPressed();
    }

    @Override
    public void onMemberRemove(EaseUser item) {
        selectedList.remove(item);
        if(TextUtils.equals(result.getUsername(), item.getUsername())){
            radioGroup.setOnCheckedChangeListener(null);
            radioGroup.clearCheck();
            addRadioGroupListener();
        }
        refreshSelectedView();
        adapter.setData(selectedList);
    }

    private void refreshSelectedView(){
        if(selectedList.size() == 0){
            selectedTitle.setVisibility(View.GONE);
            rvList.setVisibility(View.GONE);
        } else {
            selectedTitle.setVisibility(View.VISIBLE);
            rvList.setVisibility(View.VISIBLE);
        }
    }

}
