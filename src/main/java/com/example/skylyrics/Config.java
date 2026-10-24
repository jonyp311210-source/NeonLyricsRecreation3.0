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
    public boolean showSpectrum = true;
    /** 0 = top-left, 1 = top-right, 2 = bottom-left, 3 = bottom-right */
    public int cardCorner = 0;
    // Lyrics
    public boolean showLyrics = true;
    public boolean showContext = true;
    /** 0 = Sky (pinned in the world), 1 = Screen (fixed on the HUD) */
    public int lyricsStyle = 0;
    public float lyricsSize = 1.5f;
    public int lyricsOffsetMs = 0;
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
