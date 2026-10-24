package com.example.skylyrics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads "what is playing" from Windows' own media controls (the same thing that powers the
 * volume pop-up). Works with Spotify, browsers, etc. No login or API key needed.
 * It starts a small PowerShell script that prints one line of JSON every ~0.7 seconds.
 * Windows only.
 */
public class MediaSession {
    private static volatile Process proc;
    private static boolean started = false;

    private static final String SCRIPT = """
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
function Await($op, $type) {
  $task = $asTaskGeneric.MakeGenericMethod($type).Invoke($null, @($op))
  $task.Wait(-1) | Out-Null
  $task.Result
}
[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime] | Out-Null
$mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
while ($true) {
  try {
    $session = $null
    foreach ($s in $mgr.GetSessions()) { if ($s.SourceAppUserModelId -like '*Spotify*') { $session = $s; break } }
    if ($null -eq $session) { $session = $mgr.GetCurrentSession() }
    if ($null -eq $session) {
      $out = @{ ok = $false }
    } else {
      $props = Await ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
      $tl = $session.GetTimelineProperties()
      $pb = $session.GetPlaybackInfo()
      $playing = ([string]$pb.PlaybackStatus -eq 'Playing')
      $pos = $tl.Position.TotalMilliseconds
      $dur = ($tl.EndTime - $tl.StartTime).TotalMilliseconds
      if ($playing) { $pos += ([DateTimeOffset]::UtcNow - $tl.LastUpdatedTime).TotalMilliseconds }
      $out = @{ ok = $true; title = $props.Title; artist = $props.Artist; pos = [long]$pos; dur = [long]$dur; playing = $playing }
    }
  } catch {
    $out = @{ ok = $false; err = $_.Exception.Message }
  }
  [Console]::Out.WriteLine(($out | ConvertTo-Json -Compress))
  [Console]::Out.Flush()
  Start-Sleep -Milliseconds 700
}
""";

    public static synchronized void start() {
        if (started) return;
        started = true;
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            NowPlaying.clear("Song detection only works on Windows for now");
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Process p = proc;
            if (p != null) p.destroyForcibly();
        }));
        Thread t = new Thread(MediaSession::loop, "skylyrics-media");
        t.setDaemon(true);
        t.start();
    }

    private static void loop() {
        while (true) {
            try {
                runOnce();
                NowPlaying.clear("Media reader stopped, retrying...");
            } catch (Exception e) {
                NowPlaying.clear("Media reader failed: " + e.getClass().getSimpleName());
            }
            try { Thread.sleep(5000); } catch (InterruptedException ignored) { return; }
        }
    }

    private static void runOnce() throws Exception {
        Path script = Files.createTempFile("skylyrics-media", ".ps1");
        script.toFile().deleteOnExit();
        Files.writeString(script, SCRIPT, StandardCharsets.UTF_8);

        ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass", "-File", script.toString());
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process p = pb.start();
        proc = p;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) handle(line);
        } finally {
            p.destroyForcibly();
        }
    }

    private static void handle(String line) {
        try {
            if (!line.isEmpty() && line.charAt(0) == '\uFEFF') line = line.substring(1);
            JsonObject o = JsonParser.parseString(line).getAsJsonObject();
            if (!o.get("ok").getAsBoolean()) {
                NowPlaying.clear("Nothing playing. Start a song in Spotify.");
                return;
            }
            String title = str(o, "title");
            if (title.isEmpty()) { NowPlaying.clear("Nothing playing. Start a song in Spotify."); return; }
            NowPlaying.update(title, str(o, "artist"),
                    o.get("dur").getAsLong(), o.get("pos").getAsLong(), o.get("playing").getAsBoolean());
        } catch (Exception ignored) {
            // ignore malformed lines
        }
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return (e == null || e.isJsonNull()) ? "" : e.getAsString();
    }
}
