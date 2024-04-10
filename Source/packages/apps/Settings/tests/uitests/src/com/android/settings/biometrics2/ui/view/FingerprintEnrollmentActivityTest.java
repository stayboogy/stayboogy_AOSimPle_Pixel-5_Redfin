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

package com.android.settings.biometrics2.ui.view;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.hardware.fingerprint.IFingerprintAuthenticatorsRegisteredCallback;
import android.os.UserHandle;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;

import androidx.annotation.NonNull;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.settings.biometrics2.utils.LockScreenUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class FingerprintEnrollmentActivityTest {

    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    private static final String ACTIVITY_CLASS_NAME =
            "com.android.settings.biometrics2.ui.view.FingerprintEnrollmentActivity";
    private static final String EXTRA_IS_SETUP_FLOW = "isSetupFlow";
    private static final String EXTRA_SKIP_INTRO = "skip_intro";
    private static final String EXTRA_SKIP_FIND_SENSOR = "skip_find_sensor";
    private static final String EXTRA_FROM_SETTINGS_SUMMARY = "from_settings_summary";
    private static final String EXTRA_PAGE_TRANSITION_TYPE = "page_transition_type";
    private static final String EXTRA_KEY_GK_PW_HANDLE = "gk_pw_handle";
    private static final String TEST_PIN = "1234";

    private static final String DO_IT_LATER = "Do it later";

    private static final String UDFPS_ENROLLING_TITLE = "Touch & hold the fingerprint sensor";
    private static final String SFPS_ENROLLING_TITLE =
            "Lift, then touch. Move your finger slightly each time.";
    private static final String RFPS_ENROLLING_TITLE = "Lift, then touch again";

    private UiDevice mDevice;
    private byte[] mToken = new byte[]{};
    private Context mContext;
    private boolean mFingerprintPropCallbackLaunched;
    private boolean mCanAssumeUdfps;
    private boolean mCanAssumeSfps;
    private String mEnrollingTitle;

    private static final int IDLE_TIMEOUT = 10000;

    @Before
    public void setUp() throws InterruptedException {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        mContext = InstrumentationRegistry.getContext();

        // Stop every test if it is not a fingerprint device
        assumeTrue(mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_FINGERPRINT));

        final FingerprintManager fingerprintManager = mContext.getSystemService(
                FingerprintManager.class);
        mFingerprintPropCallbackLaunched = false;
        fingerprintManager.addAuthenticatorsRegisteredCallback(
                new IFingerprintAuthenticatorsRegisteredCallback.Stub() {
                    @Override
                    public void onAllAuthenticatorsRegistered(
                            List<FingerprintSensorPropertiesInternal> list) {
                        mFingerprintPropCallbackLaunched = true;

                        assertThat(list).isNotNull();
                        assertThat(list).isNotEmpty();
                        final FingerprintSensorPropertiesInternal prop = list.get(0);
                        mCanAssumeUdfps = prop.isAnyUdfpsType();
                        mCanAssumeSfps = prop.isAnySidefpsType();
                        if (mCanAssumeUdfps) {
                            mEnrollingTitle = UDFPS_ENROLLING_TITLE;
                        } else if (mCanAssumeSfps) {
                            mEnrollingTitle = SFPS_ENROLLING_TITLE;
                        } else {
                            mEnrollingTitle = RFPS_ENROLLING_TITLE;
                        }
                    }
                });

        for (long i = 0; i < IDLE_TIMEOUT && !mFingerprintPropCallbackLaunched; i += 100L) {
            Thread.sleep(100L);
        }
        assertThat(mFingerprintPropCallbackLaunched).isTrue();

        mDevice.pressHome();
    }

    @After
    public void tearDown() throws Exception {
        LockScreenUtil.resetLockscreen(TEST_PIN);
        mDevice.pressHome();
    }

    @Test
    public void testIntroChooseLock() {
        final Intent intent = newActivityIntent();
        mContext.startActivity(intent);
        assertThat(mDevice.wait(Until.hasObject(By.text("Choose your backup screen lock method")),
                IDLE_TIMEOUT)).isTrue();
    }

    private void verifyIntroPage() {
        mDevice.waitForIdle();
        for (long i = 0; i < IDLE_TIMEOUT; i += 100L) {
            if (mDevice.wait(Until.hasObject(By.text("More")), 50L)) {
                break;
            } else if (mDevice.wait(Until.hasObject(By.text("I agree")), 50L)) {
                break;
            }
        }

        // Click more btn at most twice and the introduction should stay in the last page
        UiObject2 moreBtn;
        for (int i = 0; i < 2 && (moreBtn = mDevice.findObject(By.text("More"))) != null; ++i) {
            moreBtn.click();
            mDevice.waitForIdle();
            mDevice.wait(Until.hasObject(By.text("More")), IDLE_TIMEOUT);
        }

        assertThat(mDevice.wait(Until.hasObject(By.text("No thanks")), IDLE_TIMEOUT)).isTrue();
        assertThat(mDevice.wait(Until.hasObject(By.text("I agree")), IDLE_TIMEOUT)).isTrue();
    }

    @Test
    public void testIntroWithGkPwHandle_withUdfps_clickStart() {
        assumeTrue(mCanAssumeUdfps);

        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true);

        launchIntroWithGkPwHandle(false);

        // Intro page
        verifyIntroPage();
        final UiObject2 agreeBtn = mDevice.findObject(By.text("I agree"));
        assertThat(agreeBtn).isNotNull();
        agreeBtn.click();

        // FindUdfps page
        assertThat(mDevice.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue();
        final UiObject2 lottie = mDevice.findObject(By.res(SETTINGS_PACKAGE_NAME,
                "illustration_lottie"));
        assertThat(lottie).isNotNull();
        assertThat(lottie.isClickable()).isTrue();
        final UiObject2 startBtn = mDevice.findObject(By.text("Start"));
        assertThat(startBtn.isClickable()).isTrue();
        startBtn.click();

        // Enrolling page
        assertThat(mDevice.wait(Until.hasObject(By.text(mEnrollingTitle)), IDLE_TIMEOUT)).isTrue();
    }

    @Test
    public void testIntroWithGkPwHandle_withUdfps_clickLottie() {
        assumeTrue(mCanAssumeUdfps);

        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true);

        launchIntroWithGkPwHandle(false);

        // Intro page
        verifyIntroPage();
        final UiObject2 agreeBtn = mDevice.findObject(By.text("I agree"));
        assertThat(agreeBtn).isNotNull();
        agreeBtn.click();

        // FindUdfps page
        assertThat(mDevice.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue();
        final UiObject2 lottie = mDevice.findObject(By.res(SETTINGS_PACKAGE_NAME,
                "illustration_lottie"));
        assertThat(lottie).isNotNull();
        assertThat(lottie.isClickable()).isTrue();
        final UiObject2 startBtn = mDevice.findObject(By.text("Start"));
        assertThat(startBtn.isClickable()).isTrue();
        lottie.click();

        // Enrolling page
        assertThat(mDevice.wait(Until.hasObject(By.text(mEnrollingTitle)), IDLE_TIMEOUT)).isTrue();
    }

    @Test
    public void testIntroWithGkPwHandle_withSfps() {
        assumeTrue(mCanAssumeSfps);

        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true);

        launchIntroWithGkPwHandle(false);

        // Intro page
        verifyIntroPage();
        final UiObject2 agreeBtn = mDevice.findObject(By.text("I agree"));
        assertThat(agreeBtn).isNotNull();
        agreeBtn.click();

        // FindSfps page
        assertThat(mDevice.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue();
        final UiObject2 lottie = mDevice.findObject(By.res(SETTINGS_PACKAGE_NAME,
                "illustration_lottie"));
        assertThat(lottie).isNotNull();

        // We don't have view which can be clicked to run to next page, stop at here.
    }

    @Test
    public void testIntroWithGkPwHandle_withRfps() {
        assumeFalse(mCanAssumeUdfps || mCanAssumeSfps);

        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true);

        launchIntroWithGkPwHandle(false);

        // Intro page
        verifyIntroPage();
        final UiObject2 agreeBtn = mDevice.findObject(By.text("I agree"));
        assertThat(agreeBtn).isNotNull();
        agreeBtn.click();

        // FindRfps page
        assertThat(mDevice.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue();
        final UiObject2 lottie = mDevice.findObject(By.res(SETTINGS_PACKAGE_NAME,
                "illustration_lottie"));
        if (lottie == null) {
            // FindSfps page shall have an animation view if no lottie view
            assertThat(mDevice.findObject(By.res(SETTINGS_PACKAGE_NAME,
                    "fingerprint_sensor_location_animation"))).isNotNull();
        }
    }

    @Test
    public void testIntroWithGkPwHandle_clickNoThanksInIntroPage() {
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true);

        launchIntroWithGkPwHandle(false);

        // Intro page
        verifyIntroPage();
        final UiObject2 noThanksBtn = mDevice.findObject(By.text("No thanks"));
        assertThat(noThanksBtn).isNotNull();
        noThanksBtn.click();

        // Back to home
        mDevice.waitForWindowUpdate("com.android.settings", IDLE_TIMEOUT);
        assertThat(mDevice.findObject(By.text("No thanks"))).isNull();
    }

    @Test
    public void testIntroWithGkPwHandle_clickSkipInFindSensor() {
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true);

        launchIntroWithGkPwHandle(false);

        // Intro page
        verifyIntroPage();
        final UiObject2 agreeBtn = mDevice.findObject(By.text("I agree"));
        assertThat(agreeBtn).isNotNull();
        agreeBtn.click();

        // FindSensor page
        assertThat(mDevice.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue();
        final UiObject2 doItLaterBtn = mDevice.findObject(By.text(DO_IT_LATER));
        assertThat(doItLaterBtn).isNotNull();
        assertThat(doItLaterBtn.isClickable()).isTrue();
        doItLaterBtn.click();

        // Back to home
        mDevice.waitForWindowUpdate("com.android.settings", IDLE_TIMEOUT);
        assertThat(mDevice.findObject(By.text(DO_IT_LATER))).isNull();
    }

    @Test
    public void testIntroWithGkPwHandle_clickSkipAnywayInFindFpsDialog_whenIsSuw() {
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true);

        launchIntroWithGkPwHandle(true);

        // Intro page
        verifyIntroPage();
        final UiObject2 agreeBtn = mDevice.findObject(By.text("I agree"));
        assertThat(agreeBtn).isNotNull();
        agreeBtn.click();

        // FindSensor page
        assertThat(mDevice.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue();
        final UiObject2 doItLaterBtn = mDevice.findObject(By.text(DO_IT_LATER));
        assertThat(doItLaterBtn).isNotNull();
        assertThat(doItLaterBtn.isClickable()).isTrue();
        doItLaterBtn.click();

        // SkipSetupFindFpsDialog
        assertThat(mDevice.wait(Until.hasObject(By.text("Skip fingerprint?")),
                IDLE_TIMEOUT)).isTrue();
        final UiObject2 skipAnywayBtn = mDevice.findObject(By.text("Skip anyway"));
        assertThat(skipAnywayBtn).isNotNull();
        assertThat(skipAnywayBtn.isClickable()).isTrue();
        skipAnywayBtn.click();

        // Back to home
        mDevice.waitForWindowUpdate("com.android.settings", IDLE_TIMEOUT);
        assertThat(mDevice.findObject(By.text("Skip anyway"))).isNull();
        assertThat(mDevice.findObject(By.text(DO_IT_LATER))).isNull();
    }

    @Test
    public void testIntroWithGkPwHandle_clickGoBackInFindFpsDialog_whenIsSuw() {
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true);

        launchIntroWithGkPwHandle(true);

        // Intro page
        verifyIntroPage();
        final UiObject2 agreeBtn = mDevice.findObject(By.text("I agree"));
        assertThat(agreeBtn).isNotNull();
        agreeBtn.click();

        // FindSensor page
        assertThat(mDevice.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue();
        final UiObject2 doItLaterBtn = mDevice.findObject(By.text(DO_IT_LATER));
        assertThat(doItLaterBtn).isNotNull();
        assertThat(doItLaterBtn.isClickable()).isTrue();
        doItLaterBtn.click();

        // SkipSetupFindFpsDialog
        assertThat(mDevice.wait(Until.hasObject(By.text("Skip fingerprint?")), IDLE_TIMEOUT))
                .isTrue();
        final UiObject2 goBackBtn = mDevice.findObject(By.text("Go back"));
        assertThat(goBackBtn).isNotNull();
        assertThat(goBackBtn.isClickable()).isTrue();
        goBackBtn.click();

        // FindSensor page again
        assertThat(mDevice.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue();
    }

    @Test
    public void testIntroCheckPin() {
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true);
        final Intent intent = newActivityIntent();
        mContext.startActivity(intent);
        assertThat(mDevice.wait(Until.hasObject(By.text("Enter your device PIN to continue")),
                IDLE_TIMEOUT)).isTrue();
    }

    @Test
    public void testEnrollingWithGkPwHandle() {
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true);

        launchEnrollingWithGkPwHandle();

        // Enrolling screen
        mDevice.waitForIdle();
        assertThat(mDevice.wait(Until.hasObject(By.text(mEnrollingTitle)), IDLE_TIMEOUT)).isTrue();
    }

    @Test
    public void testEnrollingIconTouchDialog_withSfps() {
        assumeTrue(mCanAssumeSfps);

        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true);

        launchEnrollingWithGkPwHandle();

        // Enrolling screen
        mDevice.waitForIdle();
        assertThat(mDevice.wait(Until.hasObject(By.text(mEnrollingTitle)), IDLE_TIMEOUT)).isTrue();

        final UiObject2 lottie = mDevice.findObject(By.res(SETTINGS_PACKAGE_NAME,
                "illustration_lottie"));
        assertThat(lottie).isNotNull();

        lottie.click();
        lottie.click();
        lottie.click();

        // IconTouchDialog
        mDevice.waitForIdle();
        assertThat(mDevice.wait(Until.hasObject(By.text("Touch the sensor instead")), IDLE_TIMEOUT))
                .isTrue();
        final UiObject2 okButton = mDevice.findObject(By.text("OK"));
        assertThat(okButton).isNotNull();

        okButton.click();

        // Enrolling screen again
        mDevice.waitForIdle();
        assertThat(mDevice.wait(Until.hasObject(By.text(mEnrollingTitle)), IDLE_TIMEOUT)).isTrue();
    }

    @Test
    public void testEnrollingIconTouchDialog_withRfps() {
        assumeFalse(mCanAssumeUdfps || mCanAssumeSfps);

        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true);

        launchEnrollingWithGkPwHandle();

        // Enrolling screen
        mDevice.waitForIdle();
        assertThat(mDevice.wait(Until.hasObject(By.text(mEnrollingTitle)), IDLE_TIMEOUT)).isTrue();

        final UiObject2 lottie = mDevice.findObject(By.res(SETTINGS_PACKAGE_NAME,
                "fingerprint_progress_bar"));
        assertThat(lottie).isNotNull();

        lottie.click();
        lottie.click();
        lottie.click();

        // IconTouchDialog
        mDevice.waitForIdle();
        assertThat(mDevice.wait(Until.hasObject(By.text("Whoops, that\u2019s not the sensor")),
                IDLE_TIMEOUT)).isTrue();
        final UiObject2 okButton = mDevice.findObject(By.text("OK"));
        assertThat(okButton).isNotNull();

        okButton.click();

        // Enrolling screen again
        mDevice.waitForIdle();
        assertThat(mDevice.wait(Until.hasObject(By.text(mEnrollingTitle)), IDLE_TIMEOUT)).isTrue();
    }

    @Test
    public void testFindUdfpsWithGkPwHandle_clickStart() {
        assumeTrue(mCanAssumeUdfps);

        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true);

        launchFindSensorWithGkPwHandle();

        // FindUdfps page
        assertThat(mDevice.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue();
        final UiObject2 lottie = mDevice.findObject(By.res(SETTINGS_PACKAGE_NAME,
                "illustration_lottie"));
        assertThat(lottie).isNotNull();
        assertThat(lottie.isClickable()).isTrue();
        final UiObject2 startBtn = mDevice.findObject(By.text("Start"));
        assertThat(startBtn.isClickable()).isTrue();
        startBtn.click();

        // Enrolling page
        assertThat(mDevice.wait(Until.hasObject(By.text(mEnrollingTitle)), IDLE_TIMEOUT)).isTrue();
    }

    @Test
    public void testFindUdfpsWithGkPwHandle_clickLottie() {
        assumeTrue(mCanAssumeUdfps);

        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true);

        launchFindSensorWithGkPwHandle();

        // FindUdfps page
        assertThat(mDevice.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue();
        final UiObject2 lottie = mDevice.findObject(By.res(SETTINGS_PACKAGE_NAME,
                "illustration_lottie"));
        assertThat(lottie).isNotNull();
        assertThat(lottie.isClickable()).isTrue();
        final UiObject2 startBtn = mDevice.findObject(By.text("Start"));
        assertThat(startBtn.isClickable()).isTrue();
        lottie.click();

        // Enrolling page
        assertThat(mDevice.wait(Until.hasObject(By.text(mEnrollingTitle)), IDLE_TIMEOUT)).isTrue();
    }

    @Test
    public void testFindSfpsWithGkPwHandle() {
        assumeTrue(mCanAssumeSfps);

        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true);

        launchFindSensorWithGkPwHandle();

        // FindSfps page
        assertThat(mDevice.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue();
        final UiObject2 lottie = mDevice.findObject(By.res(SETTINGS_PACKAGE_NAME,
                "illustration_lottie"));
        assertThat(lottie).isNotNull();

        // We don't have view which can be clicked to run to next page, stop at here.
    }

    @Test
    public void testFindRfpsWithGkPwHandle() {
        assumeFalse(mCanAssumeUdfps || mCanAssumeSfps);

        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true);

        launchFindSensorWithGkPwHandle();

        // FindRfps page
        assertThat(mDevice.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue();
        final UiObject2 lottie = mDevice.findObject(By.res(SETTINGS_PACKAGE_NAME,
                "illustration_lottie"));
        if (lottie == null) {
            // FindSfps page shall have an animation view if no lottie view
            assertThat(mDevice.findObject(By.res(SETTINGS_PACKAGE_NAME,
                    "fingerprint_sensor_location_animation"))).isNotNull();
        }
    }


    @Test
    public void testFindSensorWithGkPwHandle_clickSkipInFindSensor() {
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true);

        launchFindSensorWithGkPwHandle();

        // FindSensor page
        assertThat(mDevice.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue();
        final UiObject2 doItLaterBtn = mDevice.findObject(By.text(DO_IT_LATER));
        assertThat(doItLaterBtn).isNotNull();
        assertThat(doItLaterBtn.isClickable()).isTrue();
        doItLaterBtn.click();

        // Back to home
        mDevice.waitForWindowUpdate("com.android.settings", IDLE_TIMEOUT);
        assertThat(mDevice.findObject(By.text(DO_IT_LATER))).isNull();
    }

    private void launchIntroWithGkPwHandle(boolean isSuw) {
        LockPatternUtils lockPatternUtils = new LockPatternUtils(mContext);
        final LockscreenCredential lockscreenCredential = LockscreenCredential.createPin(TEST_PIN);
        final int userId = UserHandle.myUserId();
        final LockPatternChecker.OnVerifyCallback onVerifyCallback = (response, timeoutMs) -> {
            final Intent intent = newActivityIntent();
            if (isSuw) {
                intent.putExtra(EXTRA_IS_SETUP_FLOW, true);
            }
            intent.putExtra(EXTRA_KEY_GK_PW_HANDLE, response.getGatekeeperPasswordHandle());
            mContext.startActivity(intent);
        };
        LockPatternChecker.verifyCredential(lockPatternUtils, lockscreenCredential,
                userId, LockPatternUtils.VERIFY_FLAG_REQUEST_GK_PW_HANDLE, onVerifyCallback);
    }

    private void launchFindSensorWithGkPwHandle() {
        LockPatternUtils lockPatternUtils = new LockPatternUtils(mContext);
        final LockscreenCredential lockscreenCredential = LockscreenCredential.createPin(TEST_PIN);
        final int userId = UserHandle.myUserId();
        final LockPatternChecker.OnVerifyCallback onVerifyCallback = (response, timeoutMs) -> {
            final Intent intent = newActivityIntent();
            intent.putExtra(EXTRA_SKIP_INTRO, true);
            intent.putExtra(EXTRA_KEY_GK_PW_HANDLE, response.getGatekeeperPasswordHandle());
            mContext.startActivity(intent);
        };
        LockPatternChecker.verifyCredential(lockPatternUtils, lockscreenCredential,
                userId, LockPatternUtils.VERIFY_FLAG_REQUEST_GK_PW_HANDLE, onVerifyCallback);
    }

    private void launchEnrollingWithGkPwHandle() {
        LockPatternUtils lockPatternUtils = new LockPatternUtils(mContext);
        final LockscreenCredential lockscreenCredential = LockscreenCredential.createPin(TEST_PIN);
        final int userId = UserHandle.myUserId();
        final LockPatternChecker.OnVerifyCallback onVerifyCallback = (response, timeoutMs) -> {
            final Intent intent = newActivityIntent();
            intent.putExtra(EXTRA_SKIP_FIND_SENSOR, true);
            intent.putExtra(EXTRA_KEY_GK_PW_HANDLE, response.getGatekeeperPasswordHandle());
            mContext.startActivity(intent);
        };
        LockPatternChecker.verifyCredential(lockPatternUtils, lockscreenCredential,
                userId, LockPatternUtils.VERIFY_FLAG_REQUEST_GK_PW_HANDLE, onVerifyCallback);
    }

    @NonNull
    private Intent newActivityIntent() {
        Intent intent = new Intent();
        intent.setClassName(SETTINGS_PACKAGE_NAME, ACTIVITY_CLASS_NAME);
        intent.putExtra(EXTRA_FROM_SETTINGS_SUMMARY, true);
        intent.putExtra(EXTRA_PAGE_TRANSITION_TYPE, 1);
        intent.putExtra(Intent.EXTRA_USER_ID, mContext.getUserId());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;

    }
}
