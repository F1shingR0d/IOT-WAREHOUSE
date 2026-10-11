// Encodes a JPEG frame sequence + a 16-bit PCM WAV into an H.264/AAC MP4 using the
// Windows Media Foundation encoder (Windows.Media.Transcoding + MediaStreamSource).
// Usage: ReelEncoder <framesDir> <fps> <frameCount> <wav> <out.mp4> <bitrate>
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Runtime.InteropServices;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Media.Core;
using Windows.Media.MediaProperties;
using Windows.Media.Transcoding;
using Windows.Storage;
using Windows.Storage.Streams;

public static class ReelEncoder
{
    static string framesDir;
    static int fps, frameCount, width, height;
    static byte[] pcm;           // interleaved stereo 16-bit
    static int pcmOffset;        // byte offset of the next audio chunk
    static int nextFrame;
    static long audioTicks;

    public static int Main(string[] args)
    {
        framesDir = args[0];
        fps = int.Parse(args[1]);
        frameCount = int.Parse(args[2]);
        string wavPath = args[3];
        string outPath = Path.GetFullPath(args[4]);
        uint bitrate = uint.Parse(args[5]);
        string flags = args.Length > 6 ? args[6] : "";

        using (var first = new Bitmap(FramePath(0))) { width = first.Width; height = first.Height; }
        pcm = ReadWavData(wavPath);

        var vprops = VideoEncodingProperties.CreateUncompressed(MediaEncodingSubtypes.Bgra8, (uint)width, (uint)height);
        vprops.FrameRate.Numerator = (uint)fps;
        vprops.FrameRate.Denominator = 1;
        var vdesc = new VideoStreamDescriptor(vprops);
        var adesc = new AudioStreamDescriptor(AudioEncodingProperties.CreatePcm(48000, 2, 16));
        var mss = flags.Contains("noaudio") ? new MediaStreamSource(vdesc) : new MediaStreamSource(vdesc, adesc);
        mss.BufferTime = TimeSpan.Zero;
        mss.Duration = TimeSpan.FromTicks(10000000L * frameCount / fps);
        mss.Starting += (s, e) => e.Request.SetActualStartPosition(TimeSpan.Zero);
        mss.SampleRequested += OnSampleRequested;

        var profile = MediaEncodingProfile.CreateMp4(flags.Contains("hd720") ? VideoEncodingQuality.HD720p : VideoEncodingQuality.HD1080p);
        if (flags.Contains("noaudio")) profile.Audio = null;
        if (!flags.Contains("defaultprofile")) {
        profile.Video.Width = (uint)width;
        profile.Video.Height = (uint)height;
        profile.Video.FrameRate.Numerator = (uint)fps;
        profile.Video.FrameRate.Denominator = 1;
        profile.Video.Bitrate = bitrate;
        if (!flags.Contains("noaudio")) profile.Audio = AudioEncodingProperties.CreateAac(48000, 2, 192000);
        }
        Console.WriteLine("profile video " + profile.Video.Subtype + " " + profile.Video.Width + "x" + profile.Video.Height + " fps " + profile.Video.FrameRate.Numerator + " br " + profile.Video.Bitrate);

        string dir = Path.GetDirectoryName(outPath);
        StorageFolder folder = StorageFolder.GetFolderFromPathAsync(dir).AsTask().Result;
        StorageFile file = folder.CreateFileAsync(Path.GetFileName(outPath), CreationCollisionOption.ReplaceExisting).AsTask().Result;
        using (IRandomAccessStream stream = file.OpenAsync(FileAccessMode.ReadWrite).AsTask().Result)
        {
            var tc = new MediaTranscoder();
            tc.HardwareAccelerationEnabled = !flags.Contains("nohw");
            PrepareTranscodeResult prep = tc.PrepareMediaStreamSourceTranscodeAsync(mss, stream, profile).AsTask().Result;
            if (!prep.CanTranscode)
            {
                Console.WriteLine("CANNOT TRANSCODE: " + prep.FailureReason);
                return 2;
            }
            var op = prep.TranscodeAsync();
            op.Progress = (info, p) => { };
            op.AsTask().Wait();
        }
        Console.WriteLine("DONE frames=" + nextFrame + " size=" + new FileInfo(outPath).Length);
        return 0;
    }

    static string FramePath(int i) { return Path.Combine(framesDir, string.Format("f{0:D5}.jpg", i)); }

    static void OnSampleRequested(MediaStreamSource sender, MediaStreamSourceSampleRequestedEventArgs e)
    {
        try
        {
            if (e.Request.StreamDescriptor is VideoStreamDescriptor)
            {
                if (nextFrame >= frameCount) return;   // end of stream
                byte[] bgra = LoadFrame(FramePath(nextFrame));
                var ts = TimeSpan.FromTicks(10000000L * nextFrame / fps);
                var sample = MediaStreamSample.CreateFromBuffer(bgra.AsBuffer(), ts);
                sample.Duration = TimeSpan.FromTicks(10000000L / fps);
                sample.KeyFrame = true;
                e.Request.Sample = sample;
                nextFrame++;
                if (nextFrame % 120 == 0) Console.WriteLine("frame " + nextFrame);
            }
            else
            {
                if (pcmOffset >= pcm.Length) return;   // end of stream
                int chunkFrames = 4800;                 // 100 ms
                int bytes = Math.Min(chunkFrames * 4, pcm.Length - pcmOffset);
                byte[] chunk = new byte[bytes];
                System.Buffer.BlockCopy(pcm, pcmOffset, chunk, 0, bytes);
                var sample = MediaStreamSample.CreateFromBuffer(chunk.AsBuffer(), TimeSpan.FromTicks(audioTicks));
                long dur = 10000000L * (bytes / 4) / 48000;
                sample.Duration = TimeSpan.FromTicks(dur);
                e.Request.Sample = sample;
                audioTicks += dur;
                pcmOffset += bytes;
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine("ERROR " + ex);
        }
    }

    static byte[] LoadFrame(string path)
    {
        using (var bmp = new Bitmap(path))
        {
            var rect = new Rectangle(0, 0, bmp.Width, bmp.Height);
            BitmapData data = bmp.LockBits(rect, ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
            try
            {
                int rowBytes = bmp.Width * 4;
                byte[] result = new byte[rowBytes * bmp.Height];
                for (int y = 0; y < bmp.Height; y++)
                {
                    // Media Foundation reads uncompressed RGB bottom-up, so the rows are stored flipped
                    Marshal.Copy(IntPtr.Add(data.Scan0, y * data.Stride), result, (bmp.Height - 1 - y) * rowBytes, rowBytes);
                }
                return result;
            }
            finally { bmp.UnlockBits(data); }
        }
    }

    static byte[] ReadWavData(string path)
    {
        byte[] all = File.ReadAllBytes(path);
        int pos = 12;
        while (pos + 8 <= all.Length)
        {
            string id = System.Text.Encoding.ASCII.GetString(all, pos, 4);
            int size = BitConverter.ToInt32(all, pos + 4);
            if (id == "data")
            {
                byte[] d = new byte[size];
                System.Buffer.BlockCopy(all, pos + 8, d, 0, size);
                return d;
            }
            pos += 8 + size + (size & 1);
        }
        throw new Exception("no data chunk");
    }
}
