package com.melonloader.installer.adbbridge;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.util.Log;
import com.melonloader.installer.Callable;
import com.melonloader.installer.helpers.FileHelper;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicBoolean;

public class ADBBridgeHelper
{
    private static final boolean useSocket = false;
    private static ADBBridgeSocket bridgeSocket;
    private static AlertDialog alertDialog;

    // Non-socket specific
    private static boolean shouldDie;
    public static LocalTime taskStartTime;
    public static LocalTime lastCheckTime;

    public static void AttemptConnect(String filesDir, String packageName, Callable afterConnect)
    {
        taskStartTime = null;
        if (useSocket) {
            try {
                bridgeSocket = new ADBBridgeSocket(9000, afterConnect, packageName);
                bridgeSocket.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        else  {
            shouldDie = false;
            File tempFile = Paths.get(filesDir, "temp", "adbbridge.txt").toFile();
            FileHelper.writeFile(tempFile.getAbsolutePath(), packageName);
            taskStartTime = LocalTime.now();
            AsyncTask.execute(() -> {
                while (tempFile.exists()) {
                    if (shouldDie)
                        return;

                    lastCheckTime = LocalTime.now();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        //throw new RuntimeException(e);
                    }
                }

                 Finalize(afterConnect);
            });
        }
    }

    private static void Finalize(Callable afterConnect) {
        alertDialog.dismiss();
        afterConnect.call();
    }

    public static AlertDialog GetDialog() {
        return alertDialog;
    }

    public static void SetDialog(AlertDialog dialog) {
        alertDialog = dialog;
        if (useSocket) {
            bridgeSocket.alertDialog = dialog;
        }
    }

    public static void Kill() {
        if (useSocket) {
            if (bridgeSocket != null) {
                try {
                    bridgeSocket.stop();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else {
            shouldDie = true;
        }
    }
}
