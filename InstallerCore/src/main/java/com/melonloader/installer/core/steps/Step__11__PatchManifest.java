package com.melonloader.installer.core.steps;

import com.melonloader.installer.core.InstallerStep;
import android2.content.Context;
import com.bigzhao.xml2axml.Encoder;
import com.bigzhao.xml2axml.test.AXMLPrinter;
import com.melonloader.installer.core.Properties;
import com.melonloader.installer.core.ZipHelper;
import org.apache.commons.io.FileUtils;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Step__11__PatchManifest extends InstallerStep {
    public boolean Run() throws Exception {
        // Only thing the manifest patching does is fix a splitting specific issue
        if (!properties.isSplit)
            return true;

        properties.logger.Log("Extracting manifest");

        Path manifestPath = Paths.get(properties.tempDir, "AndroidManifest.xml");
        Path tempManifestPath = Paths.get(properties.tempDir, "AndroidManifest.dec.xml");

        ZipHelper zipHelper = new ZipHelper(paths.outputAPK.toString());
        zipHelper.QueueExtract("AndroidManifest.xml", manifestPath.toString());
        zipHelper.Extract();

        properties.logger.Log("Patching manifest");

        // TODO: fix read/write of string
        decode(manifestPath.toString(), tempManifestPath.toString());
        String manifestText = properties.readerWriter.readFile(tempManifestPath.toString());
        if (manifestText.contains("extractNativeLibs=\"false\""))
            manifestText = manifestText.replace("extractNativeLibs=\"false\"", "extractNativeLibs=\"true\"");
        else
        {
            // There is nothing to edit, use original
            Files.delete(tempManifestPath);
            return true;
        }

        properties.readerWriter.writeFile(tempManifestPath.toString(), manifestText);
        try {
            encode(tempManifestPath.toString(), manifestPath.toString());
        } catch (XmlPullParserException e) {
            properties.logger.Log(e.getMessage());
            // I'm not sure if this is really needed since sometimes patching the manifest is unneeded
            return false;
        }

        Files.delete(tempManifestPath);

        return true;
    }

    public static void encode(String in,String out) throws IOException, XmlPullParserException {
        Encoder e = new Encoder();
        byte[] bs = e.encodeFile(null, in);
        FileUtils.writeByteArrayToFile(new File(out), bs);
    }

    public static void decode(String in,String out) throws FileNotFoundException {
        AXMLPrinter.out=new PrintStream(new File(out));
        AXMLPrinter.main(new String[]{in});
        AXMLPrinter.out.close();
    }
}