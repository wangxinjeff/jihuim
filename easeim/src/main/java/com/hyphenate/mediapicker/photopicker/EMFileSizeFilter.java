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
package com.hyphenate.mediapicker.photopicker;

import android.content.Context;
import android.graphics.Point;

import com.hyphenate.mediapicker.EMMimeType;
import com.hyphenate.mediapicker.filter.EMFilter;
import com.hyphenate.mediapicker.internal.entity.EMIncapableCause;
import com.hyphenate.mediapicker.internal.entity.EMItem;
import com.hyphenate.mediapicker.internal.utils.EMPhotoMetadataUtils;

import java.util.HashSet;
import java.util.Set;

import com.hyphenate.easeim.R;

/**
 * 文件过滤
 * @author : BaoZhou
 * @date : 2018/7/18 20:05
 */
public class EMFileSizeFilter extends EMFilter {

    private int mMaxWidth;
    private int mMaxHeight;
    private int mMaxVideoSize;
    private int mMaxVideoLength;
    private int mMaxImageSize;

    public EMFileSizeFilter(int maxWidth, int maxHeight, int maxVideoSizeInBytes, int maxImageSize, int maxVideoLength) {
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
        mMaxVideoSize = maxVideoSizeInBytes;
        mMaxVideoLength = maxVideoLength;
        mMaxImageSize = maxImageSize;
    }

    @Override
    public Set<EMMimeType> constraintTypes() {
        return new HashSet<EMMimeType>() {{
        }};
    }

    @Override
    public EMIncapableCause filter(Context context, EMItem item) {
        if (item.duration > 0) {
            if (item.duration > mMaxVideoLength) {
                return new EMIncapableCause(EMIncapableCause.DIALOG, context.getString(R.string.em_error_video, mMaxVideoLength / 1000));
            }
            if (item.size > mMaxVideoSize) {
                return new EMIncapableCause(EMIncapableCause.DIALOG,  context.getString(R.string.em_error_size,  mMaxVideoSize / EMFilter.K / EMFilter.K));
            }

        } else if (item.duration == 0) {
            Point size = EMPhotoMetadataUtils.getBitmapBound(context.getContentResolver(), item.getContentUri());
            if (item.size > mMaxImageSize) {
                return new EMIncapableCause(EMIncapableCause.DIALOG,  context.getString(R.string.em_error_size,  mMaxImageSize / EMFilter.K / EMFilter.K));
            }
//            if (size.x > mMaxWidth || size.y > mMaxHeight || item.size > mMaxImageSize) {
//                return new EMIncapableCause(EMIncapableCause.DIALOG, context.getString(R.string.em_error_gif,mMaxHeight,mMaxWidth
//                        , String.valueOf(EMPhotoMetadataUtils.getSizeInMB(mMaxImageSize))));
//            }

        }

        return null;
    }

}
