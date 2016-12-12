/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.afwsamples.testdpc.common;

import android.annotation.TargetApi;
import android.content.RestrictionEntry;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

/**
 * Ported from {@link android.content.RestrictionsManager}.
 */
public class RestrictionManagerCompat {
    private static final String TAG = "RestrictionManager";

    // These keys are used inside TesDPC only. We never save them to DevicePolicyManager
    public static final String CHOICE_SELECTED_VALUE = "testdpc_arg_choice_selected_value";
    public static final String CHOICE_ENTRIES = "testdpc_arg_choice_entries";
    public static final String CHOICE_VALUES = "testdpc_arg_choice_values";


    /**
     * Converts a list of restrictions to the corresponding bundle, using the following mapping:
     * <table>
     *     <tr><th>RestrictionEntry</th><th>Bundle</th></tr>
     *     <tr><td>{@link RestrictionEntry#TYPE_BOOLEAN}</td><td>{@link Bundle#putBoolean}</td></tr>
     *     <tr><td>{@link RestrictionEntry#TYPE_CHOICE},
     *     {@link RestrictionEntry#TYPE_MULTI_SELECT}</td>
     *     <td>{@link Bundle#putStringArray}</td></tr>
     *     <tr><td>{@link RestrictionEntry#TYPE_INTEGER}</td><td>{@link Bundle#putInt}</td></tr>
     *     <tr><td>{@link RestrictionEntry#TYPE_STRING}</td><td>{@link Bundle#putString}</td></tr>
     *     <tr><td>{@link RestrictionEntry#TYPE_BUNDLE}</td><td>{@link Bundle#putBundle}</td></tr>
     *     <tr><td>{@link RestrictionEntry#TYPE_BUNDLE_ARRAY}</td>
     *     <td>{@link Bundle#putParcelableArray}</td></tr>
     * </table>
     * TYPE_BUNDLE and TYPE_BUNDLE_ARRAY are supported from api level 23 onwards.
     * @param entries list of restrictions
     * @param saveChoiceDataForDialog if true, then Choice restriction will be converted into Bundle with entries and values
     */
    public static Bundle convertRestrictionsToBundle(List<RestrictionEntry> entries, boolean saveChoiceDataForDialog) {
        final Bundle bundle = new Bundle();
        for (RestrictionEntry entry : entries) {
            addRestrictionToBundle(bundle, entry, saveChoiceDataForDialog);
        }
        return bundle;
    }

    private static Bundle addRestrictionToBundle(Bundle bundle, RestrictionEntry entry, boolean saveChoiceDataForDialog) {
        switch (entry.getType()) {
            case RestrictionEntry.TYPE_BOOLEAN:
                bundle.putBoolean(entry.getKey(), entry.getSelectedState());
                break;
            case RestrictionEntry.TYPE_MULTI_SELECT:
                bundle.putStringArray(entry.getKey(), entry.getAllSelectedStrings());
                break;
            case RestrictionEntry.TYPE_INTEGER:
                bundle.putInt(entry.getKey(), entry.getIntValue());
                break;
            case RestrictionEntry.TYPE_STRING:
                // UI uses value to find restriction type.
                // If string restrictions has null value, it will be displayed as boolean
                // To avoid this we should set empty string as value
                String value = entry.getSelectedString();
                if (value == null) {
                    value = "";
                }
                bundle.putString(entry.getKey(), value);
                break;
            case RestrictionEntry.TYPE_NULL:
                bundle.putString(entry.getKey(), entry.getSelectedString());
                break;
            case RestrictionEntry.TYPE_BUNDLE:
                addBundleRestrictionToBundle(bundle, entry, saveChoiceDataForDialog);
                break;
            case RestrictionEntry.TYPE_BUNDLE_ARRAY:
                addBundleArrayRestrictionToBundle(bundle, entry, saveChoiceDataForDialog);
                break;
            case RestrictionEntry.TYPE_CHOICE:
                // For UI we are adding entries and values of Choice restriction
                // For DevicePolicyManager we are saving selected string value only
                if (saveChoiceDataForDialog) {
                    bundle.putBundle(entry.getKey(), convertChoiceRestrictionToBundle(entry));
                } else {
                    bundle.putString(entry.getKey(), entry.getSelectedString());
                }
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported restrictionEntry type: " + entry.getType());
        }
        return bundle;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static void addBundleRestrictionToBundle(Bundle bundle, RestrictionEntry entry, boolean saveChoiceDataForDialog) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            RestrictionEntry[] restrictions = entry.getRestrictions();
            Bundle childBundle = convertRestrictionsToBundle(Arrays.asList(restrictions), saveChoiceDataForDialog);
            bundle.putBundle(entry.getKey(), childBundle);
        } else {
            Log.w(TAG, "addBundleRestrictionToBundle is called in pre-M");
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static void addBundleArrayRestrictionToBundle(Bundle bundle, RestrictionEntry entry, boolean saveChoiceDataForDialog) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            RestrictionEntry[] bundleRestrictionArray = entry.getRestrictions();
            Bundle[] bundleArray = new Bundle[bundleRestrictionArray.length];
            for (int i = 0; i < bundleRestrictionArray.length; i++) {
                RestrictionEntry[] bundleRestrictions =
                        bundleRestrictionArray[i].getRestrictions();
                if (bundleRestrictions == null) {
                    // Non-bundle entry found in bundle array.
                    Log.w(TAG, "addRestrictionToBundle: " +
                            "Non-bundle entry found in bundle array");
                    bundleArray[i] = new Bundle();
                } else {
                    bundleArray[i] = convertRestrictionsToBundle(Arrays.asList(
                            bundleRestrictions), saveChoiceDataForDialog);
                }
            }
            bundle.putParcelableArray(entry.getKey(), bundleArray);
        } else {
            Log.w(TAG, "addBundleArrayRestrictionToBundle is called in pre-M");
        }
    }

    /*
     * Saves Choice restriction data to Bundle for UI
     * @param restrictionEntry Choice restriction entry
     */
    public static Bundle convertChoiceRestrictionToBundle(RestrictionEntry restrictionEntry) {
        Bundle choiceData = new Bundle();

        if (restrictionEntry != null) {
            choiceData.putString(CHOICE_SELECTED_VALUE, restrictionEntry.getSelectedString());
            choiceData.putStringArray(CHOICE_ENTRIES, restrictionEntry.getChoiceEntries());
            choiceData.putStringArray(CHOICE_VALUES, restrictionEntry.getChoiceValues());
        }

        return choiceData;
    }
}
