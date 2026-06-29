package com.whatsap.whatunban;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import rikka.shizuku.IShizukuService;
import rikka.shizuku.IRemoteProcess;

public class ShizukuHelper {

    private static final String PROVIDER_AUTHORITY = "moe.shizuku.privileged.api";
    private static final Uri PROVIDER_URI = Uri.parse("content://" + PROVIDER_AUTHORITY);

    private static IShizukuService sService;

    public static synchronized IShizukuService getService(final Context context) {
        if (sService != null) {
            try {
                if (sService.asBinder().isBinderAlive()) {
                    return sService;
                }
            } catch (Exception ignored) {}
        }

        try {
            // First attempt: Standard "getBinder" call
            Bundle bundle = context.getContentResolver().call(PROVIDER_URI, "getBinder", null, null);
            if (bundle != null) {
                IBinder binder = bundle.getBinder("binder");
                if (binder == null) {
                    binder = bundle.getBinder("moe.shizuku.privileged.api.intent.extra.BINDER");
                }

                if (binder != null) {
                    sService = IShizukuService.Stub.asInterface(binder);
                    return sService;
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ShizukuHelper", "getService call failed", e);
        }

        // Second attempt: Direct Binder retrieval via Binder object in bundle (if any other method exists)
        // Shizuku V11+ usually responds to 'getBinder' but visibility is the main blocker.

        return null;
    }

    public static boolean isRunning(Context context) {
        IShizukuService service = getService(context);
        if (service == null) return false;
        try {
            // Test if we can actually call a method
            int version = service.getVersion();
            return version >= 11;
        } catch (Exception e) {
            android.util.Log.e("ShizukuHelper", "isRunning error", e);
            return false;
        }
    }

    public static boolean checkSelfPermission(Context context) {
        IShizukuService service = getService(context);
        if (service == null) return false;
        try {
            return service.checkSelfPermission();
        } catch (RemoteException e) {
            return false;
        }
    }

    public static void requestPermission(int requestCode, Context context) {
        IShizukuService service = getService(context);
        if (service == null) return;
        try {
            service.requestPermission(requestCode);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static int executeCommand(String command, Context context) throws Exception {
        IShizukuService service = getService(context);
        if (service == null) throw new Exception("Shizuku service not found");

        IRemoteProcess process = service.newProcess(new String[]{"sh", "-c", command}, null, null);
        if (process == null) throw new Exception("Failed to start process");

        int exitCode = process.waitFor();
        process.destroy();
        return exitCode;
    }

    public static String getDiagnostics(Context context) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("Checking Shizuku Package...\n");
            try {
                android.content.pm.PackageInfo pi = context.getPackageManager().getPackageInfo("moe.shizuku.privileged.api", 0);
                sb.append("✅ Package found: ").append(pi.versionName).append("\n");
            } catch (Exception e) {
                sb.append("❌ Package 'moe.shizuku.privileged.api' NOT FOUND\n");
                return sb.toString();
            }

            sb.append("Checking Shizuku Provider...\n");

            // Check authority resolution
            try {
                android.content.pm.ProviderInfo pri = context.getPackageManager().resolveContentProvider(PROVIDER_AUTHORITY, 0);
                if (pri == null) {
                    sb.append("❌ Authority NOT resolvable. (Visibility issue?)\n");
                } else {
                    sb.append("✅ Authority resolved to: ").append(pri.packageName).append("\n");
                }
            } catch (Exception e) {
                sb.append("⚠️ Authority resolution check error: ").append(e.getMessage()).append("\n");
            }

            Bundle bundle = null;
            try {
                bundle = context.getContentResolver().call(PROVIDER_URI, "getBinder", null, null);
            } catch (Exception e) {
                sb.append("❌ Provider call failed: ").append(e.getMessage()).append("\n");
                if (e.getMessage() != null && (e.getMessage().contains("Unknown authority") || e.getMessage().contains("Failed to find provider"))) {
                    sb.append("👉 Possible cause: Shizuku is not running or missing <queries> in Manifest.\n");
                }
            }

            if (bundle == null) {
                sb.append("❌ Provider call returned NULL. (Is Shizuku started?)\n");
            } else {
                IBinder binder = bundle.getBinder("binder");
                if (binder == null) {
                    sb.append("⚠️ Key 'binder' is null, trying alt key...\n");
                    binder = bundle.getBinder("moe.shizuku.privileged.api.intent.extra.BINDER");
                }

                if (binder == null) {
                    sb.append("❌ All binder keys are NULL\n");
                } else {
                    sb.append("✅ Binder obtained successfully!\n");
                    sb.append("Descriptor: ").append(binder.getInterfaceDescriptor()).append("\n");

                    IShizukuService svc = IShizukuService.Stub.asInterface(binder);
                    try {
                        int v = svc.getVersion();
                        sb.append("✅ Shizuku Version: ").append(v).append("\n");
                        sb.append("✅ UID: ").append(svc.getUid()).append("\n");
                        sb.append("✅ Permission: ").append(svc.checkSelfPermission() ? "GRANTED" : "DENIED").append("\n");
                    } catch (Exception e) {
                        sb.append("❌ Method call failed: ").append(e.getMessage()).append("\n");
                    }
                }
            }
        } catch (Exception e) {
            sb.append("❌ Critical Error: ").append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }
}
