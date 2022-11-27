package com.melonloader.installer.splitapksupport;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import com.melonloader.installer.activites.MainActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SplitAPKInstaller
{
    public static final String TAG = "melonloader";
    private static Context context;
    private static PackageInstaller mPackageInstaller;

    // This function and the other 2 classes in this package were modified or directly stolen from
    // https://github.com/Aefyr/SAI
    public static boolean Install(String[] files, Context ctx) {
        context = ctx;
        mPackageInstaller = ctx.getPackageManager().getPackageInstaller();
        PackageInstaller.Session session = null;
        try {
            PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            sessionParams.setInstallLocation(0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                sessionParams.setInstallReason(PackageManager.INSTALL_REASON_USER);

            int sessionID = mPackageInstaller.createSession(sessionParams);

            session = mPackageInstaller.openSession(sessionID);
            int currentApkFile = 0;
            for (String filePath : files) {
                File file = new File(filePath);
                Log.d(TAG, file.getAbsolutePath());
                try (InputStream inputStream = new FileInputStream(file); OutputStream outputStream = session.openWrite(String.format("%d.apk", currentApkFile++), 0, file.length())) {
                    copyStream(inputStream, outputStream);
                    session.fsync(outputStream);
                }
            }

            Intent callbackIntent = new Intent(context, SplitAPKService.class);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0, callbackIntent, 0);
            session.commit(pendingIntent.getIntentSender());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (session != null)
                session.close();
        }

        return true;
    }

    private static void copyStream(InputStream from, OutputStream to) throws IOException {
        byte[] buf = new byte[1024 * 1024];
        int len;
        while ((len = from.read(buf)) > 0) {
            to.write(buf, 0, len);
        }
    }
}