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

import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.filter.Filter;
import com.zhihu.matisse.internal.entity.IncapableCause;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils;

import java.util.HashSet;
import java.util.Set;

import com.hyphenate.easeim.R;

/**
 * 文件过滤
 * @author : BaoZhou
 * @date : 2018/7/18 20:05
 */
public class FileSizeFilter extends Filter {

    private int mMaxWidth;
    private int mMaxHeight;
    private int mMaxVideoSize;
    private int mMaxVideoLength;
    private int mMaxImageSize;

    public FileSizeFilter(int maxWidth, int maxHeight, int maxVideoSizeInBytes, int maxImageSize, int maxVideoLength) {
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
        mMaxVideoSize = maxVideoSizeInBytes;
        mMaxVideoLength = maxVideoLength;
        mMaxImageSize = maxImageSize;
    }

    @Override
    public Set<MimeType> constraintTypes() {
        return new HashSet<MimeType>() {{
        }};
    }

    @Override
    public IncapableCause filter(Context context, Item item) {
        if (item.duration > 0) {
            if (item.duration > mMaxVideoLength) {
                return new IncapableCause(IncapableCause.DIALOG, context.getString(R.string.em_error_video, mMaxVideoLength / 1000));
            }
            if (item.size > mMaxVideoSize) {
                return new IncapableCause(IncapableCause.DIALOG,  context.getString(R.string.em_error_size,  mMaxVideoSize / Filter.K / Filter.K));
            }

        } else if (item.duration == 0) {
            Point size = PhotoMetadataUtils.getBitmapBound(context.getContentResolver(), item.getContentUri());
            if (item.size > mMaxImageSize) {
                return new IncapableCause(IncapableCause.DIALOG,  context.getString(R.string.em_error_size,  mMaxImageSize / Filter.K / Filter.K));
            }
            if (size.x > mMaxWidth || size.y > mMaxHeight || item.size > mMaxImageSize) {
                return new IncapableCause(IncapableCause.DIALOG, context.getString(R.string.em_error_gif,mMaxHeight,mMaxWidth
                        , String.valueOf(PhotoMetadataUtils.getSizeInMB(mMaxImageSize))));
            }

        }

        return null;
    }

}
