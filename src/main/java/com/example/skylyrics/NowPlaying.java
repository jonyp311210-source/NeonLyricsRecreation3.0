package com.example.skylyrics;

/**
 * Holds the current track. Stage 1 uses a demo track so the UI works offline.
 * Stage 2 replaces tick() with a real provider (Spotify Web API, or the OS media session).
 */
public class NowPlaying {
    public record Track(String title, String artist, long durationMs) {}

    public static Track track = new Track("Demo Song", "Demo Artist", 231_000);
    public static boolean playing = true;
    public static long positionMs = 0;

    public static void tick() {
        if (!playing || track == null) return;
        positionMs += 50; // client tick = 50 ms
        if (positionMs >= track.durationMs()) positionMs = 0;
    }

    public static void togglePlay() { playing = !playing; }
}
