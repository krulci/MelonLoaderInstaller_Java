using SharpAdbClient;
using System.Text;
using WatsonWebsocket;

namespace LemonADBBridge
{
    internal static class UninstallationHandler
    {
        private static WatsonWsClient wsClient;
        private static AdbClient adbClient;
        private static DeviceData deviceData;

        private static string? packageToUninstall;

        public static async Task Run(AdbClient client, DeviceData data, MainForm mainForm)
        {
            deviceData = data;
            adbClient = client;

            adbClient.RemoveAllForwards(deviceData);
            adbClient.CreateForward(deviceData, "tcp:9000", "tcp:9000", true);

            mainForm.statusText.Text = "ATTEMPTING CONNECTION...";

            wsClient = new("localhost", 9000, false);
            wsClient.KeepAliveInterval = 1000;
            wsClient.MessageReceived += OnMessageReceived;
            await wsClient.StartAsync();

            while (!wsClient.Connected)
            {
                await Task.Delay(500);
            }

            mainForm.statusText.Text = "CONNECTED";

            while (packageToUninstall == null)
            {
                await Task.Delay(500);
            }

            var receiver = new ConsoleOutputReceiver();

            adbClient.ExecuteRemoteCommand($"mv /sdcard/Android/data/{packageToUninstall} /sdcard/Android/data/{packageToUninstall}_BACKUP", deviceData, receiver);
            adbClient.ExecuteRemoteCommand($"mv /sdcard/Android/obb/{packageToUninstall} /sdcard/Android/obb/{packageToUninstall}_BACKUP", deviceData, receiver);

            adbClient.ExecuteRemoteCommand("pm uninstall " + packageToUninstall, deviceData, receiver);

            adbClient.ExecuteRemoteCommand($"mv /sdcard/Android/data/{packageToUninstall}_BACKUP /sdcard/Android/data/{packageToUninstall}", deviceData, receiver);
            adbClient.ExecuteRemoteCommand($"mv /sdcard/Android/obb/{packageToUninstall}_BACKUP /sdcard/Android/obb/{packageToUninstall}", deviceData, receiver);

            await wsClient.SendAsync(new byte[] { 1 });

            adbClient.RemoveForward(deviceData, 9000);

            mainForm.statusText.Text = "COMPLETE";
        }

        private static void OnMessageReceived(object? sender, MessageReceivedEventArgs e)
        {
            packageToUninstall = Encoding.UTF8.GetString(e.Data);
        }
    }
}
