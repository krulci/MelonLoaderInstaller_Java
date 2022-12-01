package com.melonloader.installer.splitapksupport;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

public class SplitAPKService extends Service {
    private static final String TAG = "melonloader";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999);
        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                Log.d(TAG, "Requesting user confirmation for installation");
                Intent confirmationIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(confirmationIntent);
                } catch (Exception e) {
                }
                break;
            default:
                Log.d(TAG, getErrorString(status));
                break;
        }
        stopSelf();
        return START_NOT_STICKY;
    }

    public String getErrorString(int status) {
        switch (status) {
            case PackageInstaller.STATUS_FAILURE_ABORTED:
                return "Installation was cancelled by user";
            case PackageInstaller.STATUS_FAILURE_BLOCKED:
                return "Installation was blocked by device";
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
                return "Unable to install the app because it conflicts with an already installed app with same package name";
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                return "Application is incompatible with this device";
            case PackageInstaller.STATUS_FAILURE_INVALID:
                return "Invalid APK files selected";
            case PackageInstaller.STATUS_FAILURE_STORAGE:
                return "Not enough storage space to install the app";
        }
        return "Installation failed";
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
