package com.hyphenate.easeim.section.group.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import android.support.v7.widget.AppCompatTextView;
import android.support.v4.content.ContextCompat;

import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeim.common.repositories.EMGroupManagerRepository;
import com.hyphenate.easeim.common.utils.ToastUtils;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.easeui.widget.EaseTitleBar;
import com.hyphenate.util.EMLog;

import java.util.List;

public class GroupEditActivity extends BaseInitActivity {

    private EaseTitleBar titleBar;
    private AppCompatEditText editContent;
    private LinearLayout noteView;
    private AppCompatTextView systemNote;
    private AppCompatEditText serviceNote;

    private String title;
    private String systemContent;
    private String content;
    private String hint;
    private boolean canEdit;
    private boolean isShowNote;
    private int requestCode;
    private String groupId;
    private boolean updateSuccess;
    private String updateContent;
    private String systemNoteText;
    private String serviceNoteText;

    public static void startEditForResult(Activity context, String title, String content, String hint, boolean canEdit, String groupId, int requestCode){
        Intent intent = new Intent(context, GroupEditActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("hint", hint);
        intent.putExtra("canEdit", canEdit);
        intent.putExtra("content", content);
        intent.putExtra("isShowNote", false);
        intent.putExtra("groupId", groupId);
        context.startActivityForResult(intent, requestCode);
    }

    public static void startNoteForResult(Activity context, String title, String systemContent, String content, String hint, String groupId, int requestCode){
        Intent intent = new Intent(context, GroupEditActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("hint", hint);
        intent.putExtra("canEdit", true);
        intent.putExtra("systemContent", systemContent);
        intent.putExtra("content", content);
        intent.putExtra("isShowNote", true);
        intent.putExtra("groupId", groupId);
        context.startActivityForResult(intent, requestCode);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.em_activity_group_edit;
    }

    @Override
    protected void initIntent(Intent intent) {
        super.initIntent(intent);
        title = intent.getStringExtra("title");
        hint = intent.getStringExtra("hint");
        canEdit = intent.getBooleanExtra("canEdit", false);
        systemContent = intent.getStringExtra("systemContent");
        content = intent.getStringExtra("content");
        isShowNote = intent.getBooleanExtra("isShowNote", false);
        groupId = intent.getStringExtra("groupId");
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        titleBar = findViewById(R.id.title_bar);
        editContent = findViewById(R.id.et_content);
        noteView = findViewById(R.id.group_note_view);
        systemNote = findViewById(R.id.system_note);
        serviceNote = findViewById(R.id.service_note);

        titleBar.setLeftImageResource(R.drawable.em_icon_back_admin);

        if(isShowNote){
            noteView.setVisibility(View.VISIBLE);
            editContent.setVisibility(View.GONE);
            systemNote.setText(systemContent);
            if(TextUtils.isEmpty(content)){
                serviceNote.setHint(hint);
            } else {
                serviceNote.setText(content);
            }
            serviceNote.setEnabled(false);
        } else {
            noteView.setVisibility(View.GONE);
            editContent.setVisibility(View.VISIBLE);
            if(TextUtils.isEmpty(content)){
                editContent.setHint(hint);
            } else {
                editContent.setText(content);
            }
            editContent.setEnabled(false);
        }

        titleBar.setTitle(title);
        titleBar.getRightLayout().setVisibility(canEdit ? View.VISIBLE : View.GONE);
        if(canEdit){
            titleBar.getRightText().setText(R.string.em_action_edit);
            titleBar.getRightText().setTextColor(ContextCompat.getColor(this, R.color.search_close));
        }
    }

    @Override
    protected void initData() {
        super.initData();

        if(isShowNote){
            showLoading();
            EMGroupManagerRepository.getInstance().getServiceNote(groupId, new ResultCallBack<List<String>>() {
                @Override
                public void onSuccess(List<String> data) {
                    dismissLoading();
                    runOnUiThread(() -> {
                        systemNoteText = data.get(0);
                        serviceNoteText = data.get(1);
                        systemNote.setText(systemNoteText);
                        serviceNote.setText(serviceNoteText);
                    });
                }

                @Override
                public void onError(int i, String s) {
                    dismissLoading();
                }
            });
        }
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
        if(canEdit){
            titleBar.setOnRightClickListener(new EaseTitleBar.OnRightClickListener() {
                @Override
                public void onRightClick(View view) {
                    if(TextUtils.equals(getString(R.string.em_action_edit), titleBar.getRightText().getText().toString())){
                        titleBar.getRightText().setText(R.string.em_chat_group_save);
                        if(isShowNote){
                            serviceNote.setEnabled(true);
                            EaseCommonUtils.showSoftKeyBoard(serviceNote);
                            serviceNote.setSelection(serviceNote.getText().length());
                        } else {
                            editContent.setEnabled(true);
                            EaseCommonUtils.showSoftKeyBoard(editContent);
                            editContent.setSelection(editContent.getText().length());
                        }
                    } else if(TextUtils.equals(getString(R.string.em_chat_group_save), titleBar.getRightText().getText().toString())){
                        if(isShowNote){
                            updateContent = serviceNote.getText().toString();
                        } else {
                            updateContent = editContent.getText().toString();
                        }
                        if(TextUtils.isEmpty(updateContent)){
                            ToastUtils.showCenterToast("", getString(R.string.em_content_not_empty), 0 , Toast.LENGTH_SHORT);
                            return;
                        }
                        showLoading();
                        if(TextUtils.equals(title, getString(R.string.em_chat_group_detail_name))){
                            //修改群名称
                            EMGroupManagerRepository.getInstance().setGroupName(groupId, editContent.getText().toString(), new ResultCallBack<String>() {
                                @Override
                                public void onSuccess(String s) {
                                    dismissLoading();
                                    updateSuccess = true;
                                    ToastUtils.showCenterToast("", getString(R.string.em_save_success), 0 , Toast.LENGTH_SHORT);
                                }

                                @Override
                                public void onError(int i, String s) {
                                    dismissLoading();
                                    EMLog.e("edit", "change groupName failed:" + i + " : " + s);
                                    ToastUtils.showCenterToast("", getString(R.string.em_save_failed), 0 , Toast.LENGTH_SHORT);
                                }
                            });
                        } else if(TextUtils.equals(title, getString(R.string.em_chat_group_detail_announcement))){
                            //修改群公告
                            EMGroupManagerRepository.getInstance().setGroupAnnouncement(groupId, editContent.getText().toString(), new ResultCallBack<String>() {
                                @Override
                                public void onSuccess(String s) {
                                    dismissLoading();
                                    updateSuccess = true;
                                    ToastUtils.showCenterToast("", getString(R.string.em_save_success), 0 , Toast.LENGTH_SHORT);
                                }

                                @Override
                                public void onError(int i, String s) {
                                    dismissLoading();
                                    EMLog.e("edit", "change announcement failed:" + i + " : " + s);
                                    ToastUtils.showCenterToast("", getString(R.string.em_save_failed), 0 , Toast.LENGTH_SHORT);
                                }
                            });
                        } else if(TextUtils.equals(title, getString(R.string.em_chat_group_detail_introduction))){
                            //修改群介绍
                            EMGroupManagerRepository.getInstance().setGroupDescription(groupId, editContent.getText().toString(), new ResultCallBack<String>() {
                                @Override
                                public void onSuccess(String s) {
                                    dismissLoading();
                                    updateSuccess = true;
                                    ToastUtils.showCenterToast("", getString(R.string.em_save_success), 0 , Toast.LENGTH_SHORT);
                                }

                                @Override
                                public void onError(int i, String s) {
                                    dismissLoading();
                                    EMLog.e("edit", "change description failed:" + i + " : " + s);
                                    ToastUtils.showCenterToast("", getString(R.string.em_save_failed), 0 , Toast.LENGTH_SHORT);
                                }
                            });
                        } else if(TextUtils.equals(title, getString(R.string.em_group_note))){
                            //修改运营备注
                            EMGroupManagerRepository.getInstance().changeServiceNote(groupId, serviceNote.getText().toString(), new ResultCallBack() {
                                @Override
                                public void onSuccess(Object o) {
                                    dismissLoading();
                                    updateSuccess = true;
                                    ToastUtils.showCenterToast("", getString(R.string.em_save_success), 0, Toast.LENGTH_SHORT);
                                }

                                @Override
                                public void onError(int i, String s) {
                                    dismissLoading();
                                    EMLog.e("edit", "note save failed:" + i + " : " + s);
                                    ToastUtils.showCenterToast("", getString(R.string.em_save_failed), 0 , Toast.LENGTH_SHORT);
                                }
                            });
                        }
                        titleBar.getRightText().setText(R.string.em_action_edit);
                        if(isShowNote){
                            serviceNote.setEnabled(false);
                            EaseCommonUtils.hideSoftKeyBoard(serviceNote);
                        } else {
                            editContent.setEnabled(false);
                            EaseCommonUtils.hideSoftKeyBoard(editContent);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if(updateSuccess){
            Intent intent = new Intent();
            intent.putExtra("content", updateContent);
            setResult(RESULT_OK, intent);
        }
        super.onBackPressed();
    }
}
