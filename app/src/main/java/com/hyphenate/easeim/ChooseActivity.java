package com.hyphenate.easeim;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.hyphenate.easeim.login.activity.LoginActivity;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeui.constants.EaseConstant;

import static android.view.View.GONE;

public class ChooseActivity extends BaseInitActivity {

    Button customer;
    Button admin;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_choose;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        if(EaseIMHelper.getInstance().getAutoLogin()){
            if(EaseIMHelper.getInstance().getModel().getAppMode()){
                startActivity(new Intent(ChooseActivity.this, AdminLoginActivity.class));
            } else {
//                LoginActivity.startAction(ChooseActivity.this);
                EaseIMHelper.getInstance().setAid("222510");
                EaseIMHelper.getInstance().setAidToken("ad8s8d9adhka");
                EaseIMHelper.getInstance().loginSuccess();
                startActivity(new Intent(ChooseActivity.this, MainActivity.class));
            }
            finish();
        }

        customer = findViewById(R.id.customer);
        admin = findViewById(R.id.admin);
        admin.setVisibility(GONE);
    }

    @Override
    protected void initListener() {
        super.initListener();
        customer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EaseIMHelper.getInstance().setAid("222510");
                EaseIMHelper.getInstance().setAidToken("ad8s8d9adhka");
                EaseIMHelper.getInstance().getModel().setAppMode(false);
                LoginActivity.startAction(ChooseActivity.this);
                finish();
            }
        });
        admin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EaseIMHelper.getInstance().getModel().setAppMode(true);
                startActivity(new Intent(ChooseActivity.this, AdminLoginActivity.class));
                finish();
            }
        });
    }
}
