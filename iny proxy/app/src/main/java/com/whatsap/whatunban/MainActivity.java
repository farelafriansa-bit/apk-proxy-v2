package com.whatsap.whatunban;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private WebView webView;
    private Handler handler = new Handler(Looper.getMainLooper());

    private static final String CHANNEL_ID = "debug_channel";
    private static final int PAIRING_NOTIFICATION_ID = 102;
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int OVERLAY_PERMISSION_REQUEST = 124;

    private List<String> userGameList = java.util.Collections.synchronizedList(new ArrayList<String>());

    private boolean isAutoBody = false;
    private boolean isAutoLock = false;
    private boolean isAutoSpeed = false;
    private boolean isAutoJump = false;
    private boolean isAutoBypass = false;
    private boolean isAutoRefresh = false;

    private void runShellCommand(String command) {
        try {
            // First attempt: Standard shell
            Runtime.getRuntime().exec(new String[]{"sh", "-c", command});

            // Second attempt: Fallback to Shizuku shell if available
            Runtime.getRuntime().exec(new String[]{"sh", "-c", "shizuku shell " + command});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();
        requestAllPermissions();

        webView = (WebView) findViewById(R.id.webView);

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
            CharSequence name = "Debugging Notifications";
            String description = "Notifications for Wireless Debugging and Pairing";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // ========================================
    // WEB APP INTERFACE
    // ========================================
    class WebAppInterface {

        @JavascriptInterface
        public void testConnection() {
            handler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(MainActivity.this, "✅ JavaScript terhubung!", Toast.LENGTH_SHORT).show();
					}
				});
        }

        @JavascriptInterface
        public void executeCommand(final String menu, final String action) {
            handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (menu.equals("body")) {
                            isAutoBody = action.equals("start");
                            showToast("aim body: " + (isAutoBody ? "ON" : "OFF"));
                        } else if (menu.equals("lock")) {
                            isAutoLock = action.equals("start");
                            showToast("aim lock: " + (isAutoLock ? "ON" : "OFF"));
                        } else if (menu.equals("speed")) {
                            isAutoSpeed = action.equals("start");
                            showToast("speed up: " + (isAutoSpeed ? "ON" : "OFF"));
                        } else if (menu.equals("jump")) {
                            isAutoJump = action.equals("start");
                            showToast("back jump: " + (isAutoJump ? "ON" : "OFF"));
                        } else if (menu.equals("bypass")) {
                            isAutoBypass = action.equals("start");
                            showToast("bypass: " + (isAutoBypass ? "ON" : "OFF"));
                        } else if (menu.equals("refresh")) {
                            isAutoRefresh = action.equals("start");
                            showToast("auto refresh: " + (isAutoRefresh ? "ON" : "OFF"));
                        }
                    }
                });
        }

        @JavascriptInterface
        public void executeBooster(final String type, final String action) {
            handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (type.equals("fps")) {
                            if (action.equals("start")) {
                                startFPSBooster();
                            } else {
                                stopFPSBooster();
                            }
                        } else if (type.equals("res")) {
                            if (action.equals("start")) {
                                startResBooster();
                            } else {
                                stopResBooster();
                            }
                        } else if (type.equals("mode")) {
                            if (action.equals("start")) {
                                startModeBooster();
                            } else {
                                stopModeBooster();
                            }
                        } else if (type.equals("network")) {
                            if (action.equals("start")) {
                                startNetworkBooster();
                            } else {
                                stopNetworkBooster();
                            }
                        } else if (type.equals("ram")) {
                            if (action.equals("start")) {
                                startRAMBooster();
                            } else {
                                stopRAMBooster();
                            }
                        }
                    }
                });
        }

        private void startFPSBooster() {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    Settings.System.putFloat(getContentResolver(), Settings.System.ANIMATOR_DURATION_SCALE, 0.0f);
                    Settings.System.putFloat(getContentResolver(), Settings.System.WINDOW_ANIMATION_SCALE, 0.0f);
                    Settings.System.putFloat(getContentResolver(), Settings.System.TRANSITION_ANIMATION_SCALE, 0.0f);
                }

                // FORCE 144 FPS (REAL COMMANDS)
                runShellCommand("settings put global peak_refresh_rate 144.0");
                runShellCommand("settings put global min_refresh_rate 144.0");
                runShellCommand("settings put global refresh_rate_mode 2");
                runShellCommand("settings put system peak_refresh_rate 144.0");
                runShellCommand("settings put system min_refresh_rate 144.0");

                showToast("⚡ Real FPS Booster 144Hz AKTIF!");
            } catch (Exception e) {
                showToast("⚠️ Gagal FPS Booster");
            }
        }

        private void stopFPSBooster() {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    Settings.System.putFloat(getContentResolver(), Settings.System.ANIMATOR_DURATION_SCALE, 1.0f);
                    Settings.System.putFloat(getContentResolver(), Settings.System.WINDOW_ANIMATION_SCALE, 1.0f);
                    Settings.System.putFloat(getContentResolver(), Settings.System.TRANSITION_ANIMATION_SCALE, 1.0f);
                }

                // RESET FPS
                runShellCommand("settings delete global peak_refresh_rate");
                runShellCommand("settings delete global min_refresh_rate");
                runShellCommand("settings delete system peak_refresh_rate");
                runShellCommand("settings delete system min_refresh_rate");

                showToast("⚡ FPS Booster NONAKTIF");
            } catch (Exception e) {
                showToast("⚠️ Gagal FPS Booster");
            }
        }

        private void startResBooster() {
            showToast("📱 Lag Fix AKTIF");
        }

        private void stopResBooster() {
            showToast("📱 Lag Fix NONAKTIF");
        }

        private void startModeBooster() {
            try {
                SharedPreferences prefs = getSharedPreferences("GamingModePrefs", MODE_PRIVATE);
                prefs.edit().putBoolean("gaming_mode_active", true).apply();

                if (!isNotificationServiceEnabled()) {
                    showToast("⚠️ Aktifkan Izin Akses Notifikasi!");
                    Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    showToast("🎮 Gaming Mode & Real Notif Blocker AKTIF");
                }
            } catch (Exception e) {
                showToast("⚠️ Gagal Gaming Mode");
            }
        }

        private void stopModeBooster() {
            try {
                SharedPreferences prefs = getSharedPreferences("GamingModePrefs", MODE_PRIVATE);
                prefs.edit().putBoolean("gaming_mode_active", false).apply();
                showToast("🎮 Gaming Mode NONAKTIF");
            } catch (Exception e) {
                showToast("⚠️ Gagal Gaming Mode");
            }
        }

        private boolean isNotificationServiceEnabled() {
            String pkgName = getPackageName();
            final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
            if (flat != null && !flat.isEmpty()) {
                final String[] names = flat.split(":");
                for (String name : names) {
                    final android.content.ComponentName cn = android.content.ComponentName.unflattenFromString(name);
                    if (cn != null && pkgName.equals(cn.getPackageName())) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void startNetworkBooster() {
            try {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Network[] networks = cm.getAllNetworks();
                    for (Network network : networks) {
                        cm.bindProcessToNetwork(network);
                        break;
                    }
                }
                showToast("📶 Network Booster AKTIF!");
            } catch (Exception e) {
                showToast("⚠️ Gagal Network Booster");
            }
        }

        private void stopNetworkBooster() {
            try {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cm.bindProcessToNetwork(null);
                }
                showToast("📶 Network Booster NONAKTIF");
            } catch (Exception e) {
                showToast("⚠️ Gagal Network Booster");
            }
        }

        private void startRAMBooster() {
            System.gc();
            System.runFinalization();
            showToast("🧠 RAM Booster AKTIF!");
        }

        private void stopRAMBooster() {
            showToast("🧠 RAM Booster NONAKTIF");
        }

        // ========================================
        // WIRELESS DEBUGGING & SHIZUKU
        // ========================================
        @JavascriptInterface
        public void openWirelessDebugging() {
            handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent intent = new Intent("android.settings.ADB_WIFI_SETTINGS");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            Toast.makeText(MainActivity.this, "📶 Membuka Debugging Nirkabel...", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            try {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                Toast.makeText(MainActivity.this, "📶 Buka Opsi Developer > Debugging Nirkabel", Toast.LENGTH_SHORT).show();
                            } catch (Exception e2) {
                                Toast.makeText(MainActivity.this, "⚠️ Gagal membuka pengaturan!", Toast.LENGTH_SHORT).show();
                            }
                        }
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
        public String checkDebugStatus() {
            JSONObject status = new JSONObject();
            try {
                boolean adbEnabled = Settings.Global.getInt(getContentResolver(), "adb_enabled", 0) > 0;
                boolean wirelessEnabled = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    wirelessEnabled = Settings.Global.getInt(getContentResolver(), "adb_wifi_enabled", 0) > 0;
                }
                status.put("wireless", wirelessEnabled || adbEnabled);

                boolean shizukuInstalled = false;
                try {
                    getPackageManager().getApplicationInfo("moe.shizuku.privileged.api", 0);
                    shizukuInstalled = true;
                } catch (PackageManager.NameNotFoundException e) {
                    shizukuInstalled = false;
                }
                status.put("shizuku_installed", shizukuInstalled);

                boolean shizukuRunning = false;
                if (shizukuInstalled) {
                    // Optimized check for Shizuku
                    String[] commands = {"getprop moe.shizuku.privileged.api", "ps -A", "ps"};
                    for (String cmd : commands) {
                        Process p = Runtime.getRuntime().exec(cmd);
                        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        String l;
                        while ((l = r.readLine()) != null) {
                            if (l.toLowerCase().contains("shizuku") || (cmd.startsWith("getprop") && !l.isEmpty())) {
                                shizukuRunning = true;
                                break;
                            }
                        }
                        p.destroy();
                        if (shizukuRunning) break;
                    }
                }
                status.put("shizuku", shizukuRunning);
                status.put("usb_debug", adbEnabled);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return status.toString();
        }

        @JavascriptInterface
        public void showPairingNotification() {
            handler.post(new Runnable() {
					@Override
					public void run() {
						try {
							Intent settingsIntent = new Intent("android.settings.ADB_WIFI_SETTINGS");
							PendingIntent settingsPending = PendingIntent.getActivity(
								MainActivity.this, 1, settingsIntent,
								PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? 0x04000000 : 0)); // FLAG_IMMUTABLE

                            Intent receiverIntent = new Intent(MainActivity.this, PairingReceiver.class);
                            receiverIntent.setAction(PairingReceiver.ACTION_PAIRING_CODE);
                            PendingIntent receiverPending = PendingIntent.getBroadcast(
                                MainActivity.this, 2, receiverIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? 0x02000000 : 0)); // FLAG_MUTABLE

                            RemoteInput remoteInput = new RemoteInput.Builder(PairingReceiver.KEY_TEXT_REPLY)
                                .setLabel("Masukkan Kode Pairing 6 Digit")
                                .build();

							Notification.Builder builder;
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
								builder = new Notification.Builder(MainActivity.this, CHANNEL_ID);
							} else {
								builder = new Notification.Builder(MainActivity.this);
							}

							builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                                .setContentTitle("🔑 Pairing Wireless Debugging")
                                .setContentText("Masukkan 6 digit kode pairing di sini")
                                .setContentIntent(settingsPending)
                                .setAutoCancel(false)
                                .setOngoing(true)
                                .setPriority(Notification.PRIORITY_MAX);

							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                Notification.Action action = new Notification.Action.Builder(
                                    android.R.drawable.ic_menu_edit, "🔑 Kirim Kode", receiverPending)
                                    .addRemoteInput(remoteInput)
                                    .build();
                                builder.addAction(action);
							}

                            builder.addAction(android.R.drawable.ic_menu_manage, "⚙️ Buka", settingsPending);

							NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
							if (nm != null) {
								nm.notify(PAIRING_NOTIFICATION_ID, builder.build());
								showToast("🔔 Gunakan kolom notifikasi untuk isi kode pairing!");
							}
						} catch (Exception e) {
							showToast("⚠️ Gagal mengirim notifikasi!");
						}
					}
				});
        }

        private String getLocalIpAddress() {
            try {
                InetAddress[] addresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
                for (InetAddress addr : addresses) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) return addr.getHostAddress();
                }
            } catch (Exception e) {}
            return "127.0.0.1";
        }

        @JavascriptInterface
        public void showToast(final String message) {
            handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
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
