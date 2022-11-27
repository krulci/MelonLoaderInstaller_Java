package com.melonloader.installer.splitapksupport;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

public class SplitAPKService extends Service {
    private static final String TAG = "melonloader";
    public static final String ACTION_INSTALLATION_STATUS_NOTIFICATION = "com.aefyr.sai.action.INSTALLATION_STATUS_NOTIFICATION";
    public static final String EXTRA_INSTALLATION_STATUS = "com.aefyr.sai.extra.INSTALLATION_STATUS";
    public static final String EXTRA_SESSION_ID = "com.aefyr.sai.extra.SESSION_ID";
    public static final String EXTRA_PACKAGE_NAME = "com.aefyr.sai.extra.PACKAGE_NAME";
    public static final String EXTRA_ERROR_DESCRIPTION = "com.aefyr.sai.extra.ERROR_DESCRIPTION";

    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_CONFIRMATION_PENDING = 1;
    public static final int STATUS_FAILURE = 2;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999);
        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                Log.d(TAG, "Requesting user confirmation for installation");
                sendStatusChangeBroadcast(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), STATUS_CONFIRMATION_PENDING, intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME));
                Intent confirmationIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);

                ConfirmationIntentWrapperActivity.start(this, intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), confirmationIntent);
                break;
            case PackageInstaller.STATUS_SUCCESS:
                Log.d(TAG, "Installation succeed");
                sendStatusChangeBroadcast(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), STATUS_SUCCESS, intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME));
                break;
            default:
                Log.d(TAG, "Installation failed");
                sendErrorBroadcast(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), getErrorString(status, intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)));
                break;
        }
        stopSelf();
        return START_NOT_STICKY;
    }

    private void sendStatusChangeBroadcast(int sessionID, int status, String packageName) {
        Intent statusIntent = new Intent(ACTION_INSTALLATION_STATUS_NOTIFICATION);
        statusIntent.putExtra(EXTRA_INSTALLATION_STATUS, status);
        statusIntent.putExtra(EXTRA_SESSION_ID, sessionID);

        if (packageName != null)
            statusIntent.putExtra(EXTRA_PACKAGE_NAME, packageName);

        sendBroadcast(statusIntent);
    }

    private void sendErrorBroadcast(int sessionID, String error) {
        Intent statusIntent = new Intent(ACTION_INSTALLATION_STATUS_NOTIFICATION);
        statusIntent.putExtra(EXTRA_INSTALLATION_STATUS, STATUS_FAILURE);
        statusIntent.putExtra(EXTRA_SESSION_ID, sessionID);
        statusIntent.putExtra(EXTRA_ERROR_DESCRIPTION, error);

        sendBroadcast(statusIntent);
    }

    public String getErrorString(int status, String blockingPackage) {
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
