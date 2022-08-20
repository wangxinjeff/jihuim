/*
 * Copyright (C) 2014 nohana, Inc.
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hyphenate.mediapicker;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StyleRes;
import androidx.fragment.app.Fragment;

import com.hyphenate.mediapicker.engine.EMImageEngine;
import com.hyphenate.mediapicker.engine.impl.EMGlideEngine;
import com.hyphenate.mediapicker.engine.impl.EMPicassoEngine;
import com.hyphenate.mediapicker.filter.EMFilter;
import com.hyphenate.mediapicker.internal.entity.EMCaptureStrategy;
import com.hyphenate.mediapicker.internal.entity.EMSelectionSpec;
import com.hyphenate.mediapicker.listener.EMOnCheckedListener;
import com.hyphenate.mediapicker.listener.EMOnSelectedListener;
import com.hyphenate.mediapicker.ui.EMMatisseActivity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Set;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_BEHIND;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_NOSENSOR;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT;

/**
 * Fluent API for building media select specification.
 */
@SuppressWarnings("unused")
public final class EMSelectionCreator {
    private final EMMatisse mMatisse;
    private final EMSelectionSpec mSelectionSpec;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @IntDef({
            SCREEN_ORIENTATION_UNSPECIFIED,
            SCREEN_ORIENTATION_LANDSCAPE,
            SCREEN_ORIENTATION_PORTRAIT,
            SCREEN_ORIENTATION_USER,
            SCREEN_ORIENTATION_BEHIND,
            SCREEN_ORIENTATION_SENSOR,
            SCREEN_ORIENTATION_NOSENSOR,
            SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            SCREEN_ORIENTATION_SENSOR_PORTRAIT,
            SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            SCREEN_ORIENTATION_REVERSE_PORTRAIT,
            SCREEN_ORIENTATION_FULL_SENSOR,
            SCREEN_ORIENTATION_USER_LANDSCAPE,
            SCREEN_ORIENTATION_USER_PORTRAIT,
            SCREEN_ORIENTATION_FULL_USER,
            SCREEN_ORIENTATION_LOCKED
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface ScreenOrientation {
    }

    /**
     * Constructs a new specification builder on the context.
     *
     * @param matisse   a requester context wrapper.
     * @param mimeTypes MIME type set to select.
     */
    EMSelectionCreator(EMMatisse matisse, @NonNull Set<EMMimeType> mimeTypes, boolean mediaTypeExclusive) {
        mMatisse = matisse;
        mSelectionSpec = EMSelectionSpec.getCleanInstance();
        mSelectionSpec.mimeTypeSet = mimeTypes;
        mSelectionSpec.mediaTypeExclusive = mediaTypeExclusive;
        mSelectionSpec.orientation = SCREEN_ORIENTATION_UNSPECIFIED;
    }

    /**
     * Whether to show only one media type if choosing medias are only images or videos.
     *
     * @param showSingleMediaType whether to show only one media type, either images or videos.
     * @return {@link EMSelectionCreator} for fluent API.
     * @see EMSelectionSpec#onlyShowImages()
     * @see EMSelectionSpec#onlyShowVideos()
     */
    public EMSelectionCreator showSingleMediaType(boolean showSingleMediaType) {
        mSelectionSpec.showSingleMediaType = showSingleMediaType;
        return this;
    }

    /**
     * Theme for media selecting Activity.
     * <p>
     * There are two built-in themes:
     * 1. import com.hyphenate.easeim.R;.style.Matisse_Zhihu;
     * 2. import com.hyphenate.easeim.R;.style.Matisse_Dracula
     * you can define a custom theme derived from the above ones or other themes.
     *
     * @param themeId theme resource id. Default value is import com.hyphenate.easeim.R;.style.Matisse_Zhihu.
     * @return {@link EMSelectionCreator} for fluent API.
     */
    public EMSelectionCreator theme(@StyleRes int themeId) {
        mSelectionSpec.themeId = themeId;
        return this;
    }

    /**
     * Show a auto-increased number or a check mark when user select media.
     *
     * @param countable true for a auto-increased number from 1, false for a check mark. Default
     *                  value is false.
     * @return {@link EMSelectionCreator} for fluent API.
     */
    public EMSelectionCreator countable(boolean countable) {
        mSelectionSpec.countable = countable;
        return this;
    }

    /**
     * Maximum selectable count.
     *
     * @param maxSelectable Maximum selectable count. Default value is 1.
     * @return {@link EMSelectionCreator} for fluent API.
     */
    public EMSelectionCreator maxSelectable(int maxSelectable) {
        if (maxSelectable < 1)
            throw new IllegalArgumentException("maxSelectable must be greater than or equal to one");
        if (mSelectionSpec.maxImageSelectable > 0 || mSelectionSpec.maxVideoSelectable > 0)
            throw new IllegalStateException("already set maxImageSelectable and maxVideoSelectable");
        mSelectionSpec.maxSelectable = maxSelectable;
        return this;
    }

    /**
     * Only useful when {@link EMSelectionSpec#mediaTypeExclusive} set true and you want to set different maximum
     * selectable files for image and video media types.
     *
     * @param maxImageSelectable Maximum selectable count for image.
     * @param maxVideoSelectable Maximum selectable count for video.
     * @return  {@link EMSelectionCreator} for fluent API.
     */
    public EMSelectionCreator maxSelectablePerMediaType(int maxImageSelectable, int maxVideoSelectable) {
        if (maxImageSelectable < 1 || maxVideoSelectable < 1)
            throw new IllegalArgumentException(("max selectable must be greater than or equal to one"));
        mSelectionSpec.maxSelectable = -1;
        mSelectionSpec.maxImageSelectable = maxImageSelectable;
        mSelectionSpec.maxVideoSelectable = maxVideoSelectable;
        return this;
    }

    /**
     * Add filter to filter each selecting item.
     *
     * @param filter {@link EMFilter}
     * @return {@link EMSelectionCreator} for fluent API.
     */
    public EMSelectionCreator addFilter(@NonNull EMFilter filter) {
        if (mSelectionSpec.filters == null) {
            mSelectionSpec.filters = new ArrayList<>();
        }
        if (filter == null) throw new IllegalArgumentException("filter cannot be null");
        mSelectionSpec.filters.add(filter);
        return this;
    }

    /**
     * Determines whether the photo capturing is enabled or not on the media grid view.
     * <p>
     * If this value is set true, photo capturing entry will appear only on All Media's page.
     *
     * @param enable Whether to enable capturing or not. Default value is false;
     * @return {@link EMSelectionCreator} for fluent API.
     */
    public EMSelectionCreator capture(boolean enable) {
        mSelectionSpec.capture = enable;
        return this;
    }

    /**
     * Show a original photo check options.Let users decide whether use original photo after select
     *
     * @param enable Whether to enable original photo or not
     * @return {@link EMSelectionCreator} for fluent API.
     */
    public EMSelectionCreator originalEnable(boolean enable) {
        mSelectionSpec.originalable = enable;
        return this;
    }


    /**
     * Determines Whether to hide top and bottom toolbar in PreView mode ,when user tap the picture
     * @param enable
     * @return {@link EMSelectionCreator} for fluent API.
     */
    public EMSelectionCreator autoHideToolbarOnSingleTap(boolean enable) {
        mSelectionSpec.autoHideToobar = enable;
        return this;
    }

    /**
     * Maximum original size,the unit is MB. Only useful when {link@originalEnable} set true
     *
     * @param size Maximum original size. Default value is Integer.MAX_VALUE
     * @return {@link EMSelectionCreator} for fluent API.
     */
    public EMSelectionCreator maxOriginalSize(int size) {
        mSelectionSpec.originalMaxSize = size;
        return this;
    }

    /**
     * Capture strategy provided for the location to save photos including internal and external
     * storage and also a authority for {@link androidx.core.content.FileProvider}.
     *
     * @param captureStrategy {@link EMCaptureStrategy}, needed only when capturing is enabled.
     * @return {@link EMSelectionCreator} for fluent API.
     */
    public EMSelectionCreator captureStrategy(EMCaptureStrategy captureStrategy) {
        mSelectionSpec.captureStrategy = captureStrategy;
        return this;
    }

    /**
     * Set the desired orientation of this activity.
     *
     * @param orientation An orientation constant as used in {@link ScreenOrientation}.
     *                    Default value is {@link android.content.pm.ActivityInfo#SCREEN_ORIENTATION_PORTRAIT}.
     * @return {@link EMSelectionCreator} for fluent API.
     * @see Activity#setRequestedOrientation(int)
     */
    public EMSelectionCreator restrictOrientation(@ScreenOrientation int orientation) {
        mSelectionSpec.orientation = orientation;
        return this;
    }

    /**
     * Set a fixed span count for the media grid. Same for different screen orientations.
     * <p>
     * This will be ignored when {@link #gridExpectedSize(int)} is set.
     *
     * @param spanCount Requested span count.
     * @return {@link EMSelectionCreator} for fluent API.
     */
    public EMSelectionCreator spanCount(int spanCount) {
        if (spanCount < 1) throw new IllegalArgumentException("spanCount cannot be less than 1");
        mSelectionSpec.spanCount = spanCount;
        return this;
    }

    /**
     * Set expected size for media grid to adapt to different screen sizes. This won't necessarily
     * be applied cause the media grid should fill the view container. The measured media grid's
     * size will be as close to this value as possible.
     *
     * @param size Expected media grid size in pixel.
     * @return {@link EMSelectionCreator} for fluent API.
     */
    public EMSelectionCreator gridExpectedSize(int size) {
        mSelectionSpec.gridExpectedSize = size;
        return this;
    }

    /**
     * Photo thumbnail's scale compared to the View's size. It should be a float value in (0.0,
     * 1.0].
     *
     * @param scale Thumbnail's scale in (0.0, 1.0]. Default value is 0.5.
     * @return {@link EMSelectionCreator} for fluent API.
     */
    public EMSelectionCreator thumbnailScale(float scale) {
        if (scale <= 0f || scale > 1f)
            throw new IllegalArgumentException("Thumbnail scale must be between (0.0, 1.0]");
        mSelectionSpec.thumbnailScale = scale;
        return this;
    }

    /**
     * Provide an image engine.
     * <p>
     * There are two built-in image engines:
     * 1. {@link EMGlideEngine}
     * 2. {@link EMPicassoEngine}
     * And you can implement your own image engine.
     *
     * @param imageEngine {@link EMImageEngine}
     * @return {@link EMSelectionCreator} for fluent API.
     */
    public EMSelectionCreator imageEngine(EMImageEngine imageEngine) {
        mSelectionSpec.imageEngine = imageEngine;
        return this;
    }

    /**
     * Set listener for callback immediately when user select or unselect something.
     * <p>
     * It's a redundant API with {@link EMMatisse#obtainResult(Intent)},
     * we only suggest you to use this API when you need to do something immediately.
     *
     * @param listener {@link EMOnSelectedListener}
     * @return {@link EMSelectionCreator} for fluent API.
     */
    @NonNull
    public EMSelectionCreator setOnSelectedListener(@Nullable EMOnSelectedListener listener) {
        mSelectionSpec.onSelectedListener = listener;
        return this;
    }

    /**
     * Set listener for callback immediately when user check or uncheck original.
     *
     * @param listener {@link EMOnSelectedListener}
     * @return {@link EMSelectionCreator} for fluent API.
     */
    public EMSelectionCreator setOnCheckedListener(@Nullable EMOnCheckedListener listener) {
        mSelectionSpec.onCheckedListener = listener;
        return this;
    }

    /**
     * Start to select media and wait for result.
     *
     * @param requestCode Identity of the request Activity or Fragment.
     */
    public void forResult(int requestCode) {
        Activity activity = mMatisse.getActivity();
        if (activity == null) {
            return;
        }

        Intent intent = new Intent(activity, EMMatisseActivity.class);

        Fragment fragment = mMatisse.getFragment();
        if (fragment != null) {
            fragment.startActivityForResult(intent, requestCode);
        } else {
            activity.startActivityForResult(intent, requestCode);
        }
    }

    public EMSelectionCreator showPreview(boolean showPreview) {
        mSelectionSpec.showPreview = showPreview;
        return this;
    }
}
