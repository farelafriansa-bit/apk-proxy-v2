package com.whatsap.whatunban;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.app.RemoteInput;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.content.ClipboardManager;
import android.content.ClipData;

public class PairingReceiver extends BroadcastReceiver {
    public static final String ACTION_PAIRING_CODE = "com.whatsap.whatunban.ACTION_PAIRING_CODE";
    public static final String KEY_TEXT_REPLY = "key_text_reply";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_PAIRING_CODE.equals(intent.getAction())) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                CharSequence result = remoteInput.getCharSequence(KEY_TEXT_REPLY);
                if (result != null) {
                    String pairingCode = result.toString().trim();

                    // Store pairing code
                    SharedPreferences prefs = context.getSharedPreferences("pairing", Context.MODE_PRIVATE);
                    prefs.edit().putString("pairing_code", pairingCode).apply();

                    // Copy to clipboard for convenience
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("pairing_code", pairingCode);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                    }

                    Toast.makeText(context, "🔑 Kode Pairing: " + pairingCode + " ✅ Disalin!", Toast.LENGTH_SHORT).show();

                    // Triggering a broadcast or intent to MainActivity could refresh the UI if it's open
                    Intent refreshIntent = new Intent("com.whatsap.whatunban.REFRESH_STATUS");
                    context.sendBroadcast(refreshIntent);
                }
            }
        }
    }
}
