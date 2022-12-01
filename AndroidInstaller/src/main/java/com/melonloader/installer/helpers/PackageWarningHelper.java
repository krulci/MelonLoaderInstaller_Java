package com.melonloader.installer.helpers;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

// This class does double-duty as the code that gets package warnings as well as checking if
// the user is able to connect to GitHub, which is required for installation
public class PackageWarningHelper {
    public static Map<String, String> AvailableWarnings;

    public static final String TAG = "melonloader";
    public static boolean Run() {
        Log.i(TAG, "Retrieving warnings from GitHub");
        try {
            URL url = new URL("https://raw.githubusercontent.com/TrevTV/MelonLoaderInstaller/master/package_warnings.json");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == connection.HTTP_OK) {
                // Read response
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Convert to map
                try {
                    AvailableWarnings = new Gson().fromJson(response.toString(), new TypeToken<Map<String, String>>() {}.getType());
                }
                catch (JsonSyntaxException ex)
                {
                    // Assuming the issue here is I changed the syntax or something
                    AvailableWarnings = new HashMap<>();
                    return true;
                }
            } else {
                Log.e(TAG, "Could not retrieve warnings from GitHub, cannot run!");
                return false;
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }

        return true;
    }
}
