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
package com.android.settings.fuelgauge.batteryusage;

import android.annotation.IntDef;
import android.annotation.Nullable;
import android.app.usage.IUsageStatsManager;
import android.app.usage.UsageEvents.Event;
import android.app.usage.UsageStatsManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.BatteryUsageStats;
import android.os.Build;
import android.os.LocaleList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batteryusage.db.AppUsageEventEntity;
import com.android.settings.fuelgauge.batteryusage.db.BatteryEventEntity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;
import java.util.TimeZone;

/** A utility class to convert data into another types. */
public final class ConvertUtils {
    private static final String TAG = "ConvertUtils";

    /** A fake package name to represent no BatteryEntry data. */
    public static final String FAKE_PACKAGE_NAME = "fake_package";

    @IntDef(prefix = {"CONSUMER_TYPE"}, value = {
            CONSUMER_TYPE_UNKNOWN,
            CONSUMER_TYPE_UID_BATTERY,
            CONSUMER_TYPE_USER_BATTERY,
            CONSUMER_TYPE_SYSTEM_BATTERY,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ConsumerType {
    }

    public static final int CONSUMER_TYPE_UNKNOWN = 0;
    public static final int CONSUMER_TYPE_UID_BATTERY = 1;
    public static final int CONSUMER_TYPE_USER_BATTERY = 2;
    public static final int CONSUMER_TYPE_SYSTEM_BATTERY = 3;

    private ConvertUtils() {
    }

    /** Converts {@link BatteryEntry} to content values */
    public static ContentValues convertBatteryEntryToContentValues(
            final BatteryEntry entry,
            final BatteryUsageStats batteryUsageStats,
            final int batteryLevel,
            final int batteryStatus,
            final int batteryHealth,
            final long bootTimestamp,
            final long timestamp,
            final boolean isFullChargeStart) {
        final ContentValues values = new ContentValues();
        if (entry != null && batteryUsageStats != null) {
            values.put(BatteryHistEntry.KEY_UID, Long.valueOf(entry.getUid()));
            values.put(BatteryHistEntry.KEY_USER_ID,
                    Long.valueOf(UserHandle.getUserId(entry.getUid())));
            values.put(BatteryHistEntry.KEY_PACKAGE_NAME,
                    entry.getDefaultPackageName());
            values.put(BatteryHistEntry.KEY_CONSUMER_TYPE,
                    Integer.valueOf(entry.getConsumerType()));
        } else {
            values.put(BatteryHistEntry.KEY_PACKAGE_NAME, FAKE_PACKAGE_NAME);
        }
        values.put(BatteryHistEntry.KEY_TIMESTAMP, Long.valueOf(timestamp));
        values.put(BatteryHistEntry.KEY_IS_FULL_CHARGE_CYCLE_START,
                Boolean.valueOf(isFullChargeStart));
        final BatteryInformation batteryInformation =
                constructBatteryInformation(
                        entry,
                        batteryUsageStats,
                        batteryLevel,
                        batteryStatus,
                        batteryHealth,
                        bootTimestamp);
        values.put(BatteryHistEntry.KEY_BATTERY_INFORMATION,
                convertBatteryInformationToString(batteryInformation));
        // Save the BatteryInformation unencoded string into database for debugging.
        if (Build.TYPE.equals("userdebug")) {
            values.put(
                    BatteryHistEntry.KEY_BATTERY_INFORMATION_DEBUG, batteryInformation.toString());
        }
        return values;
    }

    /** Converts {@link AppUsageEvent} to content values */
    public static ContentValues convertAppUsageEventToContentValues(final AppUsageEvent event) {
        final ContentValues values = new ContentValues();
        values.put(AppUsageEventEntity.KEY_UID, event.getUid());
        values.put(AppUsageEventEntity.KEY_USER_ID, event.getUserId());
        values.put(AppUsageEventEntity.KEY_TIMESTAMP, event.getTimestamp());
        values.put(AppUsageEventEntity.KEY_APP_USAGE_EVENT_TYPE, event.getType().getNumber());
        values.put(AppUsageEventEntity.KEY_PACKAGE_NAME, event.getPackageName());
        values.put(AppUsageEventEntity.KEY_INSTANCE_ID, event.getInstanceId());
        values.put(AppUsageEventEntity.KEY_TASK_ROOT_PACKAGE_NAME, event.getTaskRootPackageName());
        return values;
    }

    /** Converts {@link BatteryEvent} to content values */
    public static ContentValues convertBatteryEventToContentValues(final BatteryEvent event) {
        final ContentValues values = new ContentValues();
        values.put(BatteryEventEntity.KEY_TIMESTAMP, event.getTimestamp());
        values.put(BatteryEventEntity.KEY_BATTERY_EVENT_TYPE, event.getType().getNumber());
        values.put(BatteryEventEntity.KEY_BATTERY_LEVEL, event.getBatteryLevel());
        return values;
    }

    /** Gets the encoded string from {@link BatteryInformation} instance. */
    public static String convertBatteryInformationToString(
            final BatteryInformation batteryInformation) {
        return Base64.encodeToString(batteryInformation.toByteArray(), Base64.DEFAULT);
    }

    /** Gets the {@link BatteryInformation} instance from {@link ContentValues}. */
    public static BatteryInformation getBatteryInformation(
            final ContentValues values, final String key) {
        final BatteryInformation defaultInstance = BatteryInformation.getDefaultInstance();
        if (values != null && values.containsKey(key)) {
            return BatteryUtils.parseProtoFromString(values.getAsString(key), defaultInstance);
        }
        return defaultInstance;
    }

    /** Gets the {@link BatteryInformation} instance from {@link Cursor}. */
    public static BatteryInformation getBatteryInformation(final Cursor cursor, final String key) {
        final BatteryInformation defaultInstance = BatteryInformation.getDefaultInstance();
        final int columnIndex = cursor.getColumnIndex(key);
        if (columnIndex >= 0) {
            return BatteryUtils.parseProtoFromString(
                    cursor.getString(columnIndex), defaultInstance);
        }
        return defaultInstance;
    }

    /** Converts to {@link BatteryHistEntry} */
    public static BatteryHistEntry convertToBatteryHistEntry(
            BatteryEntry entry,
            BatteryUsageStats batteryUsageStats) {
        return new BatteryHistEntry(
                convertBatteryEntryToContentValues(
                        entry,
                        batteryUsageStats,
                        /*batteryLevel=*/ 0,
                        /*batteryStatus=*/ 0,
                        /*batteryHealth=*/ 0,
                        /*bootTimestamp=*/ 0,
                        /*timestamp=*/ 0,
                        /*isFullChargeStart=*/ false));
    }

    /** Converts to {@link AppUsageEvent} from {@link Event} */
    @Nullable
    public static AppUsageEvent convertToAppUsageEvent(
            Context context, final IUsageStatsManager usageStatsManager, final Event event,
            final long userId) {
        final String packageName = event.getPackageName();
        if (packageName == null) {
            // See b/190609174: Event package names should never be null, but sometimes they are.
            // Note that system events like device shutting down should still come with the android
            // package name.
            Log.w(TAG, String.format(
                    "Ignoring a usage event with null package name (timestamp=%d, type=%d)",
                    event.getTimeStamp(), event.getEventType()));
            return null;
        }

        final AppUsageEvent.Builder appUsageEventBuilder = AppUsageEvent.newBuilder();
        appUsageEventBuilder
                .setTimestamp(event.getTimeStamp())
                .setType(getAppUsageEventType(event.getEventType()))
                .setPackageName(packageName)
                .setUserId(userId);

        final String taskRootPackageName = getTaskRootPackageName(event);
        if (taskRootPackageName != null) {
            appUsageEventBuilder.setTaskRootPackageName(taskRootPackageName);
        }

        final String effectivePackageName =
                getEffectivePackageName(usageStatsManager, packageName, taskRootPackageName);
        try {
            final long uid = context
                    .getPackageManager()
                    .getPackageUidAsUser(effectivePackageName, (int) userId);
            appUsageEventBuilder.setUid(uid);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, String.format(
                    "Fail to get uid for package %s of user %d)", event.getPackageName(), userId));
            return null;
        }

        try {
            appUsageEventBuilder.setInstanceId(event.getInstanceId());
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            Log.w(TAG, "UsageEvent instance ID API error");
        }

        return appUsageEventBuilder.build();
    }

    /** Converts to {@link AppUsageEvent} from {@link Cursor} */
    public static AppUsageEvent convertToAppUsageEventFromCursor(final Cursor cursor) {
        final AppUsageEvent.Builder eventBuilder = AppUsageEvent.newBuilder();
        eventBuilder.setTimestamp(getLongFromCursor(cursor, AppUsageEventEntity.KEY_TIMESTAMP));
        eventBuilder.setType(
                AppUsageEventType.forNumber(
                        getIntegerFromCursor(
                                cursor, AppUsageEventEntity.KEY_APP_USAGE_EVENT_TYPE)));
        eventBuilder.setPackageName(
                getStringFromCursor(cursor, AppUsageEventEntity.KEY_PACKAGE_NAME));
        eventBuilder.setInstanceId(
                getIntegerFromCursor(cursor, AppUsageEventEntity.KEY_INSTANCE_ID));
        eventBuilder.setTaskRootPackageName(
                getStringFromCursor(cursor, AppUsageEventEntity.KEY_TASK_ROOT_PACKAGE_NAME));
        eventBuilder.setUserId(getLongFromCursor(cursor, AppUsageEventEntity.KEY_USER_ID));
        eventBuilder.setUid(getLongFromCursor(cursor, AppUsageEventEntity.KEY_UID));
        return eventBuilder.build();
    }

    /** Converts to {@link BatteryEvent} from {@link BatteryEventType} */
    public static BatteryEvent convertToBatteryEvent(
            long timestamp, BatteryEventType type, int batteryLevel) {
        final BatteryEvent.Builder eventBuilder = BatteryEvent.newBuilder();
        eventBuilder.setTimestamp(timestamp);
        eventBuilder.setType(type);
        eventBuilder.setBatteryLevel(batteryLevel);
        return eventBuilder.build();
    }

    /** Converts to {@link BatteryEvent} from {@link Cursor} */
    public static BatteryEvent convertToBatteryEventFromCursor(final Cursor cursor) {
        final BatteryEvent.Builder eventBuilder = BatteryEvent.newBuilder();
        eventBuilder.setTimestamp(getLongFromCursor(cursor, BatteryEventEntity.KEY_TIMESTAMP));
        eventBuilder.setType(
                BatteryEventType.forNumber(
                        getIntegerFromCursor(
                                cursor, BatteryEventEntity.KEY_BATTERY_EVENT_TYPE)));
        eventBuilder.setBatteryLevel(
                getIntegerFromCursor(cursor, BatteryEventEntity.KEY_BATTERY_LEVEL));
        return eventBuilder.build();
    }

    /** Converts UTC timestamp to local time string for logging only, so use the US locale for
     *  better readability in debugging. */
    public static String utcToLocalTimeForLogging(long timestamp) {
        final Locale locale = Locale.US;
        final String pattern =
                DateFormat.getBestDateTimePattern(locale, "MMM dd,yyyy HH:mm:ss");
        return DateFormat.format(pattern, timestamp).toString();
    }

    /** Converts UTC timestamp to local time hour data. */
    public static String utcToLocalTimeHour(final Context context, final long timestamp,
            final boolean is24HourFormat, final boolean showMinute) {
        final Locale locale = getLocale(context);
        // e.g. for 12-hour format: 9 PM
        // e.g. for 24-hour format: 09:00
        final String skeleton = is24HourFormat ? "HHm" : (showMinute ? "hma" : "ha");
        final String pattern = DateFormat.getBestDateTimePattern(locale, skeleton);
        return DateFormat.format(pattern, timestamp).toString();
    }

    /** Converts UTC timestamp to local time day of week data. */
    public static String utcToLocalTimeDayOfWeek(
            final Context context, final long timestamp, final boolean isAbbreviation) {
        final Locale locale = getLocale(context);
        final String pattern = DateFormat.getBestDateTimePattern(locale,
                isAbbreviation ? "E" : "EEEE");
        return DateFormat.format(pattern, timestamp).toString();
    }

    @VisibleForTesting
    static Locale getLocale(Context context) {
        if (context == null) {
            return Locale.getDefault();
        }
        final LocaleList locales =
                context.getResources().getConfiguration().getLocales();
        return locales != null && !locales.isEmpty() ? locales.get(0)
                : Locale.getDefault();
    }

    /**
     * Returns the package name the app usage should be attributed to.
     *
     * <ul>
     *   <li>If {@link UsageStatsManager#getUsageSource()} returns {@link
     *       UsageStatsManager#USAGE_SOURCE_CURRENT_ACTIVITY}, this method will return packageName.
     *   <li>If {@link UsageStatsManager#getUsageSource()} returns {@link
     *       UsageStatsManager#USAGE_SOURCE_TASK_ROOT_ACTIVITY}, this method will return
     *       taskRootPackageName if it exists, or packageName otherwise.
     * </ul>
     */
    @VisibleForTesting
    static String getEffectivePackageName(
            final IUsageStatsManager usageStatsManager, final String packageName,
            final String taskRootPackageName) {
        int usageSource = getUsageSource(usageStatsManager);
        switch (usageSource) {
            case UsageStatsManager.USAGE_SOURCE_TASK_ROOT_ACTIVITY:
                return !TextUtils.isEmpty(taskRootPackageName)
                        ? taskRootPackageName
                        : packageName;
            case UsageStatsManager.USAGE_SOURCE_CURRENT_ACTIVITY:
                return packageName;
            default:
                Log.e(TAG, "Unexpected usage source: " + usageSource);
                return packageName;
        }
    }

    /**
     * Returns the package name of the task root when this event was reported when {@code event} is
     * one of:
     *
     * <ul>
     *   <li>{@link Event#ACTIVITY_RESUMED}
     *   <li>{@link Event#ACTIVITY_STOPPED}
     * </ul>
     */
    @Nullable
    private static String getTaskRootPackageName(Event event) {
        int eventType = event.getEventType();
        if (eventType != Event.ACTIVITY_RESUMED && eventType != Event.ACTIVITY_STOPPED) {
            // Task root is only relevant for ACTIVITY_* events.
            return null;
        }

        try {
            String taskRootPackageName = event.getTaskRootPackageName();
            if (taskRootPackageName == null) {
                Log.w(TAG, String.format(
                        "Null task root in event with timestamp %d, type=%d, package %s",
                        event.getTimeStamp(), event.getEventType(), event.getPackageName()));
            }
            return taskRootPackageName;
        } catch (NoSuchMethodError e) {
            Log.w(TAG, "Failed to call Event#getTaskRootPackageName()");
            return null;
        }
    }

    /**
     * Returns what App Usage Observers will consider the source of usage for an activity.
     *
     * @see UsageStatsManager#getUsageSource()
     */
    private static int getUsageSource(final IUsageStatsManager usageStatsManager) {
        try {
            return usageStatsManager.getUsageSource();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to getUsageSource", e);
            return UsageStatsManager.USAGE_SOURCE_CURRENT_ACTIVITY;
        }
    }

    private static AppUsageEventType getAppUsageEventType(final int eventType) {
        switch (eventType) {
            case Event.ACTIVITY_RESUMED:
                return AppUsageEventType.ACTIVITY_RESUMED;
            case Event.ACTIVITY_STOPPED:
                return AppUsageEventType.ACTIVITY_STOPPED;
            case Event.DEVICE_SHUTDOWN:
                return AppUsageEventType.DEVICE_SHUTDOWN;
            default:
                return AppUsageEventType.UNKNOWN;
        }
    }

    private static BatteryInformation constructBatteryInformation(
            final BatteryEntry entry,
            final BatteryUsageStats batteryUsageStats,
            final int batteryLevel,
            final int batteryStatus,
            final int batteryHealth,
            final long bootTimestamp) {
        final DeviceBatteryState deviceBatteryState =
                DeviceBatteryState
                        .newBuilder()
                        .setBatteryLevel(batteryLevel)
                        .setBatteryStatus(batteryStatus)
                        .setBatteryHealth(batteryHealth)
                        .build();
        final BatteryInformation.Builder batteryInformationBuilder =
                BatteryInformation
                        .newBuilder()
                        .setDeviceBatteryState(deviceBatteryState)
                        .setBootTimestamp(bootTimestamp)
                        .setZoneId(TimeZone.getDefault().getID());
        if (entry != null && batteryUsageStats != null) {
            batteryInformationBuilder
                    .setIsHidden(entry.isHidden())
                    .setAppLabel(entry.getLabel() != null ? entry.getLabel() : "")
                    .setTotalPower(batteryUsageStats.getConsumedPower())
                    .setConsumePower(entry.getConsumedPower())
                    .setForegroundUsageConsumePower(entry.getConsumedPowerInForeground())
                    .setForegroundServiceUsageConsumePower(
                            entry.getConsumedPowerInForegroundService())
                    .setBackgroundUsageConsumePower(entry.getConsumedPowerInBackground())
                    .setCachedUsageConsumePower(entry.getConsumedPowerInCached())
                    .setPercentOfTotal(entry.mPercent)
                    .setDrainType(entry.getPowerComponentId())
                    .setForegroundUsageTimeInMs(entry.getTimeInForegroundMs())
                    .setBackgroundUsageTimeInMs(entry.getTimeInBackgroundMs());
        }

        return batteryInformationBuilder.build();
    }

    private static int getIntegerFromCursor(final Cursor cursor, final String key) {
        final int columnIndex = cursor.getColumnIndex(key);
        if (columnIndex >= 0) {
            return cursor.getInt(columnIndex);
        }
        return 0;
    }

    private static long getLongFromCursor(final Cursor cursor, final String key) {
        final int columnIndex = cursor.getColumnIndex(key);
        if (columnIndex >= 0) {
            return cursor.getLong(columnIndex);
        }
        return 0L;
    }

    private static String getStringFromCursor(final Cursor cursor, final String key) {
        final int columnIndex = cursor.getColumnIndex(key);
        if (columnIndex >= 0) {
            return cursor.getString(columnIndex);
        }
        return "";
    }
}
