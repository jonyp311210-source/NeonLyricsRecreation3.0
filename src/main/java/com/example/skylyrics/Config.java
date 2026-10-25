package com.example.skylyrics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("skylyrics.json");
    public static Config I = new Config();

    public boolean showOverlay = true;
    public boolean showSpectrum = true;
    public int cardCorner = 0;
    public boolean showLyrics = true;
    public boolean showContext = true;
    public int lyricsMode = 2;              // 0=Follow, 1=Sky, 2=Screen (floating bar)
    public int followSmooth = 1;
    public float lyricsOpacity = 0.85f;
    public boolean keepCenterClear = true;
    public float lyricsSize = 1.4f;
    public int lyricsOffsetMs = 0;
    public int accentColor = 0xFF9B5DE5;

    public boolean splitLyrics = false;
    public int lyricsAlign = 0;             // 0=Center, 1=Left, 2=Right
    public float lyricsY = 0.78f;
    public boolean fadePrevious = true;
    public int wordRevealMode = 0;          // 0 = Progressive, 1 = Whole line
    public boolean showSongTitleIntro = true;
    public boolean glowEnabled = true;

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                I = GSON.fromJson(Files.readString(FILE), Config.class);
            }
        } catch (Exception e) {
            I = new Config();
        }
        if (I == null) I = new Config();
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(I));
        } catch (Exception ignored) {}
    }
}
