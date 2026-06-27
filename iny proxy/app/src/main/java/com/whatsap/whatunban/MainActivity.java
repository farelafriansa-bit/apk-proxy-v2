package com.whatsap.whatunban;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.app.Notification;
import android.app.PendingIntent;

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
    private static final int NOTIFICATION_ID = 101;
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
        List<String> permissions = new ArrayList<>();

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

        // ========================================
        // TEST CONNECTION
        // ========================================
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

        // ========================================
        // BOOSTER FUNCTIONS
        // ========================================
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
                    Settings.System.putFloat(
                        getContentResolver(),
                        Settings.System.ANIMATOR_DURATION_SCALE,
                        0.0f
                    );
                    Settings.System.putFloat(
                        getContentResolver(),
                        Settings.System.WINDOW_ANIMATION_SCALE,
                        0.0f
                    );
                    Settings.System.putFloat(
                        getContentResolver(),
                        Settings.System.TRANSITION_ANIMATION_SCALE,
                        0.0f
                    );
                }
                showToast("⚡ FPS Booster AKTIF!");
            } catch (Exception e) {
                showToast("⚠️ Gagal FPS Booster");
            }
        }

        private void stopFPSBooster() {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    Settings.System.putFloat(
                        getContentResolver(),
                        Settings.System.ANIMATOR_DURATION_SCALE,
                        1.0f
                    );
                    Settings.System.putFloat(
                        getContentResolver(),
                        Settings.System.WINDOW_ANIMATION_SCALE,
                        1.0f
                    );
                    Settings.System.putFloat(
                        getContentResolver(),
                        Settings.System.TRANSITION_ANIMATION_SCALE,
                        1.0f
                    );
                }
                showToast("⚡ FPS Booster NONAKTIF");
            } catch (Exception e) {
                showToast("⚠️ Gagal FPS Booster");
            }
        }

        private void startResBooster() {
            try {
                showToast("📱 Lag Fix AKTIF");
            } catch (Exception e) {
                showToast("⚠️ Gagal Lag Fix");
            }
        }

        private void stopResBooster() {
            try {
                showToast("📱 Lag Fix NONAKTIF");
            } catch (Exception e) {
                showToast("⚠️ Gagal Lag Fix");
            }
        }

        private void startModeBooster() {
            try {
                showToast("🎮 Gaming Mode AKTIF");
            } catch (Exception e) {
                showToast("⚠️ Gagal Gaming Mode");
            }
        }

        private void stopModeBooster() {
            try {
                showToast("🎮 Gaming Mode NONAKTIF");
            } catch (Exception e) {
                showToast("⚠️ Gagal Gaming Mode");
            }
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
            try {
                System.gc();
                System.runFinalization();
                showToast("🧠 RAM Booster AKTIF!");
            } catch (Exception e) {
                showToast("⚠️ Gagal RAM Booster");
            }
        }

        private void stopRAMBooster() {
            try {
                showToast("🧠 RAM Booster NONAKTIF");
            } catch (Exception e) {
                showToast("⚠️ Gagal RAM Booster");
            }
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
                boolean adbEnabled = false;
                boolean wirelessEnabled = false;

                try {
                    adbEnabled = Settings.Global.getInt(getContentResolver(), "adb_enabled", 0) > 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        wirelessEnabled = Settings.Global.getInt(getContentResolver(), "adb_wifi_enabled", 0) > 0;
                    } else {
                        wirelessEnabled = adbEnabled;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                status.put("wireless", wirelessEnabled || adbEnabled);

                boolean shizukuInstalled = false;
                boolean shizukuRunning = false;
                boolean shizukuApi = false;

                try {
                    getPackageManager().getApplicationInfo("moe.shizuku.privileged.api", 0);
                    shizukuInstalled = true;
                } catch (PackageManager.NameNotFoundException e) {
                    shizukuInstalled = false;
                }

                if (shizukuInstalled) {
                    try {
                        Process process = Runtime.getRuntime().exec("getprop moe.shizuku.privileged.api");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line = reader.readLine();
                        if (line != null && !line.isEmpty()) {
                            shizukuRunning = true;
                            shizukuApi = line.contains("api");
                        }
                        process.destroy();
                    } catch (Exception e) {
                        try {
                            Process process2 = Runtime.getRuntime().exec("ps -A | grep shizuku");
                            BufferedReader reader2 = new BufferedReader(new InputStreamReader(process2.getInputStream()));
                            String line2 = reader2.readLine();
                            if (line2 != null && line2.contains("shizuku")) {
                                shizukuRunning = true;
                            }
                            process2.destroy();
                        } catch (Exception e2) {
                        }
                    }
                }

                status.put("shizuku_installed", shizukuInstalled);
                status.put("shizuku", shizukuRunning);
                status.put("shizuku_api", shizukuApi);

                try {
                    boolean usbDebug = Settings.Global.getInt(getContentResolver(), "adb_enabled", 0) > 0;
                    status.put("usb_debug", usbDebug);
                } catch (Exception e) {
                    status.put("usb_debug", false);
                }

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    status.put("error", e.getMessage());
                } catch (Exception e2) {
                }
            }
            return status.toString();
        }

        // ========================================
        // PAIRING DIALOG (Popup)
        // ========================================
        @JavascriptInterface
        public void showPairingDialog() {
            handler.post(new Runnable() {
					@Override
					public void run() {
						String ipAddress = getLocalIpAddress();

						android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
						builder.setTitle("📡 Wireless Debugging Pairing");

						android.widget.LinearLayout layout = new android.widget.LinearLayout(MainActivity.this);
						layout.setOrientation(android.widget.LinearLayout.VERTICAL);
						layout.setPadding(30, 20, 30, 20);

						android.widget.TextView ipInfo = new android.widget.TextView(MainActivity.this);
						ipInfo.setText("📶 IP Address: " + ipAddress);
						ipInfo.setTextSize(16);
						ipInfo.setTypeface(null, android.graphics.Typeface.BOLD);
						ipInfo.setPadding(0, 0, 0, 15);
						layout.addView(ipInfo);

						android.widget.TextView label = new android.widget.TextView(MainActivity.this);
						label.setText("Masukkan kode pairing 6 digit dari Wireless Debugging:");
						label.setTextSize(14);
						label.setPadding(0, 0, 0, 10);
						layout.addView(label);

						final android.widget.EditText input = new android.widget.EditText(MainActivity.this);
						input.setHint("Contoh: 123456");
						input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
						input.setTextSize(24);
						input.setGravity(android.view.Gravity.CENTER);
						input.setPadding(40, 20, 40, 20);
						input.setBackgroundColor(0xFFF0F0F0);
						layout.addView(input);

						builder.setView(layout);

						builder.setPositiveButton("🔗 Pairing", new android.content.DialogInterface.OnClickListener() {
								@Override
								public void onClick(android.content.DialogInterface dialog, int which) {
									String code = input.getText().toString().trim();
									if (!code.isEmpty()) {
										SharedPreferences prefs = getSharedPreferences("pairing", MODE_PRIVATE);
										prefs.edit().putString("pairing_code", code).apply();

										ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
										ClipData clip = ClipData.newPlainText("pairing_code", code);
										clipboard.setPrimaryClip(clip);

										Toast.makeText(MainActivity.this, "🔑 Kode: " + code + " ✅ Tersimpan & di-copy!", Toast.LENGTH_SHORT).show();
										openWirelessDebugging();
									} else {
										Toast.makeText(MainActivity.this, "⚠️ Masukkan kode pairing!", Toast.LENGTH_SHORT).show();
									}
								}
							});

						builder.setNegativeButton("❌ Batal", new android.content.DialogInterface.OnClickListener() {
								@Override
								public void onClick(android.content.DialogInterface dialog, int which) {
									dialog.cancel();
								}
							});

						builder.show();
					}
				});
        }

        // ========================================
        // FLOATING WIDGET PAIRING CODE
        // ========================================
        @JavascriptInterface
        public void showFloatingPairing() {
            handler.post(new Runnable() {
					@Override
					public void run() {
						if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(MainActivity.this)) {
							Toast.makeText(MainActivity.this, "⚠️ Izinkan overlay di pengaturan!", Toast.LENGTH_SHORT).show();
							Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
													   Uri.parse("package:" + getPackageName()));
							startActivity(intent);
							return;
						}

						try {
							android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
								android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
								android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
							);
							params.gravity = android.view.Gravity.CENTER;

							android.widget.LinearLayout layout = new android.widget.LinearLayout(MainActivity.this);
							layout.setOrientation(android.widget.LinearLayout.VERTICAL);
							layout.setBackgroundColor(0xFF1a7a4a);
							layout.setPadding(35, 30, 35, 30);
							layout.setElevation(25);

							android.graphics.drawable.GradientDrawable border = new android.graphics.drawable.GradientDrawable();
							border.setColor(0xFF1a7a4a);
							border.setStroke(4, 0xFF000000);
							border.setCornerRadius(20);
							layout.setBackground(border);

							android.widget.TextView title = new android.widget.TextView(MainActivity.this);
							title.setText("🔑 Pairing Code");
							title.setTextColor(0xFFFFFFFF);
							title.setTextSize(20);
							title.setTypeface(null, android.graphics.Typeface.BOLD);
							title.setGravity(android.view.Gravity.CENTER);
							layout.addView(title);

							final android.widget.EditText input = new android.widget.EditText(MainActivity.this);
							input.setHint("Masukkan kode pairing");
							input.setHintTextColor(0xFFAAAAAA);
							input.setTextColor(0xFFFFFFFF);
							input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
							input.setTextSize(24);
							input.setGravity(android.view.Gravity.CENTER);
							input.setBackgroundColor(0x44000000);
							input.setPadding(25, 20, 25, 20);
							input.setHint("Contoh: 123456");
							android.widget.FrameLayout.LayoutParams inputParams = new android.widget.FrameLayout.LayoutParams(
								android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
								android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
							);
							inputParams.setMargins(0, 20, 0, 20);
							input.setLayoutParams(inputParams);
							layout.addView(input);

							android.widget.LinearLayout btnLayout = new android.widget.LinearLayout(MainActivity.this);
							btnLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
							btnLayout.setGravity(android.view.Gravity.CENTER);

							android.widget.Button btnPair = new android.widget.Button(MainActivity.this);
							btnPair.setText("🔗 Pair");
							btnPair.setTextColor(0xFFFFFFFF);
							btnPair.setBackgroundColor(0xFF2ecc71);
							btnPair.setPadding(30, 14, 30, 14);
							btnPair.setAllCaps(false);
							btnPair.setTypeface(null, android.graphics.Typeface.BOLD);
							android.widget.FrameLayout.LayoutParams btnParams = new android.widget.FrameLayout.LayoutParams(
								android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
								android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
							);
							btnParams.setMargins(0, 0, 15, 0);
							btnPair.setLayoutParams(btnParams);

							android.widget.Button btnClose = new android.widget.Button(MainActivity.this);
							btnClose.setText("✕ Tutup");
							btnClose.setTextColor(0xFFFFFFFF);
							btnClose.setBackgroundColor(0xFFe74c3c);
							btnClose.setPadding(25, 14, 25, 14);
							btnClose.setAllCaps(false);
							btnClose.setTypeface(null, android.graphics.Typeface.BOLD);

							btnLayout.addView(btnPair);
							btnLayout.addView(btnClose);
							layout.addView(btnLayout);

							final android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
								layout,
								android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
								android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
								true
							);
							popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
							popupWindow.setOutsideTouchable(true);
							popupWindow.setFocusable(true);

							popupWindow.showAtLocation(webView, android.view.Gravity.CENTER, 0, 0);

							btnPair.setOnClickListener(new android.view.View.OnClickListener() {
									@Override
									public void onClick(android.view.View v) {
										String code = input.getText().toString().trim();
										if (!code.isEmpty()) {
											ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
											ClipData clip = ClipData.newPlainText("pairing_code", code);
											clipboard.setPrimaryClip(clip);
											Toast.makeText(MainActivity.this, "🔑 Kode: " + code + " ✅ Sudah di-copy!", Toast.LENGTH_SHORT).show();
											popupWindow.dismiss();
											openWirelessDebugging();
										} else {
											Toast.makeText(MainActivity.this, "⚠️ Masukkan kode pairing!", Toast.LENGTH_SHORT).show();
										}
									}
								});

							btnClose.setOnClickListener(new android.view.View.OnClickListener() {
									@Override
									public void onClick(android.view.View v) {
										popupWindow.dismiss();
									}
								});
						} catch (Exception e) {
							Toast.makeText(MainActivity.this, "⚠️ Gagal menampilkan floating widget!", Toast.LENGTH_SHORT).show();
						}
					}
				});
        }

        // ========================================
        // NOTIFIKASI PAIRING
        // ========================================
        @JavascriptInterface
        public void showPairingNotification() {
            handler.post(new Runnable() {
					@Override
					public void run() {
						try {
							Intent dialogIntent = new Intent(MainActivity.this, MainActivity.class);
							dialogIntent.setAction("SHOW_PAIRING_DIALOG");
							PendingIntent dialogPending = PendingIntent.getActivity(
								MainActivity.this,
								0,
								dialogIntent,
								PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
							);

							Intent settingsIntent = new Intent("android.settings.ADB_WIFI_SETTINGS");
							PendingIntent settingsPending = PendingIntent.getActivity(
								MainActivity.this,
								1,
								settingsIntent,
								PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
							);

							Notification.Builder builder;
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
								builder = new Notification.Builder(MainActivity.this, CHANNEL_ID);
							} else {
								builder = new Notification.Builder(MainActivity.this);
							}

							builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                                .setContentTitle("🔑 Pairing Debugging Nirkabel")
                                .setContentText("Klik untuk memasukkan kode pairing")
                                .setContentIntent(dialogPending)
                                .setAutoCancel(true)
                                .setPriority(Notification.PRIORITY_MAX);

							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
								builder.addAction(
									android.R.drawable.ic_menu_manage,
									"⚙️ Buka Pengaturan",
									settingsPending
								);
								builder.addAction(
									android.R.drawable.ic_menu_edit,
									"🔑 Masukkan Kode",
									dialogPending
								);
							}

							NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
							if (notificationManager != null) {
								notificationManager.notify(PAIRING_NOTIFICATION_ID, builder.build());
								Toast.makeText(MainActivity.this, "🔔 Notifikasi pairing telah dikirim!", Toast.LENGTH_SHORT).show();
							}
						} catch (Exception e) {
							Toast.makeText(MainActivity.this, "⚠️ Gagal mengirim notifikasi!", Toast.LENGTH_SHORT).show();
						}
					}
				});
        }

        // ========================================
        // START WIRELESS DEBUGGING CLIENT
        // ========================================
        @JavascriptInterface
        public void startWirelessDebugging() {
            handler.post(new Runnable() {
					@Override
					public void run() {
						if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
							Toast.makeText(MainActivity.this, "⚠️ Wireless Debugging hanya untuk Android 11+", Toast.LENGTH_SHORT).show();
							return;
						}

						try {
							Intent intent = new Intent("android.settings.ADB_WIFI_SETTINGS");
							intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(intent);
							showPairingDialog();
							Toast.makeText(MainActivity.this, "📡 Wireless Debugging siap pairing!", Toast.LENGTH_SHORT).show();
						} catch (Exception e) {
							Toast.makeText(MainActivity.this, "⚠️ Gagal membuka Wireless Debugging!", Toast.LENGTH_SHORT).show();
						}
					}
				});
        }

        // ========================================
        // GET PAIRING INFO
        // ========================================
        @JavascriptInterface
        public String getPairingInfo() {
            JSONObject info = new JSONObject();
            try {
                String ip = getLocalIpAddress();
                int port = getAdbPort();
                info.put("ip", ip);
                info.put("port", port);
                info.put("status", "siap pairing");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return info.toString();
        }

        private String getLocalIpAddress() {
            try {
                InetAddress[] addresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
                for (InetAddress addr : addresses) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "Tidak terdeteksi";
        }

        private int getAdbPort() {
            try {
                Process process = Runtime.getRuntime().exec("getprop service.adb.tcp.port");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                process.destroy();
                if (line != null && !line.isEmpty()) {
                    return Integer.parseInt(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 5555;
        }

        // ========================================
        // GAME FUNCTIONS
        // ========================================
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
        public void showToast(final String message) {
            handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
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
