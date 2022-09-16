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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hyphenate.easeim.R;
import com.hyphenate.easeui.player.EasyVideoCallback;
import com.hyphenate.easeui.player.EasyVideoPlayer;
import com.hyphenate.mediapicker.internal.entity.EMItem;
import com.hyphenate.mediapicker.internal.entity.EMSelectionSpec;
import com.hyphenate.mediapicker.internal.utils.EMPhotoMetadataUtils;
import com.hyphenate.mediapicker.listener.EMOnFragmentInteractionListener;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;

public class EMPreviewItemFragment extends Fragment implements EasyVideoCallback {

    private static final String ARGS_ITEM = "args_item";
    private EMOnFragmentInteractionListener mListener;
    private EasyVideoPlayer evpPlayer;
    private Uri uri;

    public static EMPreviewItemFragment newInstance(EMItem item) {
        EMPreviewItemFragment fragment = new EMPreviewItemFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARGS_ITEM, item);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.em_fragment_preview_item, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final EMItem item = getArguments().getParcelable(ARGS_ITEM);
        if (item == null) {
            return;
        }

        View videoPlayButton = view.findViewById(R.id.video_play_button);
        if (item.isVideo()) {
            uri = item.getContentUri();
            evpPlayer = view.findViewById(R.id.evp_player);
            evpPlayer.setAutoPlay(true);
            evpPlayer.setCallback(this);

            videoPlayButton.setVisibility(View.VISIBLE);
            videoPlayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    Intent intent = new Intent(Intent.ACTION_VIEW);
//                    intent.setDataAndType(item.uri, "video/*");
//                    try {
//                        startActivity(intent);
//                    } catch (ActivityNotFoundException e) {
//                        Toast.makeText(getContext(), R.string.em_error_no_video_activity, Toast.LENGTH_SHORT).show();
//                    }
                    if(uri != null) {
                        evpPlayer.setVisibility(View.VISIBLE);
                        evpPlayer.setSource(uri);
                    }
                }
            });
        } else {
            videoPlayButton.setVisibility(View.GONE);
        }

        ImageViewTouch image = (ImageViewTouch) view.findViewById(R.id.image_view);
        image.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);

        image.setSingleTapListener(new ImageViewTouch.OnImageViewTouchSingleTapListener() {
            @Override
            public void onSingleTapConfirmed() {
                if (mListener != null) {
                    mListener.onClick();
                }
            }
        });

        Point size = EMPhotoMetadataUtils.getBitmapSize(item.getContentUri(), getActivity());
        if (item.isGif()) {
            EMSelectionSpec.getInstance().imageEngine.loadGifImage(getContext(), size.x, size.y, image,
                    item.getContentUri());
        } else {
            EMSelectionSpec.getInstance().imageEngine.loadImage(getContext(), size.x, size.y, image,
                    item.getContentUri());
        }
    }

    public void resetView() {
        if (getView() != null) {
            ((ImageViewTouch) getView().findViewById(R.id.image_view)).resetMatrix();
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof EMOnFragmentInteractionListener) {
            mListener = (EMOnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(!isVisibleToUser && evpPlayer != null) {
            evpPlayer.reset();
            evpPlayer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(evpPlayer != null) {
            evpPlayer.pause();
            evpPlayer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(evpPlayer != null) {
            evpPlayer.release();
            evpPlayer = null;
        }
    }

    @Override
    public void onStarted(EasyVideoPlayer player) {

    }

    @Override
    public void onPaused(EasyVideoPlayer player) {

    }

    @Override
    public void onPreparing(EasyVideoPlayer player) {

    }

    @Override
    public void onPrepared(EasyVideoPlayer player) {

    }

    @Override
    public void onBuffering(int percent) {

    }

    @Override
    public void onError(EasyVideoPlayer player, Exception e) {

    }

    @Override
    public void onCompletion(EasyVideoPlayer player) {
        evpPlayer.setVisibility(View.GONE);
    }

    @Override
    public void onClickVideoFrame(EasyVideoPlayer player) {

    }
}
