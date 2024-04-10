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

import static androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import static androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY;

import static com.android.settings.biometrics2.factory.BiometricsViewModelFactory.CHALLENGE_GENERATOR_KEY;
import static com.android.settings.biometrics2.factory.BiometricsViewModelFactory.ENROLLMENT_REQUEST_KEY;
import static com.android.settings.biometrics2.factory.BiometricsViewModelFactory.USER_ID_KEY;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_FAIL_NEED_TO_CHOOSE_LOCK;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_IS_GENERATING_CHALLENGE;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_VALID;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.ErrorDialogData;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ENROLL_ENROLLING_ACTION_DONE;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ENROLL_ENROLLING_ACTION_SHOW_ICON_TOUCH_DIALOG;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_BACK_PRESSED;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_USER_SKIP;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_TIMEOUT;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FingerprintEnrollEnrollingAction;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FingerprintErrorDialogAction;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel.FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_DIALOG;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel.FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_SKIP;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel.FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_START;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel.FingerprintEnrollFindSensorAction;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFinishViewModel.FINGERPRINT_ENROLL_FINISH_ACTION_ADD_BUTTON_CLICK;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFinishViewModel.FINGERPRINT_ENROLL_FINISH_ACTION_NEXT_BUTTON_CLICK;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFinishViewModel.FingerprintEnrollFinishAction;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_CONTINUE_ENROLL;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_DONE_AND_FINISH;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_SKIP_OR_CANCEL;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FingerprintEnrollIntroAction;

import android.annotation.StyleRes;
import android.app.Application;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.viewmodel.CreationExtras;
import androidx.lifecycle.viewmodel.MutableCreationExtras;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.biometrics2.factory.BiometricsViewModelFactory;
import com.android.settings.biometrics2.ui.model.CredentialModel;
import com.android.settings.biometrics2.ui.model.EnrollmentRequest;
import com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel;
import com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.FingerprintChallengeGenerator;
import com.android.settings.biometrics2.ui.viewmodel.DeviceFoldedViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFinishViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollProgressViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollmentViewModel;
import com.android.settings.overlay.FeatureFactory;

import com.google.android.setupdesign.util.ThemeHelper;

/**
 * Fingerprint enrollment activity implementation
 */
public class FingerprintEnrollmentActivity extends FragmentActivity {

    private static final boolean DEBUG = false;
    private static final String TAG = "FingerprintEnrollmentActivity";

    private static final String INTRO_TAG = "intro";
    private static final String FIND_UDFPS_TAG = "find-udfps";
    private static final String FIND_SFPS_TAG = "find-sfps";
    private static final String FIND_RFPS_TAG = "find-rfps";
    private static final String ENROLLING_UDFPS_TAG = "enrolling-udfps";
    private static final String ENROLLING_SFPS_TAG = "enrolling-sfps";
    private static final String ENROLLING_RFPS_TAG = "enrolling-rfps";
    private static final String FINISH_TAG = "finish";
    private static final String SKIP_SETUP_FIND_FPS_DIALOG_TAG = "skip-setup-dialog";
    private static final String ENROLLING_ERROR_DIALOG_TAG = "enrolling-error-dialog";

    protected static final int LAUNCH_CONFIRM_LOCK_ACTIVITY = 1;

    // This flag is used for addBackStack(), we do not save it in ViewModel because it is just used
    // during FragmentManager calls
    private boolean mIsFirstFragmentAdded = false;

