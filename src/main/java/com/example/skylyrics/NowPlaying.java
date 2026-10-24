package com.example.skylyrics;

/** The song that is playing right now, as reported by the media reader. */
public class NowPlaying {
    public record Snap(String title, String artist, long durationMs, long basePosMs, long baseNanos, boolean playing) {}

    public static volatile Snap snap = null;
    public static volatile String status = "Looking for a media player...";

    public static void update(String title, String artist, long durMs, long posMs, boolean playing) {
        snap = new Snap(title, artist, durMs, Math.max(0, posMs), System.nanoTime(), playing);
        status = "";
    }

    public static void clear(String why) {
        snap = null;
        status = why;
    }

    /** Current position, smoothly extrapolated between updates. */
    public static long positionMs() {
        Snap s = snap;
        if (s == null) return 0;
        long p = s.basePosMs();
        if (s.playing()) p += (System.nanoTime() - s.baseNanos()) / 1_000_000L;
        if (s.durationMs() > 0) p = Math.min(p, s.durationMs());
        return Math.max(0, p);
    }
}
