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
package com.hyphenate.mediapicker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.hyphenate.mediapicker.ui.EMMatisseActivity;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;

/**
 * Entry for Matisse's media selection.
 */
public final class EMMatisse {

    private final WeakReference<Activity> mContext;
    private final WeakReference<Fragment> mFragment;

    private EMMatisse(Activity activity) {
        this(activity, null);
    }

    private EMMatisse(Fragment fragment) {
        this(fragment.getActivity(), fragment);
    }

    private EMMatisse(Activity activity, Fragment fragment) {
        mContext = new WeakReference<>(activity);
        mFragment = new WeakReference<>(fragment);
    }

    /**
     * Start Matisse from an Activity.
     * <p>
     * This Activity's {@link Activity#onActivityResult(int, int, Intent)} will be called when user
     * finishes selecting.
     *
     * @param activity Activity instance.
     * @return Matisse instance.
     */
    public static EMMatisse from(Activity activity) {
        return new EMMatisse(activity);
    }

    /**
     * Start Matisse from a Fragment.
     * <p>
     * This Fragment's {@link Fragment#onActivityResult(int, int, Intent)} will be called when user
     * finishes selecting.
     *
     * @param fragment Fragment instance.
     * @return Matisse instance.
     */
    public static EMMatisse from(Fragment fragment) {
        return new EMMatisse(fragment);
    }

    /**
     * Obtain user selected media' {@link Uri} list in the starting Activity or Fragment.
     *
     * @param data Intent passed by {@link Activity#onActivityResult(int, int, Intent)} or
     *             {@link Fragment#onActivityResult(int, int, Intent)}.
     * @return User selected media' {@link Uri} list.
     */
    public static List<Uri> obtainResult(Intent data) {
        return data.getParcelableArrayListExtra(EMMatisseActivity.EXTRA_RESULT_SELECTION);
    }

    /**
     * Obtain user selected media path list in the starting Activity or Fragment.
     *
     * @param data Intent passed by {@link Activity#onActivityResult(int, int, Intent)} or
     *             {@link Fragment#onActivityResult(int, int, Intent)}.
     * @return User selected media path list.
     */
    public static List<String> obtainPathResult(Intent data) {
        return data.getStringArrayListExtra(EMMatisseActivity.EXTRA_RESULT_SELECTION_PATH);
    }

    /**
     * Obtain state whether user decide to use selected media in original
     *
     * @param data Intent passed by {@link Activity#onActivityResult(int, int, Intent)} or
     *             {@link Fragment#onActivityResult(int, int, Intent)}.
     * @return Whether use original photo
     */
    public static boolean obtainOriginalState(Intent data) {
        return data.getBooleanExtra(EMMatisseActivity.EXTRA_RESULT_ORIGINAL_ENABLE, false);
    }

    /**
     * MIME types the selection constrains on.
     * <p>
     * Types not included in the set will still be shown in the grid but can't be chosen.
     *
     * @param EMMimeTypes MIME types set user can choose from.
     * @return {@link EMSelectionCreator} to build select specifications.
     * @see EMMimeType
     * @see EMSelectionCreator
     */
    public EMSelectionCreator choose(Set<EMMimeType> EMMimeTypes) {
        return this.choose(EMMimeTypes, false);
    }

    /**
     * MIME types the selection constrains on.
     * <p>
     * Types not included in the set will still be shown in the grid but can't be chosen.
     *
     * @param EMMimeTypes          MIME types set user can choose from.
     * @param mediaTypeExclusive Whether can choose images and videos at the same time during one single choosing
     *                           process. true corresponds to not being able to choose images and videos at the same
     *                           time, and false corresponds to being able to do this.
     * @return {@link EMSelectionCreator} to build select specifications.
     * @see EMMimeType
     * @see EMSelectionCreator
     */
    public EMSelectionCreator choose(Set<EMMimeType> EMMimeTypes, boolean mediaTypeExclusive) {
        return new EMSelectionCreator(this, EMMimeTypes, mediaTypeExclusive);
    }

    @Nullable
    Activity getActivity() {
        return mContext.get();
    }

    @Nullable
    Fragment getFragment() {
        return mFragment != null ? mFragment.get() : null;
    }

}
