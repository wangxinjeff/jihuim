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
package com.hyphenate.mediapicker.filter;

import android.content.Context;

import com.hyphenate.mediapicker.EMMimeType;
import com.hyphenate.mediapicker.EMSelectionCreator;
import com.hyphenate.mediapicker.internal.entity.EMIncapableCause;
import com.hyphenate.mediapicker.internal.entity.EMItem;

import java.util.Set;

/**
 * Filter for choosing a {@link EMItem}. You can add multiple Filters through
 * {@link EMSelectionCreator#addFilter(EMFilter)}.
 */
@SuppressWarnings("unused")
public abstract class EMFilter {
    /**
     * Convenient constant for a minimum value.
     */
    public static final int MIN = 0;
    /**
     * Convenient constant for a maximum value.
     */
    public static final int MAX = Integer.MAX_VALUE;
    /**
     * Convenient constant for 1024.
     */
    public static final int K = 1024;

    /**
     * Against what mime types this filter applies.
     */
    protected abstract Set<EMMimeType> constraintTypes();

    /**
     * Invoked for filtering each item.
     *
     * @return null if selectable, {@link EMIncapableCause} if not selectable.
     */
    public abstract EMIncapableCause filter(Context context, EMItem item);

    /**
     * Whether an {@link EMItem} need filtering.
     */
    protected boolean needFiltering(Context context, EMItem item) {
        for (EMMimeType type : constraintTypes()) {
            if (type.checkType(context.getContentResolver(), item.getContentUri())) {
                return true;
            }
        }
        return false;
    }
}
