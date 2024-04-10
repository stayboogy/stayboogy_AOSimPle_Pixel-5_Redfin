/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.biometrics2.ui.view;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Icon Touch dialog
 */
public class FingerprintEnrollEnrollingIconTouchDialog extends InstrumentedDialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                R.style.Theme_AlertDialog);
        builder.setTitle(R.string.security_settings_fingerprint_enroll_touch_dialog_title)
                .setMessage(R.string.security_settings_fingerprint_enroll_touch_dialog_message)
                .setPositiveButton(
                        R.string.security_settings_fingerprint_enroll_dialog_ok,
                        (dialog, which) -> dialog.dismiss());
        return builder.create();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_FINGERPRINT_ICON_TOUCH;
    }
}
