package com.melonloader.installer.splitapksupport;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SplitAPKInstaller
{
    public static final String TAG = "melonloader";
    private static Context context;
    private static PackageInstaller packageInstaller;

    // Adapted from
    // https://github.com/Aefyr/SAI/blob/55505d231b1390e824d1cc0c8f4fa35fd4677105/app/src/main/java/com/aefyr/sai/installer/rootless/RootlessSAIPackageInstaller.java#L73
    public static boolean Install(String[] files, Context ctx) {
        context = ctx;
        packageInstaller = ctx.getPackageManager().getPackageInstaller();
        PackageInstaller.Session session = null;
        try {
            PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                sessionParams.setInstallReason(PackageManager.INSTALL_REASON_USER);

            int sessionID = packageInstaller.createSession(sessionParams);
            session = packageInstaller.openSession(sessionID);

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