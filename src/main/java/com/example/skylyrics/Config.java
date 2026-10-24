package com.example.skylyrics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Simple JSON config stored in .minecraft/config/skylyrics.json */
public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("skylyrics.json");
    public static Config I = new Config();

    // Display
    public boolean showOverlay = true;
    public boolean showClock = true;
    public boolean showSpectrum = true;
    // Lyrics
    public boolean showLyrics = true;
    public boolean lyricsPulse = true;
    public float lyricsSize = 1.0f;
    public int accentColor = 0xFF8AB4F8;

    public static void load() {
        try {
            if (Files.exists(FILE)) I = GSON.fromJson(Files.readString(FILE), Config.class);
        } catch (Exception e) { I = new Config(); }
        if (I == null) I = new Config();
    }

    public static void save() {
        try { Files.writeString(FILE, GSON.toJson(I)); } catch (Exception ignored) {}
    }
}
