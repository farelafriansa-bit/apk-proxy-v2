package moe.shizuku.server;

import moe.shizuku.server.IRemoteProcess;
import android.content.Intent;
import android.os.Bundle;

interface IShizukuService {

    int getVersion() = 2;

    int getUid() = 3;

    int checkPermission(String permission) = 4;

    IRemoteProcess newProcess(in String[] cmd, in String[] env, in String dir) = 7;

    String getSELinuxContext() = 8;

    String getSystemProperty(in String name, in String defaultValue) = 9;

    void setSystemProperty(in String name, in String value) = 10;

    void requestPermission(int requestCode) = 14;

    boolean checkSelfPermission() = 15;

    boolean shouldShowRequestPermissionRationale() = 16;

    void exit() = 100;

    oneway void dispatchPackageChanged(in Intent intent) = 102;

    boolean isHidden(int uid) = 103;

    int getFlagsForUid(int uid, int mask) = 105;

    void updateFlagsForUid(int uid, int mask, int value) = 106;
}
