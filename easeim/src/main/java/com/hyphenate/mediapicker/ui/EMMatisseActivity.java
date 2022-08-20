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
package com.hyphenate.mediapicker.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.support.v4.app.Fragment;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.section.base.BaseInitActivity;
import com.hyphenate.mediapicker.internal.entity.EMAlbum;
import com.hyphenate.mediapicker.internal.entity.EMItem;
import com.hyphenate.mediapicker.internal.entity.EMSelectionSpec;
import com.hyphenate.mediapicker.internal.model.EMAlbumCollection;
import com.hyphenate.mediapicker.internal.model.EMSelectedItemCollection;
import com.hyphenate.mediapicker.internal.ui.EMAlbumPreviewActivityEM;
import com.hyphenate.mediapicker.internal.ui.EMBasePreviewActivity;
import com.hyphenate.mediapicker.internal.ui.EMMediaSelectionFragment;
import com.hyphenate.mediapicker.internal.ui.EMSelectedPreviewActivityEM;
import com.hyphenate.mediapicker.internal.ui.adapter.EMAlbumMediaAdapterEM;
import com.hyphenate.mediapicker.internal.ui.adapter.EMAlbumsAdapter;
import com.hyphenate.mediapicker.internal.ui.widget.EMAlbumsSpinner;
import com.hyphenate.mediapicker.internal.ui.widget.EMCheckRadioView;
import com.hyphenate.mediapicker.internal.ui.widget.EMIncapableDialog;
import com.hyphenate.mediapicker.internal.utils.EMMediaStoreCompat;
import com.hyphenate.mediapicker.internal.utils.EMPathUtils;
import com.hyphenate.mediapicker.internal.utils.EMPhotoMetadataUtils;
import com.hyphenate.mediapicker.internal.utils.EMSingleMediaScanner;

import java.util.ArrayList;

/**
 * Main Activity to display albums and media content (images/videos) in each album
 * and also support media selecting operations.
 */
