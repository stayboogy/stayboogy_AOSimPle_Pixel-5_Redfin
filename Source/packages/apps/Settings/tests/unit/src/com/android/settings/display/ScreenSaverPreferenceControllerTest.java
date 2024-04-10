/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.display;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ScreenSaverPreferenceControllerTest {
    @Mock
    private Context mContext;
    @Mock
    private Resources mResources;
    @Mock
    private UserManager mUserManager;

    private ScreenSaverPreferenceController mController;

    private final String mPrefKey = "test_screensaver";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(mContext.getResources()).thenReturn(mResources);
        when(mContext.getSystemServiceName(UserManager.class))
                .thenReturn(Context.USER_SERVICE);
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mUserManager.getMainUser()).thenReturn(UserHandle.of(0));
        when(mContext.createContextAsUser(any(), anyInt())).thenReturn(mContext);

        mController = new ScreenSaverPreferenceController(mContext, mPrefKey);
    }

    @Test
    public void isAvailable_dreamsEnabledForAllUsers_shouldBeTrueForMainUser() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsSupported)).thenReturn(true);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsOnlyEnabledForDockUser))
                .thenReturn(false);
        when(mUserManager.isUserForeground()).thenReturn(true);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void isAvailable_dreamsEnabledForAllUsers_shouldBeTrueForNonMainUser() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsSupported)).thenReturn(true);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsOnlyEnabledForDockUser))
                .thenReturn(false);
        when(mUserManager.isUserForeground()).thenReturn(false);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void isAvailable_dreamsDisabled_shouldBeFalseForMainUser() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsSupported)).thenReturn(false);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsOnlyEnabledForDockUser))
                .thenReturn(false);
        when(mUserManager.isUserForeground()).thenReturn(true);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_dreamsOnlyEnabledForDockUser_shouldBeTrueForMainUser() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsSupported)).thenReturn(true);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsOnlyEnabledForDockUser))
                .thenReturn(true);
        when(mUserManager.isUserForeground()).thenReturn(true);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void isAvailable_dreamsOnlyEnabledForDockUser_shouldBeFalseForNonMainUser() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsSupported)).thenReturn(true);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsOnlyEnabledForDockUser))
                .thenReturn(true);
        when(mUserManager.isUserForeground()).thenReturn(false);
        assertFalse(mController.isAvailable());
    }
}
