using SharpAdbClient;
using System.Text;
using WatsonWebsocket;

namespace LemonADBBridge
{
    internal static class UninstallationHandler
    {
        private static WatsonWsServer wsServer;
        private static AdbClient adbClient;
        private static DeviceData deviceData;

        private static string? deviceIpPort;
        private static string? packageToUninstall;

        public static async Task Run(AdbClient client, DeviceData data, MainForm mainForm)
        {
            deviceData = data;
            adbClient = client;
            wsServer = new WatsonWsServer();
            wsServer.ClientConnected += OnClientConnected;
            wsServer.MessageReceived += OnMessageReceived;
            wsServer.Start();

            adbClient.RemoveReverseForward(deviceData, "tcp:9000");
            adbClient.CreateReverseForward(deviceData, "tcp:9000", "tcp:9000", true);

            mainForm.statusText.Text = "READY FOR CONNECTION";

            while (deviceIpPort == null)
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

            await wsServer.SendAsync(deviceIpPort, new byte[] { 1 });

            adbClient.RemoveReverseForward(deviceData, "tcp:9000");
        }

        private static void OnMessageReceived(object? sender, MessageReceivedEventArgs e)
        {
            packageToUninstall = Encoding.UTF8.GetString(e.Data);
        }

        private static void OnClientConnected(object? sender, ClientConnectedEventArgs e)
        {
            deviceIpPort = e.IpPort;
        }
    }
}
