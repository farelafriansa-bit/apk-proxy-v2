package com.whatsap.whatunban;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;

public class CrashActivity extends Activity {

    private String mLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(android.R.style.Theme_DeviceDefault);
        setTitle("App Crash");

        mLog = getIntent().getStringExtra(Intent.EXTRA_TEXT);

        ScrollView contentView = new ScrollView(this);
        contentView.setFillViewport(true);

        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);

        TextView textView = new TextView(this);
        int padding = dp2px(16);
        textView.setPadding(padding, padding, padding, padding);
        textView.setText(mLog);
        textView.setTextIsSelectable(true);
        textView.setTypeface(Typeface.DEFAULT);
        textView.setLinksClickable(true);

        horizontalScrollView.addView(textView);
        contentView.addView(horizontalScrollView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        setContentView(contentView);
    }

    private void restart() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        finish();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    private static int dp2px(float dpValue) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, android.R.id.copy, 0, android.R.string.copy)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.copy:
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText(getPackageName(), mLog));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        restart();
    }
}