public class EMMatisseActivity extends BaseInitActivity implements
        EMAlbumCollection.AlbumCallbacks, AdapterView.OnItemSelectedListener,
        EMMediaSelectionFragment.SelectionProvider, View.OnClickListener,
        EMAlbumMediaAdapterEM.CheckStateListener, EMAlbumMediaAdapterEM.OnMediaClickListener,
        EMAlbumMediaAdapterEM.OnPhotoCapture {

    public static final String EXTRA_RESULT_SELECTION = "extra_result_selection";
    public static final String EXTRA_RESULT_SELECTION_PATH = "extra_result_selection_path";
    public static final String EXTRA_RESULT_ORIGINAL_ENABLE = "extra_result_original_enable";
    private static final int REQUEST_CODE_PREVIEW = 23;
    private static final int REQUEST_CODE_CAPTURE = 24;
    public static final String CHECK_STATE = "checkState";
    private final EMAlbumCollection mEMAlbumCollection = new EMAlbumCollection();
    private EMMediaStoreCompat mEMMediaStoreCompat;
    private EMSelectedItemCollection mSelectedCollection = new EMSelectedItemCollection(this);
    private EMSelectionSpec mSpec;

    private EMAlbumsSpinner mAlbumsSpinner;
    private EMAlbumsAdapter mEMAlbumsAdapter;
    private TextView mButtonPreview;
    private TextView mButtonApply;
    private View mContainer;
    private View mEmptyView;
    private TextView selectedAlbum;

    private LinearLayout mOriginalLayout;
    private EMCheckRadioView mOriginal;
    private boolean mOriginalEnable;
    private ImageView iconBack;

    @Override
    protected int getLayoutId() {
        mSpec = EMSelectionSpec.getInstance();
        setTheme(mSpec.themeId);
        return R.layout.em_activity_matisse;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        if (!mSpec.hasInited) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        setFitSystemForTheme(true, R.color.theme_bg_color);
        setStatusBarTextColor(true);

        if (mSpec.needOrientationRestriction()) {
            setRequestedOrientation(mSpec.orientation);
        }

        if (mSpec.capture) {
            mEMMediaStoreCompat = new EMMediaStoreCompat(this);
            if (mSpec.EMCaptureStrategy == null)
                throw new RuntimeException("Don't forget to set CaptureStrategy.");
            mEMMediaStoreCompat.setCaptureStrategy(mSpec.EMCaptureStrategy);
        }

        selectedAlbum = findViewById(R.id.selected_album);
        mButtonPreview = (TextView) findViewById(R.id.button_preview);
        mButtonApply = (TextView) findViewById(R.id.button_apply);
        mButtonPreview.setOnClickListener(this);
        mButtonApply.setOnClickListener(this);
        mContainer = findViewById(R.id.container);
        mEmptyView = findViewById(R.id.empty_view);
        mOriginalLayout = findViewById(R.id.originalLayout);
        mOriginal = findViewById(R.id.original);
        mOriginalLayout.setOnClickListener(this);

        mSelectedCollection.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mOriginalEnable = savedInstanceState.getBoolean(CHECK_STATE);
        }
        updateBottomToolbar();

        mEMAlbumsAdapter = new EMAlbumsAdapter(this, null, false);
        mAlbumsSpinner = new EMAlbumsSpinner(this);
        mAlbumsSpinner.setOnItemSelectedListener(this);
        mAlbumsSpinner.setSelectedTextView(selectedAlbum);
        mAlbumsSpinner.setPopupAnchorView(findViewById(R.id.toolbar));
        mAlbumsSpinner.setAdapter(mEMAlbumsAdapter);
        mEMAlbumCollection.onCreate(this, this);
        mEMAlbumCollection.onRestoreInstanceState(savedInstanceState);
        mEMAlbumCollection.loadAlbums();
        
        iconBack = findViewById(R.id.icon_back);
        iconBack.setOnClickListener(this);
        
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mSelectedCollection.onSaveInstanceState(outState);
        mEMAlbumCollection.onSaveInstanceState(outState);
        outState.putBoolean("checkState", mOriginalEnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEMAlbumCollection.onDestroy();
        mSpec.EMOnCheckedListener = null;
        mSpec.EMOnSelectedListener = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;

        if (requestCode == REQUEST_CODE_PREVIEW) {
            Bundle resultBundle = data.getBundleExtra(EMBasePreviewActivity.EXTRA_RESULT_BUNDLE);
            ArrayList<EMItem> selected = resultBundle.getParcelableArrayList(EMSelectedItemCollection.STATE_SELECTION);
            mOriginalEnable = data.getBooleanExtra(EMBasePreviewActivity.EXTRA_RESULT_ORIGINAL_ENABLE, false);
            int collectionType = resultBundle.getInt(EMSelectedItemCollection.STATE_COLLECTION_TYPE,
                    EMSelectedItemCollection.COLLECTION_UNDEFINED);
            if (data.getBooleanExtra(EMBasePreviewActivity.EXTRA_RESULT_APPLY, false)) {
                Intent result = new Intent();
                ArrayList<Uri> selectedUris = new ArrayList<>();
                ArrayList<String> selectedPaths = new ArrayList<>();
                if (selected != null) {
                    for (EMItem item : selected) {
                        selectedUris.add(item.getContentUri());
                        selectedPaths.add(EMPathUtils.getPath(this, item.getContentUri()));
                    }
                }
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris);
                result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths);
                result.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
                setResult(RESULT_OK, result);
                finish();
            } else {
                mSelectedCollection.overwrite(selected, collectionType);
                Fragment mediaSelectionFragment = getSupportFragmentManager().findFragmentByTag(
                        EMMediaSelectionFragment.class.getSimpleName());
                if (mediaSelectionFragment instanceof EMMediaSelectionFragment) {
                    ((EMMediaSelectionFragment) mediaSelectionFragment).refreshMediaGrid();
                }
                updateBottomToolbar();
            }
        } else if (requestCode == REQUEST_CODE_CAPTURE) {
            // Just pass the data back to previous calling Activity.
            Uri contentUri = mEMMediaStoreCompat.getCurrentPhotoUri();
            String path = mEMMediaStoreCompat.getCurrentPhotoPath();
            ArrayList<Uri> selected = new ArrayList<>();
            selected.add(contentUri);
            ArrayList<String> selectedPath = new ArrayList<>();
            selectedPath.add(path);
            Intent result = new Intent();
            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selected);
            result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPath);
            setResult(RESULT_OK, result);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                EMMatisseActivity.this.revokeUriPermission(contentUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            new EMSingleMediaScanner(this.getApplicationContext(), path, new EMSingleMediaScanner.ScanListener() {
                @Override public void onScanFinish() {
                    Log.i("SingleMediaScanner", "scan finish!");
                }
            });
            finish();
        }
    }

    private void updateBottomToolbar() {

        int selectedCount = mSelectedCollection.count();
        if (selectedCount == 0) {
            mButtonPreview.setEnabled(false);
            mButtonApply.setEnabled(false);
            mButtonApply.setText(getString(R.string.em_button_apply_default));
        } else if (selectedCount == 1 && mSpec.singleSelectionModeEnabled()) {
            mButtonPreview.setEnabled(true);
            mButtonApply.setText(R.string.em_button_apply_default);
            mButtonApply.setEnabled(true);
        } else {
            mButtonPreview.setEnabled(true);
            mButtonApply.setEnabled(true);
            mButtonApply.setText(getString(R.string.em_button_apply, selectedCount));
        }


        if (mSpec.originalable) {
            mOriginalLayout.setVisibility(View.VISIBLE);
            updateOriginalState();
        } else {
            mOriginalLayout.setVisibility(View.INVISIBLE);
        }


    }


    private void updateOriginalState() {
        mOriginal.setChecked(mOriginalEnable);
        if (countOverMaxSize() > 0) {

            if (mOriginalEnable) {
                EMIncapableDialog incapableDialog = EMIncapableDialog.newInstance("",
                        getString(R.string.em_error_over_original_size, mSpec.originalMaxSize));
                incapableDialog.show(getSupportFragmentManager(),
                        EMIncapableDialog.class.getName());

                mOriginal.setChecked(false);
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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_preview) {
            Intent intent = new Intent(this, EMSelectedPreviewActivityEM.class);
            intent.putExtra(EMBasePreviewActivity.EXTRA_DEFAULT_BUNDLE, mSelectedCollection.getDataWithBundle());
            intent.putExtra(EMBasePreviewActivity.EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
            startActivityForResult(intent, REQUEST_CODE_PREVIEW);
        } else if (v.getId() == R.id.button_apply) {
            Intent result = new Intent();
            ArrayList<Uri> selectedUris = (ArrayList<Uri>) mSelectedCollection.asListOfUri();
            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris);
            ArrayList<String> selectedPaths = (ArrayList<String>) mSelectedCollection.asListOfString();
            result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths);
            result.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
            setResult(RESULT_OK, result);
            finish();
        } else if (v.getId() == R.id.originalLayout) {
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

            if (mSpec.EMOnCheckedListener != null) {
                mSpec.EMOnCheckedListener.onCheck(mOriginalEnable);
            }
        } else if (v.getId() == R.id.icon_back){
            finish();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mEMAlbumCollection.setStateCurrentSelection(position);
        mEMAlbumsAdapter.getCursor().moveToPosition(position);
        EMAlbum album = EMAlbum.valueOf(mEMAlbumsAdapter.getCursor());
        if (album.isAll() && EMSelectionSpec.getInstance().capture) {
            album.addCaptureCount();
        }
        onAlbumSelected(album);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onAlbumLoad(final Cursor cursor) {
        mEMAlbumsAdapter.swapCursor(cursor);
        // select default album.
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            @Override
            public void run() {
                cursor.moveToPosition(mEMAlbumCollection.getCurrentSelection());
                mAlbumsSpinner.setSelection(EMMatisseActivity.this,
                        mEMAlbumCollection.getCurrentSelection());
                EMAlbum album = EMAlbum.valueOf(cursor);
                if (album.isAll() && EMSelectionSpec.getInstance().capture) {
                    album.addCaptureCount();
                }
                onAlbumSelected(album);
            }
        });
    }

    @Override
    public void onAlbumReset() {
        mEMAlbumsAdapter.swapCursor(null);
    }

    private void onAlbumSelected(EMAlbum album) {
        if (album.isAll() && album.isEmpty()) {
            mContainer.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mContainer.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            Fragment fragment = EMMediaSelectionFragment.newInstance(album);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment, EMMediaSelectionFragment.class.getSimpleName())
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public void onUpdate() {
        // notify bottom toolbar that check state changed.
        updateBottomToolbar();

        if (mSpec.EMOnSelectedListener != null) {
            mSpec.EMOnSelectedListener.onSelected(
                    mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString());
        }
    }

    @Override
    public void onMediaClick(EMAlbum album, EMItem item, int adapterPosition) {
        Intent intent = new Intent(this, EMAlbumPreviewActivityEM.class);
        intent.putExtra(EMAlbumPreviewActivityEM.EXTRA_ALBUM, album);
        intent.putExtra(EMAlbumPreviewActivityEM.EXTRA_ITEM, item);
        intent.putExtra(EMBasePreviewActivity.EXTRA_DEFAULT_BUNDLE, mSelectedCollection.getDataWithBundle());
        intent.putExtra(EMBasePreviewActivity.EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
        startActivityForResult(intent, REQUEST_CODE_PREVIEW);
    }

    @Override
    public EMSelectedItemCollection provideSelectedItemCollection() {
        return mSelectedCollection;
    }

    @Override
    public void capture() {
        if (mEMMediaStoreCompat != null) {
            mEMMediaStoreCompat.dispatchCaptureIntent(this, REQUEST_CODE_CAPTURE);
        }
    }
    
}
