package com.melonloader.installer.core;

// For some reason Android hates Files.readString and Files.writeString
// but instead of hardcoding Android specific code, I'm using an interface
public interface FileReaderWriter {
    String readFile(String path);
    void writeFile(String path, String data);
}
