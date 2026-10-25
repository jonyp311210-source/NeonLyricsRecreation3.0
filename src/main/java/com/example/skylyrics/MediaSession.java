package com.example.skylyrics;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MediaSession {
    private static final ScheduledExecutorService SCH = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "skylyrics-media");
        t.setDaemon(true);
        return t;
    });

    public static void start() {
        SCH.scheduleAtFixedRate(MediaSession::poll, 0, 500, TimeUnit.MILLISECONDS);
    }

    private static void poll() {
        // Placeholder - extend this later with real media session reading
    }
}
