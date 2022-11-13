package com.melonloader.installer.adbbridge;

import android.util.Log;
import com.melonloader.installer.Callable;
import dev.gustavoavila.websocketclient.WebSocketClient;

import java.net.URI;

public class ADBBridgeListener extends WebSocketClient {
    private Callable callable;

    /**
     * Initialize all the variables
     *
     * @param uri URI of the WebSocket server
     */
    public ADBBridgeListener(URI uri, Callable callable1) {
        super(uri);
        callable = callable1;
    }

    @Override
    public void onOpen() {
    }

    @Override
    public void onTextReceived(String message) {

    }

    @Override
    public void onBinaryReceived(byte[] data) {
        if (data[0] == 1)
            callable.call();
    }

    @Override
    public void onPingReceived(byte[] data) {

    }

    @Override
    public void onPongReceived(byte[] data) {

    }

    @Override
    public void onException(Exception e) {

    }

    @Override
    public void onCloseReceived() {

    }
}