    private ViewModelProvider mViewModelProvider;
    private FingerprintEnrollmentViewModel mViewModel;
    private AutoCredentialViewModel mAutoCredentialViewModel;
    private final Observer<Integer> mIntroActionObserver = action -> {
        if (DEBUG) {
            Log.d(TAG, "mIntroActionObserver(" + action + ")");
        }
        if (action != null) {
            onIntroAction(action);
        }
    };
    private final Observer<Integer> mFindSensorActionObserver = action -> {
        if (DEBUG) {
            Log.d(TAG, "mFindSensorActionObserver(" + action + ")");
        }
        if (action != null) {
            onFindSensorAction(action);
        }
    };
    private final Observer<Integer> mEnrollingActionObserver = action -> {
        if (DEBUG) {
            Log.d(TAG, "mEnrollingActionObserver(" + action + ")");
        }
        if (action != null) {
            onEnrollingAction(action);
        }
    };
    private final Observer<ErrorDialogData> mEnrollingErrorDialogObserver = data -> {
        if (DEBUG) {
            Log.d(TAG, "mEnrollingErrorDialogObserver(" + data + ")");
        }
        if (data != null) {
            new FingerprintEnrollEnrollingErrorDialog().show(getSupportFragmentManager(),
                    ENROLLING_ERROR_DIALOG_TAG);
        }
    };
    private final Observer<Integer> mEnrollingErrorDialogActionObserver = action -> {
        if (DEBUG) {
            Log.d(TAG, "mEnrollingErrorDialogActionObserver(" + action + ")");
        }
        if (action != null) {
            onEnrollingErrorDialogAction(action);
        }
    };
    private final Observer<Integer> mFinishActionObserver = action -> {
        if (DEBUG) {
            Log.d(TAG, "mFinishActionObserver(" + action + ")");
        }
        if (action != null) {
            onFinishAction(action);
        }
    };
    private final ActivityResultCallback<ActivityResult> mChooseLockResultCallback =
            result -> onChooseOrConfirmLockResult(true /* isChooseLock */, result);
    private final ActivityResultLauncher<Intent> mChooseLockLauncher =
            registerForActivityResult(new StartActivityForResult(), mChooseLockResultCallback);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModelProvider = new ViewModelProvider(this);

        mViewModel = mViewModelProvider.get(FingerprintEnrollmentViewModel.class);
        mViewModel.setSavedInstanceState(savedInstanceState);

        mAutoCredentialViewModel = mViewModelProvider.get(AutoCredentialViewModel.class);
        mAutoCredentialViewModel.setCredentialModel(savedInstanceState, getIntent());

        // Theme
        setTheme(mViewModel.getRequest().getTheme());
        ThemeHelper.trySetDynamicColor(this);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        // fragment
        setContentView(R.layout.biometric_enrollment_container);

        final Fragment fragment = getSupportFragmentManager().findFragmentById(
                R.id.fragment_container_view);
        if (DEBUG) {
            Log.d(TAG, "onCreate() has savedInstance:" + (savedInstanceState != null)
                    + ", fragment:" + fragment);
        }
        if (fragment == null) {
            checkCredential();
            final EnrollmentRequest request = mViewModel.getRequest();
            if (request.isSkipFindSensor()) {
                startEnrollingFragment();
            } else if (request.isSkipIntro()) {
                startFindSensorFragment();
            } else {
                startIntroFragment();
            }
        } else {
            final String tag = fragment.getTag();
            if (INTRO_TAG.equals(tag)) {
                attachIntroViewModel();
            } else if (FIND_UDFPS_TAG.equals(tag) || FIND_SFPS_TAG.equals(tag)
                    || FIND_RFPS_TAG.equals(tag)) {
                attachFindSensorViewModel();
                attachIntroViewModel();
            } else if (ENROLLING_UDFPS_TAG.equals(tag) || ENROLLING_SFPS_TAG.equals(tag)
                    || ENROLLING_RFPS_TAG.equals(tag)) {
                attachEnrollingViewModel();
                attachFindSensorViewModel();
                attachIntroViewModel();
            } else if (FINISH_TAG.equals(tag)) {
                attachFinishViewModel();
                attachFindSensorViewModel();
                attachIntroViewModel();
            } else {
                Log.e(TAG, "fragment tag " + tag + " not found");
                finish();
                return;
            }
        }

        // observe LiveData
        mViewModel.getSetResultLiveData().observe(this, this::onSetActivityResult);

