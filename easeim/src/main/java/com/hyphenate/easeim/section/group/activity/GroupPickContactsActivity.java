package com.hyphenate.easeim.section.group.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
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
import com.hyphenate.EMCallBack;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeim.common.repositories.EMGroupManagerRepository;
import com.hyphenate.easeim.common.utils.ToastUtils;
import com.hyphenate.easeim.common.widget.SearchBar;
import com.hyphenate.easeim.section.group.delegate.PickContactDelegate;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeim.section.group.adapter.GroupPickContactsAdapter;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.widget.EaseTitleBar;

import java.util.ArrayList;
import java.util.List;

import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v4.content.ContextCompat;

public class GroupPickContactsActivity extends BaseInitActivity implements EaseTitleBar.OnRightClickListener, EaseTitleBar.OnBackPressListener, PickContactDelegate.onCloseClickListener {
    private EaseTitleBar titleBar;
    private RecyclerView rvList;
    protected GroupPickContactsAdapter adapter;
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
    private EMGroupManagerRepository repository = EMGroupManagerRepository.getInstance();

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
        titleBar.setLeftImageResource(R.drawable.em_icon_back_admin);

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
                showLoading();
                radioGroup.setOnCheckedChangeListener(null);
                radioGroup.clearCheck();
                addRadioGroupListener();
                    repository.searchUserWithAdmin(text, new ResultCallBack<List<EaseUser>>() {
                        @Override
                        public void onSuccess(List<EaseUser> data) {
                            dismissLoading();
                            runOnUiThread(() -> {
                                if(data.size() > 0){
                                    result = data.get(0);
                                    resultView.setVisibility(View.VISIBLE);
                                    userName.setText(result.getNickname());
                                    Glide.with(mContext).load(result.getAvatar())
                                            .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                            .error(R.drawable.ease_default_avatar)
                                            .into(userAvatar);
                                } else {
                                    resultView.setVisibility(View.GONE);
                                    result = null;
                                    ToastUtils.showCenterToast("", "搜索无结果", 0, Toast.LENGTH_SHORT);
                                }
                            });
                        }

                        @Override
                        public void onError(int i, String s) {
                            dismissLoading();
                            ToastUtils.showCenterToast("", "搜索失败:" + i + ":" + s, 0, Toast.LENGTH_SHORT);
                        }
                    });
            }
        });
    }

    private void addRadioGroupListener(){
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = findViewById(checkedId);
                result.setCustomer(!TextUtils.equals(radioButton.getText(), getString(R.string.em_service_personnel)));
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
                showLoading();
                List<String> customers = new ArrayList<>();
                List<String> waiters = new ArrayList<>();
                for (EaseUser user : selectedList) {
                    if(user.isCustomer()){
                        customers.add(user.getUsername());
                    } else {
                        waiters.add(user.getUsername());
                    }
                }
                    repository.addMembersWithAdmin(groupId, customers, waiters, new EMCallBack() {
                        @Override
                        public void onSuccess() {
                            dismissLoading();
                            ToastUtils.showCenterToast("", getString(R.string.em_add_user_toast), 0, Toast.LENGTH_SHORT);
                            runOnUiThread(() -> {
                                setResult(RESULT_OK);
                                finish();
                            });
                        }

                        @Override
                        public void onError(int i, String s) {
                            dismissLoading();
                        }
                    });

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
