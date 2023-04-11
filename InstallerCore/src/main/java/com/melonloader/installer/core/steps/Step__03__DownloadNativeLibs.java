package com.melonloader.installer.core.steps;

import com.melonloader.installer.core.InstallerStep;
import com.melonloader.installer.core.ZipHelper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Step__03__DownloadNativeLibs extends InstallerStep {
    @Override
    public boolean Run() throws Exception {
        String packageName = paths.base.getFileName().toString();
        properties.logger.Log("Checking for extra native libraries [" + packageName + "]");
        try {
            Path path = Paths.get(properties.tempDir, "extraLibraries.zip");
            downloadFile("https://github.com/LemonLoader/NativeLibraries/raw/main/" + packageName + ".zip", path.toString());
            properties.logger.Log("Extracting Libraries");

            ZipHelper zipHelper = new ZipHelper(path.toString());
            List<String> files = zipHelper.GetFiles();

            for (String file : files) {
                zipHelper.QueueExtract(file, Paths.get(paths.dependenciesDir.toString(), "native", file).toString());
            }

            zipHelper.Extract();
        }
        catch (Exception ex)
        {
            properties.logger.Log("No extra libraries found");
        }

        return true;
    }

    protected void downloadFile(String _url, String _output) throws IOException {
        properties.logger.Log("Downloading [" + _url + "]");

        URL url = new URL(_url);
        URLConnection connection = url.openConnection();
        connection.connect();

        int lenghtOfFile = connection.getContentLength();

        // download the file
        InputStream input = new BufferedInputStream(url.openStream(),
                8192);

        // Output stream
        OutputStream output = new FileOutputStream(_output);

        byte data[] = new byte[1024];

        int count;
        while ((count = input.read(data)) != -1) {
            output.write(data, 0, count);
        }

        output.flush();

        // closing streams
        output.close();
        input.close();
    }
}
