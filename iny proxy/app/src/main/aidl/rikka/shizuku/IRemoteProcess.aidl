package rikka.shizuku;

import android.os.ParcelFileDescriptor;

interface IRemoteProcess {

    ParcelFileDescriptor getOutputStream();

    ParcelFileDescriptor getInputStream();

    ParcelFileDescriptor getErrorStream();

    int waitFor();

    int exitValue();

    void destroy();

    boolean alive();
}
