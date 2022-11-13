using SharpAdbClient;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace LemonADBBridge
{
    public partial class MainForm : Form
    {
        private AdbServer adbServer;
        private AdbClient adbClient;
        private DeviceComboBox deviceComboBox;

        public MainForm()
        {
            InitializeComponent();
        }

        private void MainForm_Load(object sender, EventArgs e)
        {
            adbServer = new AdbServer();
            adbServer.StartServer(StaticStuff.ADBPath, restartServerIfNewer: true);
            adbClient = new AdbClient();

            deviceComboBox = new DeviceComboBox(devicesComboBox);

            foreach (var device in adbClient.GetDevices())
            {
                deviceComboBox.AddItem(device);
            }
        }

        private void RefreshDevices(object sender, EventArgs e)
        {
            button2.Enabled = false;
            deviceComboBox.ClearAll();
            foreach (var device in adbClient.GetDevices())
            {
                deviceComboBox.AddItem(device);
            }
        }

        private async void ConfirmDevice(object sender, EventArgs e)
        {
            devicesComboBox.Enabled = false;
            button1.Enabled = false;
            button2.Enabled = false;

            DeviceData confirmedData = deviceComboBox.GetSelectedData();
            await UninstallationHandler.Run(adbClient, confirmedData, this);
        }

        private void devicesComboBox_SelectedIndexChanged(object sender, EventArgs e)
        {
            button2.Enabled = devicesComboBox.SelectedIndex > -1;
        }
    }
}
