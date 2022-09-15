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
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hyphenate.mediapicker.internal.entity;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.fragment.app.FragmentActivity;

import com.hyphenate.mediapicker.internal.ui.widget.EMIncapableDialog;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@SuppressWarnings("unused")
public class EMIncapableCause {
    public static final int TOAST = 0x00;
    public static final int DIALOG = 0x01;
    public static final int NONE = 0x02;

    @Retention(SOURCE)
    @IntDef({TOAST, DIALOG, NONE})
    public @interface Form {
    }

    private int mForm = TOAST;
    private String mTitle;
    private String mMessage;

    public EMIncapableCause(String message) {
        mMessage = message;
    }

    public EMIncapableCause(String title, String message) {
        mTitle = title;
        mMessage = message;
    }

    public EMIncapableCause(@Form int form, String message) {
        mForm = form;
        mMessage = message;
    }

    public EMIncapableCause(@Form int form, String title, String message) {
        mForm = form;
        mTitle = title;
        mMessage = message;
    }

    public static void handleCause(Context context, EMIncapableCause cause) {
        if (cause == null)
            return;
        Toast.makeText(context, cause.mMessage, Toast.LENGTH_SHORT).show();

        switch (cause.mForm) {
            case NONE:
                // do nothing.
                break;
            case DIALOG:
                EMIncapableDialog incapableDialog = EMIncapableDialog.newInstance(cause.mTitle, cause.mMessage);
                incapableDialog.show(((FragmentActivity) context).getSupportFragmentManager(),
                        EMIncapableDialog.class.getName());
                break;
            case TOAST:
            default:
                Toast.makeText(context, cause.mMessage, Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
