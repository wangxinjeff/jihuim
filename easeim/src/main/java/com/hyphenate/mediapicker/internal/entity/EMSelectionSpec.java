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
package com.hyphenate.mediapicker.internal.entity;

import android.content.pm.ActivityInfo;
import android.support.annotation.StyleRes;


import com.hyphenate.mediapicker.EMMimeType;
import com.hyphenate.easeim.R;
import com.hyphenate.mediapicker.engine.EMImageEngine;
import com.hyphenate.mediapicker.engine.impl.EMGlideEngine;
import com.hyphenate.mediapicker.filter.EMFilter;
import com.hyphenate.mediapicker.listener.EMOnCheckedListener;
import com.hyphenate.mediapicker.listener.EMOnSelectedListener;

import java.util.List;
import java.util.Set;

public final class EMSelectionSpec {

    public Set<EMMimeType> EMMimeTypeSet;
    public boolean mediaTypeExclusive;
    public boolean showSingleMediaType;
    @StyleRes
    public int themeId;
    public int orientation;
    public boolean countable;
    public int maxSelectable;
    public int maxImageSelectable;
    public int maxVideoSelectable;
    public List<EMFilter> filters;
    public boolean capture;
    public EMCaptureStrategy EMCaptureStrategy;
    public int spanCount;
    public int gridExpectedSize;
    public float thumbnailScale;
    public EMImageEngine EMImageEngine;
    public boolean hasInited;
    public EMOnSelectedListener EMOnSelectedListener;
    public boolean originalable;
    public boolean autoHideToobar;
    public int originalMaxSize;
    public EMOnCheckedListener EMOnCheckedListener;
    public boolean showPreview;

    private EMSelectionSpec() {
    }

    public static EMSelectionSpec getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public static EMSelectionSpec getCleanInstance() {
        EMSelectionSpec selectionSpec = getInstance();
        selectionSpec.reset();
        return selectionSpec;
    }

    private void reset() {
        EMMimeTypeSet = null;
        mediaTypeExclusive = true;
        showSingleMediaType = false;
        themeId = R.style.EMMatisse_Dracula;
        orientation = 0;
        countable = false;
        maxSelectable = 1;
        maxImageSelectable = 0;
        maxVideoSelectable = 0;
        filters = null;
        capture = false;
        EMCaptureStrategy = null;
        spanCount = 4;
        gridExpectedSize = 0;
        thumbnailScale = 0.5f;
        EMImageEngine = new EMGlideEngine();
        hasInited = true;
        originalable = false;
        autoHideToobar = false;
        originalMaxSize = Integer.MAX_VALUE;
        showPreview = true;
    }

    public boolean singleSelectionModeEnabled() {
        return !countable && (maxSelectable == 1 || (maxImageSelectable == 1 && maxVideoSelectable == 1));
    }

    public boolean needOrientationRestriction() {
        return orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    }

    public boolean onlyShowImages() {
        return showSingleMediaType && EMMimeType.ofImage().containsAll(EMMimeTypeSet);
    }

    public boolean onlyShowVideos() {
        return showSingleMediaType && EMMimeType.ofVideo().containsAll(EMMimeTypeSet);
    }

    public boolean onlyShowGif() {
        return showSingleMediaType && EMMimeType.ofGif().equals(EMMimeTypeSet);
    }

    private static final class InstanceHolder {
        private static final EMSelectionSpec INSTANCE = new EMSelectionSpec();
    }
}
