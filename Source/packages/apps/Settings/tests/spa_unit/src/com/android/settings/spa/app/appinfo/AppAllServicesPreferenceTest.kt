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

package com.android.settings.spa.app.appinfo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ResolveInfoFlags
import android.content.pm.ResolveInfo
import android.content.res.Resources
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spa.testutils.delay
import com.android.settingslib.spa.testutils.waitUntilExists
import com.android.settingslib.spaprivileged.model.app.userHandle
import com.android.settingslib.spaprivileged.model.app.userId
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.eq
import org.mockito.Mockito.verify
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class AppAllServicesPreferenceTest {
    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var packageManager: PackageManager

    @Mock
    private lateinit var resources: Resources

    @Before
    fun setUp() {
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(packageManager.getResourcesForApplication(APP)).thenReturn(resources)
        doThrow(Resources.NotFoundException()).`when`(resources).getString(anyInt())
    }

    private fun mockResolveActivityAsUser(resolveInfo: ResolveInfo?) {
        whenever(
            packageManager.resolveActivityAsUser(any(), any<ResolveInfoFlags>(), eq(APP.userId))
        ).thenReturn(resolveInfo)
    }

    @Test
    fun callResolveActivityAsUser_withIntent() {
        mockResolveActivityAsUser(null)

        setContent()

        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify(packageManager).resolveActivityAsUser(
            intentCaptor.capture(), any<ResolveInfoFlags>(), eq(APP.userId)
        )
        val intent = intentCaptor.value
        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW_APP_FEATURES)
        assertThat(intent.`package`).isEqualTo(PACKAGE_NAME)
    }

    @Test
    fun noResolveInfo_notDisplayed() {
        mockResolveActivityAsUser(null)

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun noAllServicesActivity_notDisplayed() {
        mockResolveActivityAsUser(ResolveInfo())

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun hasAllServicesActivity_displayed() {
        mockResolveActivityAsUser(RESOLVE_INFO)

        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.app_info_all_services_label))
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun hasSummary() {
        mockResolveActivityAsUser(RESOLVE_INFO)
        doReturn(SUMMARY).`when`(resources).getString(SUMMARY_RES_ID)

        setContent()

        composeTestRule.waitUntilExists(hasText(SUMMARY))
    }

    @Test
    fun whenClick_startActivity() {
        mockResolveActivityAsUser(RESOLVE_INFO)

        setContent()
        composeTestRule.onRoot().performClick()
        composeTestRule.delay()

        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify(context).startActivityAsUser(intentCaptor.capture(), eq(APP.userHandle))
        val intent = intentCaptor.value
        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW_APP_FEATURES)
        assertThat(intent.component).isEqualTo(ComponentName(PACKAGE_NAME, ACTIVITY_NAME))
    }

    private fun setContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppAllServicesPreference(APP)
            }
        }
        composeTestRule.delay()
    }

    private companion object {
        const val PACKAGE_NAME = "packageName"
        const val ACTIVITY_NAME = "activityName"
        const val UID = 123
        const val SUMMARY_RES_ID = 456
        const val SUMMARY = "summary"

        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UID
        }
        val RESOLVE_INFO = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = PACKAGE_NAME
                name = ACTIVITY_NAME
                metaData = bundleOf(
                    "app_features_preference_summary" to SUMMARY_RES_ID
                )
            }
        }
    }
}
