package com.whatsap.whatunban;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.content.SharedPreferences;
import android.util.Log;

public class GameNotificationListener extends NotificationListenerService {
    private static final String TAG = "GameNotifListener";
    private static final String PREFS_NAME = "GameSettings";
    private static final String KEY_BLOCK_NOTIF = "block_notifications";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean shouldBlock = prefs.getBoolean(KEY_BLOCK_NOTIF, false);

        if (!shouldBlock) {
            return;
        }

        String packageName = sbn.getPackageName();
        if (isInterruptive(packageName)) {
            Log.d(TAG, "Blocking notification from: " + packageName);
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                cancelNotification(sbn.getKey());
            } else {
                cancelNotification(sbn.getPackageName(), sbn.getTag(), sbn.getId());
            }
        }
    }

    private boolean isInterruptive(String pkg) {
        if (pkg == null) return false;

        // WhatsApp
        if (pkg.equals("com.whatsapp") || pkg.equals("com.whatsapp.w4b")) return true;

        // SMS
        if (pkg.equals("com.android.mms") ||
            pkg.equals("com.google.android.apps.messaging") ||
            pkg.contains("sms") || pkg.contains("messaging")) return true;

        // Phone / Calls
        if (pkg.equals("com.android.server.telecom") ||
            pkg.equals("com.android.phone") ||
            pkg.equals("com.google.android.dialer") ||
            pkg.contains("telecom") || pkg.contains("dialer")) return true;

        return false;
    }
}
