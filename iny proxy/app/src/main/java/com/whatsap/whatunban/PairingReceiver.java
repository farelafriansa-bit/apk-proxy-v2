package com.whatsap.whatunban;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.app.RemoteInput;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PairingReceiver extends BroadcastReceiver {
    private static final String TAG = "PairingReceiver";
    public static final String ACTION_PAIR = "com.whatsap.whatunban.ACTION_PAIR";
    public static final String EXTRA_TEXT_REPLY = "extra_text_reply";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (ACTION_PAIR.equals(intent.getAction())) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                final String pairingCode = remoteInput.getCharSequence(EXTRA_TEXT_REPLY).toString();

                if (ShizukuHelper.isRunning(context)) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.d(TAG, "Attempting pairing with code: " + pairingCode);

                                // Wireless Debugging pairing usually requires:
                                // adb pair localhost:<port> <pairing_code>
                                // The port is dynamic and can be found via 'service.adb.tls.port'

                                String port = getSystemProperty("service.adb.tls.port");
                                if (port == null || port.isEmpty() || port.equals("0")) {
                                    showToast(context, "❌ Gagal: Wireless Debugging tidak aktif!");
                                    return;
                                }

                                // We need an 'adb' binary to run 'adb pair'.
                                // Since 'adb' is not standard in Android shell, we try to find it
                                // or use the 'am' command to trigger pairing if available.
                                // Alternatively, we can try to find the internal 'adb' binary if it exists.

                                String[] adbPaths = {"adb", "/system/bin/adb", "/system/xbin/adb", "/apex/com.android.adbd/bin/adb", "/data/local/tmp/adb"};
                                boolean success = false;

                                for (int i = 0; i < adbPaths.length; i++) {
                                    String cmd = adbPaths[i] + " pair localhost:" + port + " " + pairingCode;
                                    try {
                                        int exitCode = ShizukuHelper.executeCommand(cmd, context);
                                        if (exitCode == 0) {
                                            success = true;
                                            break;
                                        }
                                    } catch (Exception ignored) {}
                                }

                                if (success) {
                                    showToast(context, "✅ Pairing Berhasil!");
                                } else {
                                    showToast(context, "❌ Pairing Gagal. Pastikan Wireless Debugging Aktif di Opsi Pengembang.");
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                showToast(context, "⚠️ Error Pairing: " + e.getMessage());
                            }
                        }
                    }).start();
                } else {
                    Toast.makeText(context, "Shizuku tidak aktif!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String getSystemProperty(String key) {
        try {
            Process process = Runtime.getRuntime().exec("getprop " + key);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            return line;
        } catch (Exception e) {
            return null;
        }
    }

    private void showToast(final Context context, final String msg) {
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
