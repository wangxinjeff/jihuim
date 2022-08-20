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

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.hyphenate.easeim.R;
import com.hyphenate.mediapicker.internal.entity.EMAlbum;
import com.hyphenate.mediapicker.internal.entity.EMItem;
import com.hyphenate.mediapicker.internal.entity.EMSelectionSpec;
import com.hyphenate.mediapicker.internal.model.EMAlbumMediaCollection;
import com.hyphenate.mediapicker.internal.model.EMSelectedItemCollection;
import com.hyphenate.mediapicker.internal.ui.adapter.EMAlbumMediaAdapterEM;
import com.hyphenate.mediapicker.internal.ui.widget.EMMediaGridInset;
import com.hyphenate.mediapicker.internal.utils.EMUIUtils;

public class EMMediaSelectionFragment extends Fragment implements
        EMAlbumMediaCollection.AlbumMediaCallbacks, EMAlbumMediaAdapterEM.CheckStateListener,
        EMAlbumMediaAdapterEM.OnMediaClickListener {

    public static final String EXTRA_ALBUM = "extra_album";

    private final EMAlbumMediaCollection mEMAlbumMediaCollection = new EMAlbumMediaCollection();
    private RecyclerView mRecyclerView;
    private EMAlbumMediaAdapterEM mAdapter;
    private SelectionProvider mSelectionProvider;
    private EMAlbumMediaAdapterEM.CheckStateListener mCheckStateListener;
    private EMAlbumMediaAdapterEM.OnMediaClickListener mOnMediaClickListener;

    public static EMMediaSelectionFragment newInstance(EMAlbum album) {
        EMMediaSelectionFragment fragment = new EMMediaSelectionFragment();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_ALBUM, album);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SelectionProvider) {
            mSelectionProvider = (SelectionProvider) context;
        } else {
            throw new IllegalStateException("Context must implement SelectionProvider.");
        }
        if (context instanceof EMAlbumMediaAdapterEM.CheckStateListener) {
            mCheckStateListener = (EMAlbumMediaAdapterEM.CheckStateListener) context;
        }
        if (context instanceof EMAlbumMediaAdapterEM.OnMediaClickListener) {
            mOnMediaClickListener = (EMAlbumMediaAdapterEM.OnMediaClickListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.em_fragment_media_selection, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        EMAlbum album = getArguments().getParcelable(EXTRA_ALBUM);

        mAdapter = new EMAlbumMediaAdapterEM(getContext(),
                mSelectionProvider.provideSelectedItemCollection(), mRecyclerView);
        mAdapter.registerCheckStateListener(this);
        mAdapter.registerOnMediaClickListener(this);
        mRecyclerView.setHasFixedSize(true);

        int spanCount;
        EMSelectionSpec selectionSpec = EMSelectionSpec.getInstance();
        if (selectionSpec.gridExpectedSize > 0) {
            spanCount = EMUIUtils.spanCount(getContext(), selectionSpec.gridExpectedSize);
        } else {
            spanCount = selectionSpec.spanCount;
        }
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));

        int spacing = getResources().getDimensionPixelSize(R.dimen.media_grid_spacing);
        mRecyclerView.addItemDecoration(new EMMediaGridInset(spanCount, spacing, false));
        mRecyclerView.setAdapter(mAdapter);
        mEMAlbumMediaCollection.onCreate(getActivity(), this);
        mEMAlbumMediaCollection.load(album, selectionSpec.capture);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mEMAlbumMediaCollection.onDestroy();
    }

    public void refreshMediaGrid() {
        mAdapter.notifyDataSetChanged();
    }

    public void refreshSelection() {
        mAdapter.refreshSelection();
    }

    @Override
    public void onAlbumMediaLoad(Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onAlbumMediaReset() {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onUpdate() {
        // notify outer Activity that check state changed
        if (mCheckStateListener != null) {
            mCheckStateListener.onUpdate();
        }
    }

    @Override
    public void onMediaClick(EMAlbum album, EMItem item, int adapterPosition) {
        if (mOnMediaClickListener != null) {
            mOnMediaClickListener.onMediaClick((EMAlbum) getArguments().getParcelable(EXTRA_ALBUM),
                    item, adapterPosition);
        }
    }

    public interface SelectionProvider {
        EMSelectedItemCollection provideSelectedItemCollection();
    }
}
