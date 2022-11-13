using ICSharpCode.SharpZipLib.Core;
using ICSharpCode.SharpZipLib.Zip;
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
    public partial class ADBCheck : Form
    {
        public ADBCheck()
        {
            InitializeComponent();
        }

        private void ADBCheck_Load(object sender, EventArgs e)
        {
            string folderPath = Path.Combine(Directory.GetCurrentDirectory(), "LemonADB");

            StaticStuff.ADBPath = Path.Combine(folderPath, "adb.exe");
            if (!Directory.Exists(folderPath) || !File.Exists(StaticStuff.ADBPath))
            {
                using MemoryStream stream = new MemoryStream(Resources.platform_tools, false);
                UnzipFromStream(stream, folderPath);
            }

            new MainForm().ShowDialog();
            Close();
        }

        private void UnzipFromStream(Stream zipStream, string outFolder)
        {
            using (var zipInputStream = new ZipInputStream(zipStream))
            {
                while (zipInputStream.GetNextEntry() is ZipEntry zipEntry)
                {
                    var entryFileName = zipEntry.Name;
                    var buffer = new byte[4096];
                    var fullZipToPath = Path.Combine(outFolder, entryFileName);
                    var directoryName = Path.GetDirectoryName(fullZipToPath);
                    if (directoryName.Length > 0)
                        Directory.CreateDirectory(directoryName);
                    if (Path.GetFileName(fullZipToPath).Length == 0)
                        continue;
                    using FileStream streamWriter = File.Create(fullZipToPath);
                    StreamUtils.Copy(zipInputStream, streamWriter, buffer);
                }
            }
        }
    }
}
