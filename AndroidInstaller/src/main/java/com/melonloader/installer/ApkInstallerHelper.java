package com.melonloader.installer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Trace;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.android.apksig.ApkVerifier;
import com.melonloader.installer.adbbridge.ADBBridgeHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Signature;
import java.util.List;

public class ApkInstallerHelper {
    Activity context;
    String packageName;
    String lastInstallPath;

    Integer installLoopCount;

    String pending = null;
    Runnable next = null;
    Callable afterInstall = null;

    public ApkInstallerHelper(Activity _context, String _packageName)
    {
        context = _context;
        packageName = _packageName;
        installLoopCount = 0;
    }

    public void InstallApk(String path, Callable doAfterInstall)
    {
        afterInstall = doAfterInstall;
        next = () -> InternalInstall(path);
        UninstallPackage();
    }

    protected void InternalInstall(String path)
    {
        lastInstallPath = path;
        AsyncTask.execute(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }

            context.runOnUiThread(() -> {
                Uri filePath = uriFromFile(context, new File(path));

                Intent install = new Intent(Intent.ACTION_VIEW);
                install.setDataAndType(filePath, "application/vnd.android.package-archive");

                install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                install.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try {
                    context.startActivityForResult(install, 2000);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    Log.e("TAG", "Error in opening the file!");
                }
            });
        });
    }

    private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    protected void UninstallPackage()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setTitle("ADB Bridge")
                .setMessage("Do you want to connect to the Lemon ADB Bridge® to save game data and OBBs, if they exist?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.i("MelonLoader", "Using ADBBridge");
                        ADBBridgeHelper.AttemptConnect(packageName, new Callable() {
                            @Override
                            public void call() {
                                onActivityResult(1000, 0, null);
                            }

                            @Override
                            public void callOnFail() {}
                        });
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.i("MelonLoader", "Not using ADBBridge");
                        context.runOnUiThread(() -> {
                            pending = Intent.ACTION_DELETE;

                            Intent intent = new Intent(Intent.ACTION_DELETE);
                            intent.setData(Uri.parse("package:" + packageName));
                            context.startActivityForResult(intent, 1000);
                        });
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info);

        AlertDialog alert = builder.create();
        alert.setCancelable(false);
        alert.show();
    }

    private static Uri uriFromFile(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
        } else {
            return Uri.fromFile(file);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        Log.e("melonloader", "" + requestCode + " " + resultCode);
//        if (!data.getAction().equals(pending))
//            return;

        if (requestCode == 1000)
        {
            pending = null;

            if (next != null)
                next.run();

            if (afterInstall != null
            )
                afterInstall.call();
            next = null;
        }
        if (requestCode == 2000)
        {
            if (!isPackageInstalled(packageName, context.getPackageManager())) {
                if (installLoopCount >= 3) {
                    afterInstall.callOnFail();
                    return;
                }

                Log.i("melonloader", "Package not installed, attempting installation");
                installLoopCount++;
                InternalInstall(lastInstallPath);
            }
        }
    }
}
