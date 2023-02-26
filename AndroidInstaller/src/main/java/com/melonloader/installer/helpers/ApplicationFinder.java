package com.melonloader.installer.helpers;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import com.melonloader.installer.UnityApplicationData;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ApplicationFinder {
    private static final String TAG = "melonloader";

    public static List<UnityApplicationData> GetSupportedApplications(Context context)
    {
        final PackageManager pm = context.getPackageManager();

        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        List<UnityApplicationData> applicationDatas = new ArrayList<>();

        for (ApplicationInfo packageInfo : packages) {
            File nativeLibDir = Paths.get(packageInfo.nativeLibraryDir).toFile();

            if (!nativeLibDir.exists())
                continue;

            File[] files = nativeLibDir.listFiles();

            boolean isUnity = Arrays.stream(files).anyMatch(f -> f.getName().contains("libunity.so"));
            if (!isUnity)
                continue;

            applicationDatas.add(new UnityApplicationData(pm, packageInfo));
        }

        return applicationDatas;
    }

    public static UnityApplicationData GetPackage(Context context, String packageName) throws PackageManager.NameNotFoundException {
        final PackageManager pm = context.getPackageManager();

        ApplicationInfo packageInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);

        return new UnityApplicationData(pm, packageInfo);
    }
}