        mAutoCredentialViewModel.getGenerateChallengeFailedLiveData().observe(this,
                this::onGenerateChallengeFailed);
    }

    private void startFragment(@NonNull Class<? extends Fragment> fragmentClass,
            @NonNull String tag) {
        if (!mIsFirstFragmentAdded) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragment_container_view, fragmentClass, null, tag)
                    .commit();
            mIsFirstFragmentAdded = true;
        } else {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .setCustomAnimations(R.anim.shared_x_axis_activity_open_enter_dynamic_color,
                            R.anim.shared_x_axis_activity_open_exit,
                            R.anim.shared_x_axis_activity_close_enter_dynamic_color,
                            R.anim.shared_x_axis_activity_close_exit)
                    .replace(R.id.fragment_container_view, fragmentClass, null, tag)
                    .addToBackStack(tag)
                    .commit();
        }
    }

    private void startIntroFragment() {
        attachIntroViewModel();
        startFragment(FingerprintEnrollIntroFragment.class, INTRO_TAG);
    }

    private void attachIntroViewModel() {
        final EnrollmentRequest request = mViewModel.getRequest();
        if (request.isSkipIntro() || request.isSkipFindSensor()) {
            return;
        }

        final FingerprintEnrollIntroViewModel introViewModel =
                mViewModelProvider.get(FingerprintEnrollIntroViewModel.class);

        // Clear ActionLiveData in FragmentViewModel to prevent getting previous action during
        // recreate, like press 'Agree' then press 'back' in FingerprintEnrollFindSensor activity.
        introViewModel.clearActionLiveData();
        introViewModel.getActionLiveData().observe(this, mIntroActionObserver);
    }

    // We need to make sure token is valid before entering find sensor page
    private void startFindSensorFragment() {
        // Always setToken into progressViewModel even it is not necessary action for UDFPS
        mViewModelProvider.get(FingerprintEnrollProgressViewModel.class)
                .setToken(mAutoCredentialViewModel.getToken());

        attachFindSensorViewModel();

        final String tag;
        final Class<? extends Fragment> fragmentClass;
        if (mViewModel.canAssumeUdfps()) {
            tag = FIND_UDFPS_TAG;
            fragmentClass = FingerprintEnrollFindUdfpsFragment.class;
        } else if (mViewModel.canAssumeSfps()) {
            tag = FIND_SFPS_TAG;
            fragmentClass = FingerprintEnrollFindSfpsFragment.class;
        } else {
            tag = FIND_RFPS_TAG;
            fragmentClass = FingerprintEnrollFindRfpsFragment.class;
        }
        startFragment(fragmentClass, tag);
    }

    private void attachFindSensorViewModel() {
        if (mViewModel.getRequest().isSkipFindSensor()) {
            return;
        }

        final FingerprintEnrollFindSensorViewModel findSensorViewModel =
                mViewModelProvider.get(FingerprintEnrollFindSensorViewModel.class);

        // Clear ActionLiveData in FragmentViewModel to prevent getting previous action during
        // recreate, like press 'Start' then press 'back' in FingerprintEnrollEnrolling activity.
        findSensorViewModel.clearActionLiveData();
        findSensorViewModel.getActionLiveData().observe(this, mFindSensorActionObserver);
    }

    private void startEnrollingFragment() {
        // Always setToken into progressViewModel even it is not necessary action for SFPS or RFPS
        mViewModelProvider.get(FingerprintEnrollProgressViewModel.class)
                .setToken(mAutoCredentialViewModel.getToken());

        attachEnrollingViewModel();

        final String tag;
        final Class<? extends Fragment> fragmentClass;
        if (mViewModel.canAssumeUdfps()) {
            tag = ENROLLING_UDFPS_TAG;
            fragmentClass = FingerprintEnrollEnrollingUdfpsFragment.class;
        } else if (mViewModel.canAssumeSfps()) {
            tag = ENROLLING_SFPS_TAG;
            fragmentClass = FingerprintEnrollEnrollingSfpsFragment.class;
        } else {
            tag = ENROLLING_RFPS_TAG;
            fragmentClass = FingerprintEnrollEnrollingRfpsFragment.class;
        }
        startFragment(fragmentClass, tag);
    }

    private void attachEnrollingViewModel() {
        final FingerprintEnrollEnrollingViewModel enrollingViewModel =
                mViewModelProvider.get(FingerprintEnrollEnrollingViewModel.class);
        enrollingViewModel.clearActionLiveData();
        enrollingViewModel.getActionLiveData().observe(this, mEnrollingActionObserver);
        enrollingViewModel.getErrorDialogLiveData().observe(this, mEnrollingErrorDialogObserver);
        enrollingViewModel.getErrorDialogActionLiveData().observe(this,
                mEnrollingErrorDialogActionObserver);
    }

    private void startFinishFragment() {
        mViewModel.setIsNewFingerprintAdded();
        attachFinishViewModel();

        getSupportFragmentManager().popBackStack();
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            // Replace enrolling page
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .setCustomAnimations(R.anim.shared_x_axis_activity_open_enter_dynamic_color,
                            R.anim.shared_x_axis_activity_open_exit,
                            R.anim.shared_x_axis_activity_close_enter_dynamic_color,
                            R.anim.shared_x_axis_activity_close_exit)
                    .replace(R.id.fragment_container_view, FingerprintEnrollFinishFragment.class,
                            null, FINISH_TAG)
                    .commit();
        } else {
            // Remove Enrolling page from backstack, and add Finish page. Latest backstack will
            // be changed from Intro->FindSensor->Enrolling to Intro->FindSensor->Finish
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .setCustomAnimations(R.anim.shared_x_axis_activity_open_enter_dynamic_color,
                            R.anim.shared_x_axis_activity_open_exit,
                            R.anim.shared_x_axis_activity_close_enter_dynamic_color,
                            R.anim.shared_x_axis_activity_close_exit)
                    .replace(R.id.fragment_container_view, FingerprintEnrollFinishFragment.class,
                            null, FINISH_TAG)
                    .addToBackStack(FINISH_TAG)
                    .commit();
        }
    }

    private void attachFinishViewModel() {
        final FingerprintEnrollFinishViewModel viewModel =
                mViewModelProvider.get(FingerprintEnrollFinishViewModel.class);
        viewModel.clearActionLiveData();
        viewModel.getActionLiveData().observe(this, mFinishActionObserver);
    }

    private void onGenerateChallengeFailed(@NonNull Boolean ignoredBoolean) {
        onSetActivityResult(new ActivityResult(RESULT_CANCELED, null));
    }

    private void onSetActivityResult(@NonNull ActivityResult result) {
        final Bundle challengeExtras = mAutoCredentialViewModel.createGeneratingChallengeExtras();
        final ActivityResult overrideResult = mViewModel.getOverrideActivityResult(
                result, challengeExtras);
        if (DEBUG) {
            Log.d(TAG, "onSetActivityResult(" + result + "), override:" + overrideResult
                    + ") challengeExtras:" + challengeExtras);
        }
        setResult(overrideResult.getResultCode(), overrideResult.getData());
        finish();
    }

    private void checkCredential() {
        switch (mAutoCredentialViewModel.checkCredential()) {
            case CREDENTIAL_FAIL_NEED_TO_CHOOSE_LOCK: {
                final Intent intent = mAutoCredentialViewModel.createChooseLockIntent(this,
                        mViewModel.getRequest().isSuw(), mViewModel.getRequest().getSuwExtras());
                if (!mViewModel.isWaitingActivityResult().compareAndSet(false, true)) {
                    Log.w(TAG, "chooseLock, fail to set isWaiting flag to true");
                }
                mChooseLockLauncher.launch(intent);
                return;
            }
            case CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK: {
                final boolean launched = mAutoCredentialViewModel.createConfirmLockLauncher(
                        this,
                        LAUNCH_CONFIRM_LOCK_ACTIVITY,
                        getString(R.string.security_settings_fingerprint_preference_title)
                ).launch();
                if (!launched) {
                    // This shouldn't happen, as we should only end up at this step if a lock thingy
                    // is already set.
                    Log.e(TAG, "confirmLock, launched is true");
                    finish();
                } else if (!mViewModel.isWaitingActivityResult().compareAndSet(false, true)) {
                    Log.w(TAG, "confirmLock, fail to set isWaiting flag to true");
                }
                return;
            }
            case CREDENTIAL_VALID:
            case CREDENTIAL_IS_GENERATING_CHALLENGE: {
                // Do nothing
            }
        }
    }

    private void onChooseOrConfirmLockResult(boolean isChooseLock,
            @NonNull ActivityResult activityResult) {
        if (!mViewModel.isWaitingActivityResult().compareAndSet(true, false)) {
            Log.w(TAG, "isChooseLock:" + isChooseLock + ", fail to unset waiting flag");
        }
        if (mAutoCredentialViewModel.checkNewCredentialFromActivityResult(
                isChooseLock, activityResult)) {
            overridePendingTransition(R.anim.sud_slide_next_in, R.anim.sud_slide_next_out);
        } else {
            onSetActivityResult(activityResult);
        }
    }

    private void onIntroAction(@FingerprintEnrollIntroAction int action) {
        switch (action) {
            case FINGERPRINT_ENROLL_INTRO_ACTION_DONE_AND_FINISH: {
                onSetActivityResult(
                        new ActivityResult(BiometricEnrollBase.RESULT_FINISHED, null));
                return;
            }
            case FINGERPRINT_ENROLL_INTRO_ACTION_SKIP_OR_CANCEL: {
                onSetActivityResult(
                        new ActivityResult(BiometricEnrollBase.RESULT_SKIP, null));
                return;
            }
            case FINGERPRINT_ENROLL_INTRO_ACTION_CONTINUE_ENROLL: {
                startFindSensorFragment();
            }
        }
    }

    private void onFindSensorAction(@FingerprintEnrollFindSensorAction int action) {
        switch (action) {
            case FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_SKIP: {
                onSetActivityResult(new ActivityResult(BiometricEnrollBase.RESULT_SKIP, null));
                return;
            }
            case FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_DIALOG: {
                new SkipSetupFindFpsDialog().show(getSupportFragmentManager(),
                        SKIP_SETUP_FIND_FPS_DIALOG_TAG);
                return;
            }
            case FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_START: {
                startEnrollingFragment();
            }
        }
    }

    private void onEnrollingAction(@FingerprintEnrollEnrollingAction int action) {
        switch (action) {
            case FINGERPRINT_ENROLL_ENROLLING_ACTION_DONE: {
                startFinishFragment();
                break;
            }
            case FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_USER_SKIP: {
                onSetActivityResult(new ActivityResult(BiometricEnrollBase.RESULT_SKIP, null));
                break;
            }
            case FINGERPRINT_ENROLL_ENROLLING_ACTION_SHOW_ICON_TOUCH_DIALOG: {
                new FingerprintEnrollEnrollingIconTouchDialog().show(getSupportFragmentManager(),
                        SKIP_SETUP_FIND_FPS_DIALOG_TAG);
                break;
            }
            case FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_BACK_PRESSED: {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    onSetActivityResult(new ActivityResult(RESULT_CANCELED, null));
                }
                break;
            }
        }
    }

    private void onEnrollingErrorDialogAction(@FingerprintErrorDialogAction int action) {
        switch (action) {
            case FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH:
                onSetActivityResult(new ActivityResult(BiometricEnrollBase.RESULT_FINISHED, null));
                break;
            case FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_TIMEOUT:
                onSetActivityResult(new ActivityResult(BiometricEnrollBase.RESULT_TIMEOUT, null));
                break;
        }
    }

    private void onFinishAction(@FingerprintEnrollFinishAction int action) {
        switch (action) {
            case FINGERPRINT_ENROLL_FINISH_ACTION_ADD_BUTTON_CLICK: {
                startEnrollingFragment();
                break;
            }
            case FINGERPRINT_ENROLL_FINISH_ACTION_NEXT_BUTTON_CLICK: {
                final Intent data;
                if (mViewModel.getRequest().isSuw()) {
                    data = new Intent();
                    data.putExtras(mViewModel.getSuwFingerprintCountExtra(
                            mAutoCredentialViewModel.getUserId()));
                } else {
                    data = null;
                }
                onSetActivityResult(new ActivityResult(BiometricEnrollBase.RESULT_FINISHED, data));
                break;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mViewModel.checkFinishActivityDuringOnPause(isFinishing(), isChangingConfigurations());
    }

    @Override
    protected void onDestroy() {
        mViewModel.updateFingerprintSuggestionEnableState(mAutoCredentialViewModel.getUserId());
        super.onDestroy();
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, @StyleRes int resid, boolean first) {
        theme.applyStyle(R.style.SetupWizardPartnerResource, true);
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == LAUNCH_CONFIRM_LOCK_ACTIVITY) {
            onChooseOrConfirmLockResult(false, new ActivityResult(resultCode, data));
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @NonNull
    @Override
    public CreationExtras getDefaultViewModelCreationExtras() {
        final Application application =
                super.getDefaultViewModelCreationExtras().get(APPLICATION_KEY);
        final MutableCreationExtras ret = new MutableCreationExtras();
        ret.set(APPLICATION_KEY, application);

        final FingerprintRepository repository = FeatureFactory.getFactory(application)
                .getBiometricsRepositoryProvider().getFingerprintRepository(application);
        ret.set(CHALLENGE_GENERATOR_KEY, new FingerprintChallengeGenerator(repository));

        ret.set(ENROLLMENT_REQUEST_KEY, new EnrollmentRequest(getIntent(),
                getApplicationContext()));

        Bundle extras = getIntent().getExtras();
        final CredentialModel credentialModel = new CredentialModel(extras,
                SystemClock.elapsedRealtimeClock());
        ret.set(USER_ID_KEY, credentialModel.getUserId());

        return ret;
    }

    @NonNull
    @Override
    public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
        return new BiometricsViewModelFactory();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        getWindow().setStatusBarColor(getBackgroundColor());
    }

    @ColorInt
    private int getBackgroundColor() {
        final ColorStateList stateList = Utils.getColorAttr(this, android.R.attr.windowBackground);
        return stateList != null ? stateList.getDefaultColor() : Color.TRANSPARENT;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        mViewModelProvider.get(DeviceFoldedViewModel.class).onConfigurationChanged(newConfig);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mViewModel.onSaveInstanceState(outState);
        mAutoCredentialViewModel.onSaveInstanceState(outState);
    }
}
