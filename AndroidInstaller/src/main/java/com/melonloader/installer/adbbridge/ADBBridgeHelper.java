package com.melonloader.installer.adbbridge;

import com.melonloader.installer.Callable;
import java.net.UnknownHostException;

public class ADBBridgeHelper
{
    private static ADBBridgeSocket bridgeSocket;
    public static ADBBridgeSocket getBridgeSocket() {
        return bridgeSocket;
    }

    public static void AttemptConnect(String packageName, Callable afterConnect)
    {
        try {
            bridgeSocket = new ADBBridgeSocket(9000, afterConnect, packageName);
            bridgeSocket.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void Cancel() {
        if (bridgeSocket != null) {
            try {
                bridgeSocket.stop();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
