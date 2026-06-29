package com.whatsap.whatunban;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import moe.shizuku.server.IShizukuService;
import moe.shizuku.server.IRemoteProcess;

public class ShizukuHelper {

    private static final String PROVIDER_AUTHORITY = "moe.shizuku.privileged.api";
    private static final Uri PROVIDER_URI = Uri.parse("content://" + PROVIDER_AUTHORITY);

    private static IShizukuService sService;

    public static synchronized IShizukuService getService(Context context) {
        if (sService != null && sService.asBinder().isBinderAlive()) {
            return sService;
        }

        try {
            Bundle bundle = context.getContentResolver().call(PROVIDER_URI, "getBinder", null, null);
            if (bundle != null) {
                IBinder binder = bundle.getBinder("binder");
                if (binder != null) {
                    sService = IShizukuService.Stub.asInterface(binder);
                    return sService;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isRunning(Context context) {
        IShizukuService service = getService(context);
        if (service == null) return false;
        try {
            return service.getVersion() >= 11;
        } catch (RemoteException e) {
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
}
