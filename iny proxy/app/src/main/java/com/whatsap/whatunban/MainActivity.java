package com.whatsap.whatunban;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Import Shizuku API
import moe.shizuku.api.Shizuku;

public class MainActivity extends Activity {
    private WebView webView;
    private TextView statusText;
    private Handler handler = new Handler(Looper.getMainLooper());

    private static final String CHANNEL_ID = "debug_channel";
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int OVERLAY_PERMISSION_REQUEST = 124;
    private static final int SHIZUKU_PERMISSION_REQUEST_CODE = 1000;

    private List<String> userGameList = java.util.Collections.synchronizedList(new ArrayList<String>());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind UI components
        statusText = (TextView) findViewById(R.id.statusText);
        webView = (WebView) findViewById(R.id.webView);

        createNotificationChannel();
        requestAllPermissions();

        // Setup WebView configuration
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);

        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.loadUrl("file:///android_asset/login.html");

        webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return true;
                }
            });

        webView.setWebChromeClient(new WebChromeClient());

        // Handle Shizuku permission results
        Shizuku.addRequestPermissionResultListener(new Shizuku.OnRequestPermissionResultListener() {
            @Override
            public void onRequestPermissionResult(int requestCode, int grantResult) {
                if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        updateStatusText("Shizuku: Permission Granted");
                    } else {
                        updateStatusText("Shizuku: Permission Denied");
                    }
                }
            }
        });
    }

    private void requestAllPermissions() {
        List<String> permissions = new ArrayList<String>();

        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                permissions.add("android.permission.POST_NOTIFICATIONS");
            }
        }

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissions.isEmpty()) {
            requestPermissions(permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }

        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
									   Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Debug Notifications";
            String description = "Notifications for Shizuku and Debugging";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void updateStatusText(final String text) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (statusText != null) {
                    statusText.setText("Status: " + text);
                }
            }
        });
    }

    // ========================================
    // SHIZUKU CORE METHODS
    // ========================================

    // 1. Fungsi untuk mengecek apakah Shizuku sedang berjalan
    private boolean isShizukuRunning() {
        try {
            // Gunakan Shizuku.pingBinder() untuk cek apakah service aktif
            return Shizuku.pingBinder();
        } catch (Exception e) {
            // Jika terjadi error, Shizuku dianggap tidak aktif
            return false;
        }
    }

    // 2. Fungsi utama untuk copy file (Paste Config)
    private void pasteFile() {
        // a. Cek apakah Shizuku aktif
        if (!isShizukuRunning()) {
            showToast("Shizuku is not running!");
            updateStatusText("Shizuku: Not Running");
            return;
        }

        // Cek izin Shizuku, minta jika belum ada
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE);
            return;
        }

        // b. Cek file sumber di Download
        final String sourcePath = "/sdcard/Download/localconfig.json";
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            showToast("Source file not found: " + sourcePath);
            updateStatusText("Error: Source missing");
            return;
        }

        // c. Tentukan folder tujuan (com.dts.freefireth)
        final String targetDir = "/data/data/com.dts.freefireth/files/";

        // d. Eksekusi perintah copy via Shizuku shell
        // Menjalankan di thread terpisah agar tidak freeze UI
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    updateStatusText("Processing paste config...");

                    // Buat folder tujuan jika belum ada
                    String mkdirCmd = "mkdir -p " + targetDir;
                    executeShizukuCommand(mkdirCmd);

                    // Salin file menggunakan perintah cp
                    String cpCmd = "cp " + sourcePath + " " + targetDir;
                    int exitCode = executeShizukuCommand(cpCmd);

                    // e. Tampilkan feedback hasil
                    if (exitCode == 0) {
                        showToast("✅ Paste Config Berhasil!");
                        updateStatusText("Success: Config applied to FF");

                        // f. Tutup aplikasi setelah sukses
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }, 2000);
                    } else {
                        showToast("❌ Gagal Paste Config. Code: " + exitCode);
                        updateStatusText("Error: Command failed (" + exitCode + ")");
                    }
                } catch (final Exception e) {
                    showToast("⚠️ Terjadi kesalahan sistem!");
                    updateStatusText("Exception: " + e.getMessage());
                }
            }
        }).start();
    }

    // 3. Eksekusi shell command menggunakan Shizuku.newProcess
    private int executeShizukuCommand(String command) throws Exception {
        String[] args = new String[]{"sh", "-c", command};
        Process process = Shizuku.newProcess(args, null, null);

        // Tunggu hingga perintah selesai dijalankan
        int exitCode = process.waitFor();

        // Bersihkan streams
        process.getInputStream().close();
        process.getErrorStream().close();
        process.getOutputStream().close();
        process.destroy();

        return exitCode;
    }

    private void showToast(final String msg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ========================================
    // WEB APP INTERFACE (JavaScript bridge)
    // ========================================
    class WebAppInterface {

        @JavascriptInterface
        public void testConnection() {
            handler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(MainActivity.this, "✅ JavaScript Terhubung!", Toast.LENGTH_SHORT).show();
					}
				});
        }

        @JavascriptInterface
        public void openShizuku() {
            handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent intent = getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
                            if (intent != null) {
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                Toast.makeText(MainActivity.this, "🔓 Membuka Shizuku...", Toast.LENGTH_SHORT).show();
                            } else {
                                Intent playStore = new Intent(Intent.ACTION_VIEW);
                                playStore.setData(Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"));
                                playStore.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(playStore);
                                Toast.makeText(MainActivity.this, "⚠️ Shizuku belum terinstall!", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "⚠️ Gagal membuka Shizuku!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        }

        @JavascriptInterface
        public void pasteFile() {
            MainActivity.this.pasteFile();
        }

        @JavascriptInterface
        public String checkDebugStatus() {
            JSONObject status = new JSONObject();
            try {
                boolean adbEnabled = Settings.Global.getInt(getContentResolver(), "adb_enabled", 0) > 0;

                boolean shizukuInstalled = false;
                try {
                    getPackageManager().getApplicationInfo("moe.shizuku.privileged.api", 0);
                    shizukuInstalled = true;
                } catch (PackageManager.NameNotFoundException e) {
                    shizukuInstalled = false;
                }
                status.put("shizuku_installed", shizukuInstalled);

                boolean shizukuRunning = isShizukuRunning();
                status.put("shizuku", shizukuRunning);
                status.put("usb_debug", adbEnabled);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return status.toString();
        }

        @JavascriptInterface
        public void showToast(final String message) {
            MainActivity.this.showToast(message);
        }

        @JavascriptInterface
        public String scanAllGames() {
            JSONArray gamesArray = new JSONArray();
            PackageManager pm = getPackageManager();
            try {
                List<ApplicationInfo> apps = pm.getInstalledApplications(0);
                for (ApplicationInfo appInfo : apps) {
                    if (isGameApp(appInfo, pm)) {
                        JSONObject game = new JSONObject();
                        game.put("packageName", appInfo.packageName);
                        game.put("appName", pm.getApplicationLabel(appInfo).toString());
                        game.put("isInstalled", true);
                        game.put("isAdded", userGameList.contains(appInfo.packageName));
                        Drawable icon = pm.getApplicationIcon(appInfo);
                        String iconBase64 = drawableToBase64(icon);
                        game.put("iconBase64", iconBase64);
                        gamesArray.put(game);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return gamesArray.toString();
        }

        private boolean isGameApp(ApplicationInfo appInfo, PackageManager pm) {
            String[] gamePackages = {
                "com.dts.freefireth", "com.dts.freefiremax", "com.tencent.ig",
                "com.pubg.krmobile", "com.garena.game.kg", "com.mobile.legends",
                "com.supercell.clashofclans", "com.supercell.brawlstars",
                "com.roblox.client", "com.mojang.minecraftpe",
                "com.activision.callofduty.shooter", "com.riotgames.league.wildrift",
                "com.tencent.tmgp.sgame", "com.tencent.tmgp.pubgmhd",
                "com.gameloft.android.ANMP.GloftA9HM", "com.gameloft.android.ANMP.GloftA8HM",
                "com.ea.games.simsmobile", "com.supercell.clashroyale",
                "com.netease.heartpro", "com.dragonnest", "com.vng.pubgmobile",
                "com.tencent.tmgp.cod", "com.pubg.imobile"
            };
            for (String pkg : gamePackages) {
                if (appInfo.packageName.equals(pkg)) return true;
            }
            return false;
        }

        @JavascriptInterface
        public void addGame(final String packageName) {
            handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!userGameList.contains(packageName)) {
                            userGameList.add(packageName);
                            showToast("✅ Game berhasil ditambahkan!");
                        } else {
                            showToast("⚠️ Game sudah ada di daftar");
                        }
                    }
                });
        }

        @JavascriptInterface
        public void removeGame(final String packageName) {
            handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (userGameList.contains(packageName)) {
                            userGameList.remove(packageName);
                            showToast("🗑️ Game dihapus dari daftar");
                        }
                    }
                });
        }

        @JavascriptInterface
        public String getUserGames() {
            JSONArray gamesArray = new JSONArray();
            PackageManager pm = getPackageManager();
            try {
                synchronized (userGameList) {
                    List<String> listCopy = new ArrayList<String>(userGameList);
                    for (String pkg : listCopy) {
                        try {
                            ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
                            JSONObject game = new JSONObject();
                            game.put("packageName", pkg);
                            game.put("appName", pm.getApplicationLabel(appInfo).toString());
                            Drawable icon = pm.getApplicationIcon(appInfo);
                            String iconBase64 = drawableToBase64(icon);
                            game.put("iconBase64", iconBase64);
                            gamesArray.put(game);
                        } catch (PackageManager.NameNotFoundException e) {
                            userGameList.remove(pkg);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return gamesArray.toString();
        }

        @JavascriptInterface
        public void openGame(final String packageName) {
            handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
                            if (intent != null) {
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                showToast("🎮 Membuka game...");
                            } else {
                                showToast("❌ Game tidak ditemukan!");
                            }
                        } catch (Exception e) {
                            showToast("❌ Gagal membuka game!");
                        }
                    }
                });
        }

        @JavascriptInterface
        public void openFreeFire() {
            handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent intent = getPackageManager().getLaunchIntentForPackage("com.dts.freefireth");
                            if (intent == null) intent = getPackageManager().getLaunchIntentForPackage("com.dts.freefiremax");

                            if (intent != null) {
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                showToast("🎮 Membuka Free Fire...");
                            } else {
                                Intent playStore = new Intent(Intent.ACTION_VIEW);
                                playStore.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.dts.freefireth"));
                                playStore.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(playStore);
                                showToast("❌ Free Fire tidak terinstall!");
                            }
                        } catch (Exception e) {
                            showToast("❌ Gagal membuka Free Fire!");
                        }
                    }
                });
        }

        private String drawableToBase64(Drawable drawable) {
            try {
                android.graphics.Bitmap bitmap = null;
                if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
                    bitmap = ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
                } else {
                    int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 128;
                    int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 128;
                    if (width > 128) {
                        height = (int) (height * (128.0 / width));
                        width = 128;
                    }
                    bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
                    android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawable.draw(canvas);
                }
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 80, baos);
                byte[] imageBytes = baos.toByteArray();
                return android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public void copyToClipboard(final String text) {
            handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("text", text);
                        clipboard.setPrimaryClip(clip);
                        showToast("📋 Text disalin: " + text);
                    }
                });
        }

        @JavascriptInterface
        public String getClipboardText() {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard.hasPrimaryClip()) {
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                return item.getText().toString();
            }
            return "";
        }
    }
}
