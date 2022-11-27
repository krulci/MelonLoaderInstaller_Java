package com.melonloader.installer;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;

import com.melonloader.installer.core.Main;
import com.melonloader.installer.helpers.UnityVersionDetector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class UnityApplicationData {
    private ApplicationInfo application;
    public Drawable icon;
    public boolean patched;
    public boolean supported;
    public String appName;
    public String apkLocation;
    public String packageName;
    public String unityVersion;
    private boolean getVersionAttempted = false;
    private AssetManager assetManager;

    public UnityApplicationData(PackageManager pm, ApplicationInfo info)
    {
        application = info;
        icon = info.loadIcon(pm);
        appName = info.packageName;
        packageName = info.packageName;
        apkLocation = info.publicSourceDir;

        try {
            assetManager = pm.getResourcesForApplication(info).getAssets();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        CheckPatched();
        CheckSupported();
    }

    public void CheckSupported() { supported = application.nativeLibraryDir.contains("arm64");  }

    public void CheckPatched() { patched = Main.IsPatched(application.publicSourceDir); }

    public void TryDetectVersion(String tempDir)
    {
        TryDetectVersion(tempDir, () -> {});
    }

    public void TryDetectVersion(String tempDir, Runnable callback)
    {
        if (getVersionAttempted)
            return;

        getVersionAttempted = true;

        if (assetManager == null)
        {
            Log.e("MelonLoader", "Cannot find asset manager for (" + packageName + ")");
            return;
        }

        try {
            Files.createDirectories(Paths.get(tempDir));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        UnityVersionDetector detector = new UnityVersionDetector(assetManager);

        AsyncTask.execute(() -> {
            unityVersion = detector.TryGetVersion();

            if (unityVersion == null) {
                return;
            }

            callback.run();
        });
    }
}
