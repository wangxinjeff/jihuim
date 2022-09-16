/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hyphenate.mediapicker.internal.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.viewpager.widget.ViewPager;

import com.hyphenate.easeim.R;
import com.hyphenate.mediapicker.internal.entity.EMIncapableCause;
import com.hyphenate.mediapicker.internal.entity.EMItem;
import com.hyphenate.mediapicker.internal.entity.EMSelectionSpec;
import com.hyphenate.mediapicker.internal.model.EMSelectedItemCollection;
import com.hyphenate.mediapicker.internal.ui.adapter.EMPreviewPagerAdapter;
import com.hyphenate.mediapicker.internal.ui.widget.EMCheckRadioView;
import com.hyphenate.mediapicker.internal.ui.widget.EMCheckView;
import com.hyphenate.mediapicker.internal.ui.widget.EMIncapableDialog;
import com.hyphenate.mediapicker.internal.utils.EMPhotoMetadataUtils;
import com.hyphenate.mediapicker.internal.utils.EMPlatform;
import com.hyphenate.mediapicker.listener.EMOnFragmentInteractionListener;

public abstract class EMBasePreviewActivity extends AppCompatActivity implements View.OnClickListener,
        ViewPager.OnPageChangeListener, EMOnFragmentInteractionListener {

    public static final String EXTRA_DEFAULT_BUNDLE = "extra_default_bundle";
    public static final String EXTRA_RESULT_BUNDLE = "extra_result_bundle";
    public static final String EXTRA_RESULT_APPLY = "extra_result_apply";
    public static final String EXTRA_RESULT_ORIGINAL_ENABLE = "extra_result_original_enable";
    public static final String CHECK_STATE = "checkState";

    protected final EMSelectedItemCollection mSelectedCollection = new EMSelectedItemCollection(this);
    protected EMSelectionSpec mSpec;
    protected ViewPager mPager;

    protected EMPreviewPagerAdapter mAdapter;

    protected EMCheckView mCheckView;
    protected TextView mButtonBack;
    protected TextView mButtonApply;
    protected TextView mSize;

    protected int mPreviousPos = -1;

    private LinearLayout mOriginalLayout;
    private EMCheckRadioView mOriginal;
    protected boolean mOriginalEnable;

    private FrameLayout mBottomToolbar;
    private FrameLayout mTopToolbar;
    private boolean mIsToolbarHide = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(EMSelectionSpec.getInstance().themeId);
        super.onCreate(savedInstanceState);
        if (!EMSelectionSpec.getInstance().hasInited) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        setContentView(R.layout.em_activity_media_preview);
        if (EMPlatform.hasKitKat()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        mSpec = EMSelectionSpec.getInstance();
        if (mSpec.needOrientationRestriction()) {
            setRequestedOrientation(mSpec.orientation);
        }

        if (savedInstanceState == null) {
            mSelectedCollection.onCreate(getIntent().getBundleExtra(EXTRA_DEFAULT_BUNDLE));
            mOriginalEnable = getIntent().getBooleanExtra(EXTRA_RESULT_ORIGINAL_ENABLE, false);
        } else {
            mSelectedCollection.onCreate(savedInstanceState);
            mOriginalEnable = savedInstanceState.getBoolean(CHECK_STATE);
        }
        mButtonBack = (TextView) findViewById(R.id.button_back);
        mButtonApply = (TextView) findViewById(R.id.button_apply);
        mSize = (TextView) findViewById(R.id.size);
        mButtonBack.setOnClickListener(this);
        mButtonApply.setOnClickListener(this);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.addOnPageChangeListener(this);
        mAdapter = new EMPreviewPagerAdapter(getSupportFragmentManager(), null);
        mPager.setAdapter(mAdapter);
        mCheckView = (EMCheckView) findViewById(R.id.check_view);
        mCheckView.setCountable(mSpec.countable);
        mBottomToolbar = findViewById(R.id.bottom_toolbar);
        mTopToolbar = findViewById(R.id.top_toolbar);

        mCheckView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                EMItem item = mAdapter.getMediaItem(mPager.getCurrentItem());
                if (mSelectedCollection.isSelected(item)) {
                    mSelectedCollection.remove(item);
                    if (mSpec.countable) {
                        mCheckView.setCheckedNum(EMCheckView.UNCHECKED);
                    } else {
                        mCheckView.setChecked(false);
                    }
                } else {
                    if (assertAddSelection(item)) {
                        mSelectedCollection.add(item);
                        if (mSpec.countable) {
                            mCheckView.setCheckedNum(mSelectedCollection.checkedNumOf(item));
                        } else {
                            mCheckView.setChecked(true);
                        }
                    }
                }
                updateApplyButton();

                if (mSpec.onSelectedListener != null) {
                    mSpec.onSelectedListener.onSelected(
                            mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString());
                }
            }
        });


        mOriginalLayout = findViewById(R.id.originalLayout);
        mOriginal = findViewById(R.id.original);
        mOriginalLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int count = countOverMaxSize();
                if (count > 0) {
                    EMIncapableDialog incapableDialog = EMIncapableDialog.newInstance("",
                            getString(R.string.em_error_over_original_count, count, mSpec.originalMaxSize));
                    incapableDialog.show(getSupportFragmentManager(),
                            EMIncapableDialog.class.getName());
                    return;
                }

                mOriginalEnable = !mOriginalEnable;
                mOriginal.setChecked(mOriginalEnable);
                if (!mOriginalEnable) {
                    mOriginal.setColor(Color.WHITE);
                }


                if (mSpec.onCheckedListener != null) {
                    mSpec.onCheckedListener.onCheck(mOriginalEnable);
                }
            }
        });

        updateApplyButton();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mSelectedCollection.onSaveInstanceState(outState);
        outState.putBoolean("checkState", mOriginalEnable);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        sendBackResult(false);
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_back) {
            onBackPressed();
        } else if (v.getId() == R.id.button_apply) {
            sendBackResult(true);
            finish();
        }
    }

    @Override
    public void onClick() {
        if (!mSpec.autoHideToobar) {
            return;
        }

        if (mIsToolbarHide) {
            mTopToolbar.animate()
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .translationYBy(mTopToolbar.getMeasuredHeight())
                    .start();
            mBottomToolbar.animate()
                    .translationYBy(-mBottomToolbar.getMeasuredHeight())
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .start();
        } else {
            mTopToolbar.animate()
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .translationYBy(-mTopToolbar.getMeasuredHeight())
                    .start();
            mBottomToolbar.animate()
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .translationYBy(mBottomToolbar.getMeasuredHeight())
                    .start();
        }

        mIsToolbarHide = !mIsToolbarHide;

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        EMPreviewPagerAdapter adapter = (EMPreviewPagerAdapter) mPager.getAdapter();
        if (mPreviousPos != -1 && mPreviousPos != position) {
            ((EMPreviewItemFragment) adapter.instantiateItem(mPager, mPreviousPos)).resetView();

            EMItem item = adapter.getMediaItem(position);
            if (mSpec.countable) {
                int checkedNum = mSelectedCollection.checkedNumOf(item);
                mCheckView.setCheckedNum(checkedNum);
                if (checkedNum > 0) {
                    mCheckView.setEnabled(true);
                } else {
                    mCheckView.setEnabled(!mSelectedCollection.maxSelectableReached());
                }
            } else {
                boolean checked = mSelectedCollection.isSelected(item);
                mCheckView.setChecked(checked);
                if (checked) {
                    mCheckView.setEnabled(true);
                } else {
                    mCheckView.setEnabled(!mSelectedCollection.maxSelectableReached());
                }
            }
            updateSize(item);
        }
        mPreviousPos = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private void updateApplyButton() {
        int selectedCount = mSelectedCollection.count();
        if (selectedCount == 0) {
            mButtonApply.setText(R.string.em_button_apply_default);
            mButtonApply.setEnabled(false);
        } else if (selectedCount == 1 && mSpec.singleSelectionModeEnabled()) {
            mButtonApply.setText(R.string.em_button_apply_default);
            mButtonApply.setEnabled(true);
        } else {
            mButtonApply.setEnabled(true);
            mButtonApply.setText(getString(R.string.em_button_apply, selectedCount));
        }

        if (mSpec.originalable) {
            mOriginalLayout.setVisibility(View.VISIBLE);
            updateOriginalState();
        } else {
            mOriginalLayout.setVisibility(View.GONE);
        }
    }


    private void updateOriginalState() {
        mOriginal.setChecked(mOriginalEnable);
        if (!mOriginalEnable) {
            mOriginal.setColor(Color.WHITE);
        }

        if (countOverMaxSize() > 0) {

            if (mOriginalEnable) {
                EMIncapableDialog incapableDialog = EMIncapableDialog.newInstance("",
                        getString(R.string.em_error_over_original_size, mSpec.originalMaxSize));
                incapableDialog.show(getSupportFragmentManager(),
                        EMIncapableDialog.class.getName());

                mOriginal.setChecked(false);
                mOriginal.setColor(Color.WHITE);
                mOriginalEnable = false;
            }
        }
    }


    private int countOverMaxSize() {
        int count = 0;
        int selectedCount = mSelectedCollection.count();
        for (int i = 0; i < selectedCount; i++) {
            EMItem item = mSelectedCollection.asList().get(i);
            if (item.isImage()) {
                float size = EMPhotoMetadataUtils.getSizeInMB(item.size);
                if (size > mSpec.originalMaxSize) {
                    count++;
                }
            }
        }
        return count;
    }

    protected void updateSize(EMItem item) {
        if (item.isGif()) {
            mSize.setVisibility(View.VISIBLE);
            mSize.setText(EMPhotoMetadataUtils.getSizeInMB(item.size) + "M");
        } else {
            mSize.setVisibility(View.GONE);
        }

        if (item.isVideo()) {
            mOriginalLayout.setVisibility(View.GONE);
        } else if (mSpec.originalable) {
            mOriginalLayout.setVisibility(View.VISIBLE);
        }
    }

    protected void sendBackResult(boolean apply) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_RESULT_BUNDLE, mSelectedCollection.getDataWithBundle());
        intent.putExtra(EXTRA_RESULT_APPLY, apply);
        intent.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
        setResult(Activity.RESULT_OK, intent);
    }

    private boolean assertAddSelection(EMItem item) {
        EMIncapableCause cause = mSelectedCollection.isAcceptable(item);
        EMIncapableCause.handleCause(this, cause);
        return cause == null;
    }
}
