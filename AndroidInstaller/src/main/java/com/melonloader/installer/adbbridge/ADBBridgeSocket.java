package com.melonloader.installer.adbbridge;

import android.app.AlertDialog;
import com.melonloader.installer.Callable;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class ADBBridgeSocket extends WebSocketServer {
    public AlertDialog alertDialog;
    private Callable callable;
    private String packageName;

    public ADBBridgeSocket(int port, Callable callable1, String packageName1) throws UnknownHostException {
        super(new InetSocketAddress(port));
        callable = callable1;
        packageName = packageName1;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.send(packageName);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {

    }

    @Override
    public void onMessage(WebSocket conn, String message) {

    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        if (message.get() == 1) {
            if (alertDialog != null) alertDialog.dismiss();
            callable.call();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

    }

    @Override
    public void onStart() {

    }
}