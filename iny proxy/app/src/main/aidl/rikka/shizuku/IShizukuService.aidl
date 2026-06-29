package rikka.shizuku;

import rikka.shizuku.IRemoteProcess;
import android.content.Intent;
import android.os.Bundle;

interface IShizukuService {

    // Transaction ID: 1 (Reserved/Skipped usually by Shizuku)
    void dummy1();

    // Transaction ID: 2
    int getVersion();

    // Transaction ID: 3
    int getUid();

    // Transaction ID: 4
    int checkPermission(String permission);

    // Transaction ID: 5
    void dummy5();

    // Transaction ID: 6
    void dummy6();

    // Transaction ID: 7
    IRemoteProcess newProcess(in String[] cmd, in String[] env, in String dir);

    // Transaction ID: 8
    String getSELinuxContext();

    // Transaction ID: 9
    String getSystemProperty(in String name, in String defaultValue);

    // Transaction ID: 10
    void setSystemProperty(in String name, in String value);

    // Transaction ID: 11
    void dummy11();

    // Transaction ID: 12
    void dummy12();

    // Transaction ID: 13
    void dummy13();

    // Transaction ID: 14
    void requestPermission(int requestCode);

    // Transaction ID: 15
    boolean checkSelfPermission();

    // Transaction ID: 16
    boolean shouldShowRequestPermissionRationale();
}
