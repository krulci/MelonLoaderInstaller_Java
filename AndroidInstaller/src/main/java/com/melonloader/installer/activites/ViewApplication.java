package com.melonloader.installer.activites;

import android.content.pm.ApplicationInfo;
import android.os.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.melonloader.installer.*;
import com.melonloader.installer.core.FileReaderWriter;
import com.melonloader.installer.core.ILogger;
import com.melonloader.installer.core.Main;
import com.melonloader.installer.core.Properties;
import com.melonloader.installer.helpers.*;
import com.melonloader.installer.splitapksupport.SplitAPKInstaller;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ViewApplication extends AppCompatActivity implements View.OnClickListener {
    private UnityApplicationData application;
    private LoggerHelper loggerHelper;
    private ApkInstallerHelper installerHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_application);

        String targetPackageName = getIntent().getStringExtra("target.packageName");
        if (targetPackageName == null) {
            finish();
            return;
        }

        try {
            application = ApplicationFinder.GetPackage(this, targetPackageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            finish();
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        CheckWarnings(targetPackageName);
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();

        Log.e("melonloader", "on create");

        ImageView appIcon = findViewById(R.id.applicationIcon);
        TextView appName = findViewById(R.id.applicationName);
        Button patchButton = findViewById(R.id.patchButton);
        patchButton.setOnClickListener(this);
        //patchButton.setEnabled(!application.patched);
        patchButton.setText(application.patched ? "REPATCH" : "PATCH");

        appIcon.setImageDrawable(application.icon);
        appName.setText(application.appName);

        loggerHelper = new LoggerHelper(this);
        Main._properties.logger = loggerHelper;
    }

    public void CheckWarnings(String targetPackageName)
    {
        String warning = PackageWarningHelper.AvailableWarnings.getOrDefault(targetPackageName, null);
        if (warning != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder
                    .setTitle("Warning")
                    .setMessage(warning)
                    .setIcon(android.R.drawable.ic_dialog_alert);

            AlertDialog alert = builder.create();
            alert.setCancelable(false);
            alert.show();
            new CountDownTimer(3500,1000) {

                @Override
                public void onTick(long arg0) {}

                @Override
                public void onFinish() {
                    builder.setPositiveButton("Understood", null);
                    builder.create().show();
                    alert.dismiss();
                }
            }.start();
        }
    }

    private void requestWritePermission()
    {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onClick(View view) {
        StartPatching();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id==android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void StartPatching()
    {
        loggerHelper.Clear();

        String MelonLoaderBase = getExternalFilesDir(null).toString();

        String depsLocation = Paths.get(MelonLoaderBase, "temp", "dependencies.zip").toString();
        String unityAssetsLocation = Paths.get(MelonLoaderBase, "temp", "unity.zip").toString();
        String etcLocation = Paths.get(MelonLoaderBase, "temp", "il2cpp_etc.zip").toString();

        String zipAlignLocation = Paths.get(getFilesDir().toString(), "ml-zipalign").toString();

        Button patchButton = findViewById(R.id.patchButton);
        Path tempPath = Paths.get(MelonLoaderBase, "temp", application.appName);

        String PublishedBase = Paths.get(Environment.getExternalStorageDirectory().getPath().toString(), "MelonLoader").toString();
        try {
            Files.createDirectories(Paths.get(PublishedBase));
        } catch (IOException e) {
            e.printStackTrace();
            loggerHelper.Log("ERROR: " + e.toString());
            PublishedBase = MelonLoaderBase;
        }

        String finalPublishedBase = PublishedBase;
        AsyncTask.execute(() -> {
            runOnUiThread(() -> {
                ActionBar actionBar = getSupportActionBar();
                actionBar.setDisplayHomeAsUpEnabled(false);
                patchButton.setEnabled(false);
                patchButton.setText("PATCHING");
            });

            // Deletes all contents of the temp path, useful if the install has failed before
            deleteFolder(tempPath.toFile());

            loggerHelper.Log("Build Directory: [" + MelonLoaderBase + "]");

            loggerHelper.Log("Preparing Assets");

            boolean isDebuggable =  ( 0 != ( getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE ) );

            loggerHelper.Log("IsDebuggable: " + isDebuggable);
            if (isDebuggable)
                copyAssets("installer_deps.zip", depsLocation);
            else
                DependencyDownloader.Run(depsLocation, loggerHelper);

            copyAssets("zipalign", zipAlignLocation);
            copyAssets("il2cpp_etc.zip", etcLocation);

            loggerHelper.Log("Preparing Exectables");
            makeExecutable(zipAlignLocation);

            loggerHelper.Log("Starting patch");

            String libFileName = Paths.get(application.libApkLocation).getFileName().toString();

            File baseDir = Paths.get(finalPublishedBase, application.packageName).toFile();
            if (!baseDir.exists())
                baseDir.mkdir();

            String outputApkScoped = Paths.get(tempPath.toString(), "base.apk").toString();
            String libApkScoped = Paths.get(tempPath.toString(), libFileName).toString();
            String outputApk = Paths.get(baseDir.toString(), "base.apk").toString();
            String libOutApk = Paths.get(baseDir.toString(), libFileName).toString();

            boolean success = Main.Run(new Properties() {{
                targetApk = application.apkLocation;
                libraryApk = application.libApkLocation;
                isSplit = application.split;
                outputApk = outputApkScoped;
                tempDir = tempPath.toString();
                logger = loggerHelper;
                dependencies = depsLocation;
                il2cppEtc = etcLocation;
                zipAlign = zipAlignLocation;
                readerWriter = new FileReaderWriter() {
                    @Override
                    public String readFile(String path) {
                        return FileHelper.readFile(path);
                    }

                    @Override
                    public void writeFile(String path, String data) {
                        FileHelper.writeFile(path, data);
                    }
                };
            }});

            if (success) {
                try {
                    requestWritePermission();
                    Files.move(Paths.get(outputApkScoped), Paths.get(outputApk), StandardCopyOption.REPLACE_EXISTING);
                    if (application.split)
                        Files.move(Paths.get(libApkScoped), Paths.get(libOutApk), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                    loggerHelper.Log(e.toString());
                    loggerHelper.Log("using fallback folder");
                    outputApk = outputApkScoped;
                    libOutApk = libApkScoped;
                }

                loggerHelper.Log("Application Successfully patched. Reinstalling.");

                String finalOutputApk = outputApk;
                String finalLibOutApk = libOutApk;
                runOnUiThread(() -> {
                    if (!application.split) {
                        InstallSingle(patchButton, finalOutputApk);
                    }
                    else {
                        InstallSplit(patchButton, finalOutputApk, finalLibOutApk);
                    }
                });
            }
            else
            {
                runOnUiThread(() -> {
                    ActionBar actionBar = getSupportActionBar();
                    actionBar.setDisplayHomeAsUpEnabled(true);
                    patchButton.setText("FAILED");

                    loggerHelper.scroller.fullScroll(ScrollView.FOCUS_DOWN);
                });
            }
        });
    }

    private void InstallSingle(Button patchButton, String finalOutputApk) {
        installerHelper = new ApkInstallerHelper(this, application.appName);
        installerHelper.InstallApk(Paths.get(finalOutputApk).toString(), new Callable() {
            @Override
            public void call() {
                ActionBar actionBar = getSupportActionBar();
                actionBar.setDisplayHomeAsUpEnabled(true);
                patchButton.setText("PATCHED");
                loggerHelper.scroller.fullScroll(ScrollView.FOCUS_DOWN);
            }

            @Override
            public void callOnFail() {
                ActionBar actionBar = getSupportActionBar();
                actionBar.setDisplayHomeAsUpEnabled(true);
                patchButton.setText("FAILED");

                loggerHelper.scroller.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void InstallSplit(Button patchButton, String finalOutputApk, String libraryApk) {
        Log.i("melonloader", "SPLIT - " + finalOutputApk + " - " + libraryApk);

        installerHelper = new ApkInstallerHelper(this, application.appName);
        installerHelper.UninstallPackage(new Runnable() {
            @Override
            public void run() {
                SplitAPKInstaller.Install(new String[]{
                        finalOutputApk,
                        libraryApk
                }, getApplicationContext());

                ActionBar actionBar = getSupportActionBar();
                actionBar.setDisplayHomeAsUpEnabled(true);
                patchButton.setText("PATCHED");
                loggerHelper.scroller.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void copyAssets(String assetName, String dest) {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            loggerHelper.Log("Failed to get asset file list. -> " + e.toString());
        }

        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(assetName);
            File outFile = new File(dest);
            out = new FileOutputStream(outFile);
            copyFile(in, out);

            in.close();
            in = null;

            out.flush();
            out.close();
            out = null;
        } catch(IOException e) {
            loggerHelper.Log("Failed to copy asset file: " + assetName + " -> " + e);
        }
    }

    private boolean makeExecutable(String path)
    {
        File myFile = new File(path);

        if (!myFile.canExecute()) {
            loggerHelper.Log("[" + path + "] Trying to make executable.");
            if (!myFile.setExecutable(true)) {
                loggerHelper.Log("[" + path + "] Failed to make exectuable.");
                return false;
            };
        }

        return true;
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    // https://stackoverflow.com/a/7768086
    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    public class LoggerHelper implements ILogger {
        TextView content;
        ScrollView scroller;
        boolean dirty = false;

        public LoggerHelper(Activity context)
        {
            content = context.findViewById(R.id.loggerBody);
            scroller = context.findViewById(R.id.loggerScroll);
            content.setText("");
        }

        public void Clear()
        {
            runOnUiThread(() -> {
                content.setText("");
            });
        }

        public void Log(String msg)
        {
            Log.i("melonloader", msg);

            runOnUiThread(() -> {
                if (dirty)
                    content.append("\n");
                else
                    dirty = true;

                content.append(msg);
                scroller.fullScroll(ScrollView.FOCUS_DOWN);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (installerHelper != null)
            installerHelper.onActivityResult(requestCode, resultCode, data);
    }

}