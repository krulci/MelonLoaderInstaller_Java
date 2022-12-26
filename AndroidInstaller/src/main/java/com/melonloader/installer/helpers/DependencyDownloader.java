package com.melonloader.installer.helpers;

import android.util.Log;
import com.melonloader.installer.activites.ViewApplication;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class DependencyDownloader
{
    public static boolean Run(String output, ViewApplication.LoggerHelper loggerHelper)
    {
        loggerHelper.Log("Retrieving release info from GitHub");
        try {
            URL url = new URL("https://api.github.com/repos/LemonLoader/MelonLoader/releases/latest");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == connection.HTTP_OK) {
                // Create a reader with the input stream reader.
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject jsonObject = new JSONObject(response.toString());
                JSONArray jsonArray = jsonObject.getJSONArray("assets");

                for (int i = 0; i < jsonArray.length(); i++)
                {
                    JSONObject object = jsonArray.getJSONObject(i);
                    if (object.getString("name").startsWith("installer_deps"))
                    {
                        downloadFile(object.getString("browser_download_url"), output, loggerHelper);
                        break;
                    }
                }
            } else {
                loggerHelper.Log("Could not retrieve release info from GitHub, aborting install!");
                return false;
            }
        }
        catch (Exception ex)
        {
            return false;
        }

        return true;
    }

    protected static void downloadFile(String _url, String _output, ViewApplication.LoggerHelper loggerHelper) throws IOException {
        loggerHelper.Log("Downloading [" + _url + "]");

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

        loggerHelper.Log("Done");
    }
}