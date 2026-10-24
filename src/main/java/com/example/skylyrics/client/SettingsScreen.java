package com.example.skylyrics.client;

import com.example.skylyrics.Config;
import com.example.skylyrics.Lyrics;
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
    private static final String[] MODES = {"Follow", "Sky", "Screen"};
    private static final String[] SMOOTH = {"Snappy", "Smooth", "Floaty"};
    private static final int PW = 300, PH = 215;

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
                addDrawableChild(ButtonWidget.builder(
                        Text.literal("Lyrics mode: " + MODES[Math.floorMod(c.lyricsMode, 3)]), b -> {
                            c.lyricsMode = (c.lyricsMode + 1) % 3; clearAndInit();
                        }).dimensions(cx, cy + 24, cw, 20).build());
                if (c.lyricsMode == 0)
                    addDrawableChild(ButtonWidget.builder(
                            Text.literal("Follow smoothness: " + SMOOTH[Math.floorMod(c.followSmooth, 3)]), b -> {
                                c.followSmooth = (c.followSmooth + 1) % 3; clearAndInit();
                            }).dimensions(cx, cy + 48, cw, 20).build());
                else if (c.lyricsMode == 1)
                    toggle(cx, cy + 48, cw, "Keep center clear", () -> c.keepCenterClear, v -> c.keepCenterClear = v);
                else
                    toggle(cx, cy + 48, cw, "Previous/next lines", () -> c.showContext, v -> c.showContext = v);
                addDrawableChild(ButtonWidget.builder(Text.literal("Lyrics size: " + String.format("%.2f", c.lyricsSize) + "x"), b -> {
                    c.lyricsSize = c.lyricsSize >= 3.0f ? 0.75f : c.lyricsSize + 0.25f;
                    clearAndInit();
                }).dimensions(cx, cy + 72, cw, 20).build());
                addDrawableChild(ButtonWidget.builder(Text.literal("Opacity: " + Math.round(c.lyricsOpacity * 100) + "%"), b -> {
                    c.lyricsOpacity = c.lyricsOpacity >= 0.99f ? 0.2f : Math.min(1.0f, c.lyricsOpacity + 0.1f);
                    clearAndInit();
                }).dimensions(cx, cy + 96, cw, 20).build());
                int half = cw / 2 - 2;
                addDrawableChild(ButtonWidget.builder(Text.literal("Earlier -0.25s"), b -> {
                    c.lyricsOffsetMs -= 250; clearAndInit();
                }).dimensions(cx, cy + 120, half, 20).build());
                addDrawableChild(ButtonWidget.builder(Text.literal("Later +0.25s"), b -> {
                    c.lyricsOffsetMs += 250; clearAndInit();
                }).dimensions(cx + half + 4, cy + 120, half, 20).build());
                addDrawableChild(ButtonWidget.builder(
                        Text.literal(String.format("Timing offset: %.2fs (click to reset)", c.lyricsOffsetMs / 1000.0)), b -> {
                            c.lyricsOffsetMs = 0; clearAndInit();
                        }).dimensions(cx, cy + 144, cw, 20).build());
            }
            case DISPLAY -> {
                toggle(cx, cy, cw, "Now-playing card", () -> c.showOverlay, v -> c.showOverlay = v);
                toggle(cx, cy + 24, cw, "Decorative bars", () -> c.showSpectrum, v -> c.showSpectrum = v);
                addDrawableChild(ButtonWidget.builder(
                        Text.literal("Card position: " + NowPlayingHud.CORNERS[Math.floorMod(c.cardCorner, 4)]), b -> {
                            c.cardCorner = (c.cardCorner + 1) % 4; clearAndInit();
                        }).dimensions(cx, cy + 48, cw, 20).build());
            }
            case PLAYER -> addDrawableChild(ButtonWidget.builder(Text.literal("Reload lyrics"), b -> Lyrics.reload())
                    .dimensions(cx, cy + 60, cw, 20).build());
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
        int top = (height - PH) / 2, left = (width - PW) / 2;
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, top + 10, 0xFFFFFFFF);
        if (current == Tab.PLAYER) {
            int x = left + 90, y = top + 30;
            NowPlaying.Snap s = NowPlaying.snap;
            if (s == null) {
                ctx.drawTextWithShadow(textRenderer, "Nothing playing", x, y, 0xFFFFFFFF);
                ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(NowPlaying.status, PW - 100), x, y + 12, 0xFFAAAAAA);
            } else {
                ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(s.title(), PW - 100), x, y, 0xFFFFFFFF);
                ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(s.artist(), PW - 100), x, y + 12, 0xFFAAAAAA);
                String ls = Lyrics.lines.isEmpty() ? Lyrics.status : Lyrics.lines.size() + " lyric lines loaded";
                ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(ls, PW - 100), x, y + 30, 0xFF8AB4F8);
            }
        }
    }

    @Override
    public void removed() { Config.save(); }

    @Override
    public boolean shouldPause() { return false; }
}
