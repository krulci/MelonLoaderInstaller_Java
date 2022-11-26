using SharpAdbClient;
using Websocket.Client;

namespace LemonADBBridge
{
    internal static class UninstallationHandler
    {
        private static WebsocketClient wsClient;
        private static AdbClient adbClient;
        private static DeviceData deviceData;

        private static string? packageToUninstall;

        public static async Task Run(AdbClient client, DeviceData data, MainForm mainForm)
        {
            deviceData = data;
            adbClient = client;

            adbClient.RemoveAllForwards(deviceData);
            adbClient.CreateForward(deviceData, "tcp:9000", "tcp:9000", true);

            mainForm.statusText.Text = "WAITING FOR CONNECTION...";

            wsClient = new WebsocketClient(new Uri("ws://localhost:9000"));
            wsClient.MessageReceived.Subscribe(e => packageToUninstall = e.Text);

            wsClient.ErrorReconnectTimeout = TimeSpan.FromSeconds(3);
            wsClient.ReconnectTimeout = TimeSpan.FromSeconds(3);
            wsClient.IsReconnectionEnabled = true;
            await wsClient.Start();

            mainForm.statusText.Text = "CONNECTED";

            while (packageToUninstall == null)
            {
                await Task.Delay(500);
            }

            mainForm.statusText.Text = packageToUninstall;

            var receiver = new ConsoleOutputReceiver();

            adbClient.ExecuteRemoteCommand($"mv /sdcard/Android/data/{packageToUninstall} /sdcard/Android/data/{packageToUninstall}_BACKUP", deviceData, receiver);
            adbClient.ExecuteRemoteCommand($"mv /sdcard/Android/obb/{packageToUninstall} /sdcard/Android/obb/{packageToUninstall}_BACKUP", deviceData, receiver);

            adbClient.ExecuteRemoteCommand("pm uninstall " + packageToUninstall, deviceData, receiver);

            adbClient.ExecuteRemoteCommand($"mv /sdcard/Android/data/{packageToUninstall}_BACKUP /sdcard/Android/data/{packageToUninstall}", deviceData, receiver);
            adbClient.ExecuteRemoteCommand($"mv /sdcard/Android/obb/{packageToUninstall}_BACKUP /sdcard/Android/obb/{packageToUninstall}", deviceData, receiver);

            await wsClient.NativeClient.SendAsync(new byte[] { 1 }, System.Net.WebSockets.WebSocketMessageType.Binary, true, default).ConfigureAwait(false);

            adbClient.RemoveForward(deviceData, 9000);

            mainForm.statusText.Text = "COMPLETE";

            Dispose();
        }

        public static void Dispose()
        {
            try
            {
                adbClient.KillAdb();
            }
            catch { }
            wsClient.Dispose();
        }
    }
}
