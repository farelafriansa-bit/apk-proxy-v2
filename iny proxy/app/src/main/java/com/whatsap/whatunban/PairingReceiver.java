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
                    String input = result.toString().trim();

                    // Store input
                    SharedPreferences prefs = context.getSharedPreferences("pairing", Context.MODE_PRIVATE);
                    prefs.edit().putString("pairing_code_input", input).apply();

                    // Copy to clipboard as requested for convenience
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("pairing_data", input);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                    }

                    Toast.makeText(context, "✅ Data Pairing: " + input + " disalin!", Toast.LENGTH_SHORT).show();

                    // We notify the user that we received it
                    // In a real scenario, we would trigger 'adb pair' here if we had the adb implementation
                }
            }
        }
    }
}
