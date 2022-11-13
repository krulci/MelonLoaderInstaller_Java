using SharpAdbClient;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace LemonADBBridge
{
    internal class DeviceComboBox
    {
        private ComboBox comboBox;
        private List<DeviceData> devices;

        public DeviceComboBox(ComboBox cox)
        {
            comboBox = cox;
            devices = new List<DeviceData>();
        }

        public void ClearAll()
        {
            devices.Clear();
            comboBox.Items.Clear();
        }

        public void AddItem(DeviceData data)
        {
            comboBox.Items.Add(data.Model + " : " + data.Serial);
            devices.Add(data);
        }

        public DeviceData GetSelectedData()
        {
            int index = comboBox.SelectedIndex;
            return devices[index];
        }
    }
}
