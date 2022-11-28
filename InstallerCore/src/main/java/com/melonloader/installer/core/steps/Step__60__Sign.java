package com.melonloader.installer.core.steps;

import com.android.apksigner.ApkSignerTool;
import com.melonloader.installer.core.InstallerStep;
import com.melonloader.installer.core.LogOutputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class Step__60__Sign extends InstallerStep {
    @Override
    public boolean Run() throws Exception {
        return Align(paths.outputAPK) && Align(paths.libraryAPK) && Sign();
    }

    private boolean Sign() throws Exception {
        properties.logger.Log("Signing [" + paths.outputAPK + "]");

        PrintStream oldStream = System.out;
        PrintStream oldStreamErr = System.err;

        if (isAndroid()) {
            OutputStream stream = new LogOutputStream(this.properties.logger);
            System.setOut(new PrintStream(stream));
            System.setErr(new PrintStream(stream));
        }

        ApkSignerTool.main(new String[] {
            "sign",
            "--ks",
            paths.keystore.toString(),
            "--ks-key-alias",
            "cert",
            "--ks-pass",
            "pass:" + properties.keystorePass,
            paths.outputAPK.toString()
        });

        if (properties.isSplit) {
            properties.logger.Log("Signing [" + paths.libraryAPK + "]");
            ApkSignerTool.main(new String[] {
                    "sign",
                    "--ks",
                    paths.keystore.toString(),
                    "--ks-key-alias",
                    "cert",
                    "--ks-pass",
                    "pass:" + properties.keystorePass,
                    paths.libraryAPK.toString()
            });
        }

        if (isAndroid()) {
            System.out.flush();
            System.setOut(oldStream);
            System.setErr(oldStreamErr);
        }

        return true;
    }

    boolean isAndroid() {
        try {
            Class.forName("the class name");
            return true;
        } catch(ClassNotFoundException e) {
            return false;
        }
    }

    private boolean Align(Path path) throws IOException, InterruptedException {
        // Chances are this is a non-split APK, so it's complaining about there not having a separate lib APK
        if (path == null)
            return true;

        properties.logger.Log("Aligning [" + path + "]");

        String alignedFilename = path.toString() + "-aligned";

        Process process = null;
        process = Runtime.getRuntime().exec(new String[] {
                properties.zipAlign,
                "-v",
                "-f",
                "4",
                path.toString(),
                alignedFilename
        });

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(process.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(process.getErrorStream()));

        String s = null;
        while ((s = stdInput.readLine()) != null) {
            properties.logger.Log(s);
        }

        stdInput.close();
        stdError.close();

        process.waitFor();

        Files.delete(path);
        Files.move(Paths.get(alignedFilename), path);

        Files.deleteIfExists(Paths.get(alignedFilename));

        return true;
    }
}
