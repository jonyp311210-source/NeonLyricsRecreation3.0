package com.example.skylyrics.client;

import com.example.skylyrics.Config;
import com.example.skylyrics.NowPlaying;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Tabbed settings menu (Lyrics / Display / Player). Opens with the J key. */
public class SettingsScreen extends Screen {
    private enum Tab { LYRICS("Lyrics"), DISPLAY("Display"), PLAYER("Player");
        final String label; Tab(String l) { label = l; } }

    private static Tab current = Tab.LYRICS;
    private static final int PW = 300, PH = 190;

    public SettingsScreen() { super(Text.literal("SkyLyrics")); }

    @Override
    protected void init() {
        int left = (width - PW) / 2, top = (height - PH) / 2;
        int ty = top + 30;
        for (Tab t : Tab.values()) {
            addDrawableChild(ButtonWidget.builder(Text.literal(t.label), b -> { current = t; clearAndInit(); })
                    .dimensions(left + 8, ty, 70, 20).build());
            ty += 24;
        }
        int cx = left + 90, cy = top + 30, cw = PW - 100;
        Config c = Config.I;
        switch (current) {
            case LYRICS -> {
                toggle(cx, cy, cw, "Show lyrics", () -> c.showLyrics, v -> c.showLyrics = v);
                toggle(cx, cy + 24, cw, "Pulse on the beat", () -> c.lyricsPulse, v -> c.lyricsPulse = v);
                addDrawableChild(ButtonWidget.builder(Text.literal("Lyrics size: " + c.lyricsSize + "x"), b -> {
                    c.lyricsSize = c.lyricsSize >= 2.0f ? 0.5f : c.lyricsSize + 0.25f;
                    clearAndInit();
                }).dimensions(cx, cy + 48, cw, 20).build());
            }
            case DISPLAY -> {
                toggle(cx, cy, cw, "Now-playing card", () -> c.showOverlay, v -> c.showOverlay = v);
                toggle(cx, cy + 24, cw, "Spectrum bars", () -> c.showSpectrum, v -> c.showSpectrum = v);
                toggle(cx, cy + 48, cw, "Clock", () -> c.showClock, v -> c.showClock = v);
            }
            case PLAYER -> addDrawableChild(ButtonWidget.builder(
                    Text.literal(NowPlaying.playing ? "Pause" : "Play"), b -> {
                        NowPlaying.togglePlay(); clearAndInit();
                    }).dimensions(cx, cy, cw, 20).build());
        }
    }

    private void toggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label + ": " + (get.getAsBoolean() ? "ON" : "OFF")), b -> {
            set.accept(!get.getAsBoolean());
            clearAndInit();
        }).dimensions(x, y, w, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        // TODO stage 2: draw a styled dark panel behind the widgets (needs renderBackground override).
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, (height - PH) / 2 + 10, 0xFFFFFFFF);
    }

    @Override
    public void removed() { Config.save(); }

    @Override
    public boolean shouldPause() { return false; }
}
