package com.hyphenate.easeim.login.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.section.base.BaseFragment;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.easeim.login.fragment.LoginFragment;
import com.hyphenate.easeim.login.viewmodels.LoginViewModel;

public class LoginActivity extends BaseInitActivity {

    public static void startAction(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.demo_activity_login;
    }

    @Override
    protected void initSystemFit() {
        setFitSystemForTheme(false, R.color.transparent);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        getSupportFragmentManager().
                beginTransaction().
                replace(R.id.fl_fragment, new LoginFragment()).
                commit();
    }

    @Override
    protected void initData() {
        super.initData();
        LoginViewModel viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        viewModel.getPageSelect().observe(this, page -> {
            if(page == 0) {
                return;
            }
        });
    }

    private void replace(BaseFragment fragment) {
        getSupportFragmentManager().
                beginTransaction().
                setCustomAnimations(
                        R.anim.em_slide_in_from_right,
                        R.anim.em_slide_out_to_left,
                        R.anim.em_slide_in_from_left,
                        R.anim.em_slide_out_to_right
                ).
                replace(R.id.fl_fragment, fragment).
                addToBackStack(null).
                commit();
    }
}
