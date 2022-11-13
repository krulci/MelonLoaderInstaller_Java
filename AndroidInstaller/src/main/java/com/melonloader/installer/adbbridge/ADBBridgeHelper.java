package com.melonloader.installer.adbbridge;

import com.melonloader.installer.Callable;
import dev.gustavoavila.websocketclient.WebSocketClient;

import java.net.URI;
import java.net.URISyntaxException;

public class ADBBridgeHelper
{
    public static void AttemptConnect(String packageName, Callable afterConnect)
    {
        URI uri;
        try {
            uri = new URI("ws://localhost:9000");
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        WebSocketClient webSocketClient = new ADBBridgeListener(uri, afterConnect);

        webSocketClient.setConnectTimeout(10000);
        webSocketClient.setReadTimeout(60000);
        webSocketClient.enableAutomaticReconnection(5000);
        webSocketClient.connect();
        webSocketClient.send(packageName);
    }
}
