package com.melonloader.installer.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileHelper {
    public static String readFile(String path) {
        File file = new File(path);
        int length = (int) file.length();
        byte[] bytes = new byte[length];

        try {
            FileInputStream in = new FileInputStream(file);
            in.read(bytes);
            in.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String contents = new String(bytes);
        return contents;
    }

    public static void writeFile(String path, String data) {
        try {
            FileOutputStream stream = new FileOutputStream(path);
            stream.write(data.getBytes());
            stream.close();
        }
        catch (Exception ex) {
        }
    }
}
