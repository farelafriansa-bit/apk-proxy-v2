package com.whatsap.whatunban;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.content.SharedPreferences;
import android.util.Log;

public class GameNotificationListener extends NotificationListenerService {
    private static final String TAG = "GameNotifListener";
    private static final String PREFS_NAME = "GamingModePrefs";
    private static final String KEY_GAMING_MODE = "gaming_mode_active";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isGamingMode = prefs.getBoolean(KEY_GAMING_MODE, false);

        if (isGamingMode) {
            String packageName = sbn.getPackageName();

            // Do not block our own app notifications
            if (packageName.equals(getPackageName())) {
                return;
            }

            // Real blocking of disturbing apps during game
            // We block common messaging and social apps, or just block everything else
            Log.d(TAG, "Gaming Mode Active: Cancelling notification from " + packageName);

            // Standard behavior for Gaming Mode: Block all except our app
            cancelNotification(sbn.getKey());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // No action needed
    }
}
