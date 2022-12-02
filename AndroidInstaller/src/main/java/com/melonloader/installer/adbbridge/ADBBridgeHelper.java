package com.melonloader.installer.adbbridge;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;
import com.melonloader.installer.Callable;
import com.melonloader.installer.R;
import com.melonloader.installer.helpers.FileHelper;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ADBBridgeHelper
{
    private static final boolean useSocket = false;
    private static ADBBridgeSocket bridgeSocket;
    private static AlertDialog alertDialog;
    private static boolean shouldDie;

    public static void AttemptConnect(String filesDir, String packageName, Callable afterConnect)
    {
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
            AsyncTask.execute(() -> {
                while (tempFile.exists()) {
                    if (shouldDie)
                        return;

                    //Log.i("melonloader", "looping");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                alertDialog.dismiss();
                afterConnect.call();
            });
        }
    }

    public static void SetDialog(AlertDialog dialog) {
        if (useSocket) {
            bridgeSocket.alertDialog = dialog;
        } else {
            alertDialog = dialog;
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
