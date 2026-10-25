package com.example.skylyrics.client;

import com.example.skylyrics.Config;
import com.example.skylyrics.Lyrics;
import com.example.skylyrics.NowPlaying;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class SettingsScreen extends Screen {

    private static Tab current = Tab.LYRICS;

    private static final String[] MODES      = {"Follow", "Sky", "Screen (Bar)"};
    private static final String[] SMOOTH     = {"Snappy", "Smooth", "Floaty"};
    private static final String[] ALIGN      = {"Center", "Left", "Right"};
    private static final String[] WORD_MODE  = {"Progressive (1 by 1)", "Whole line (wait)"};

    private static final int PANEL_W = 360;
    private static final int PANEL_H = 280;

    public SettingsScreen() {
        super(Component.literal("SkyLyrics"));
    }

    @Override
    protected void init() {
        int left = (width - PANEL_W) / 2;
        int top  = (height - PANEL_H) / 2;

        int ty = top + 40;
        for (Tab t : Tab.values()) {
            addRenderableWidget(Button.builder(Component.literal(t.label), b -> {
                current = t;
                rebuildWidgets();
            }).bounds(left + 10, ty, 82, 22).build());
            ty += 26;
        }

        int cx = left + 105;
        int cy = top + 40;
        int cw = 240;
        Config c = Config.I;

        switch (current) {
            case LYRICS -> buildLyricsTab(cx, cy, cw, c);
            case DISPLAY -> buildDisplayTab(cx, cy, cw, c);
            case PLAYER -> {
                addRenderableWidget(Button.builder(Component.literal("Reload lyrics"), b -> Lyrics.reload())
                        .bounds(cx, cy + 80, cw, 20).build());
            }
        }
    }

    private void buildLyricsTab(int cx, int cy, int cw, Config c) {
        addToggle(cx, cy, cw, "Show lyrics", () -> c.showLyrics, v -> c.showLyrics = v);
        cy += 24;

        addButton(cx, cy, cw, "Mode: " + MODES[Math.floorMod(c.lyricsMode, 3)], b -> {
            c.lyricsMode = (c.lyricsMode + 1) % 3;
            rebuildWidgets();
        });
        cy += 24;

        if (c.lyricsMode == 0) {
            addButton(cx, cy, cw, "Follow smoothness: " + SMOOTH[Math.floorMod(c.followSmooth, 3)], b -> {
                c.followSmooth = (c.followSmooth + 1) % 3;
                rebuildWidgets();
            });
        } else if (c.lyricsMode == 1) {
            addToggle(cx, cy, cw, "Keep center clear", () -> c.keepCenterClear, v -> c.keepCenterClear = v);
        } else {
            addToggle(cx, cy, cw, "Show previous / next", () -> c.showContext, v -> c.showContext = v);
        }
        cy += 26;

        addButton(cx, cy, cw, "Word style: " + WORD_MODE[Math.floorMod(c.wordRevealMode, 2)], b -> {
            c.wordRevealMode = (c.wordRevealMode + 1) % 2;
            rebuildWidgets();
        });
        cy += 24;

        addToggle(cx, cy, cw, "Split Left / Right", () -> c.splitLyrics, v -> c.splitLyrics = v);
        cy += 24;

        addButton(cx, cy, cw, "Line position: " + ALIGN[Math.floorMod(c.lyricsAlign, 3)], b -> {
            c.lyricsAlign = (c.lyricsAlign + 1) % 3;
            rebuildWidgets();
        });
        cy += 24;

        addButton(cx, cy, cw, String.format("Vertical height: %.0f%%", c.lyricsY * 100), b -> {
            c.lyricsY += 0.04f;
            if (c.lyricsY > 0.88f) c.lyricsY = 0.15f;
            rebuildWidgets();
        });
        cy += 24;

        addToggle(cx, cy, cw, "Previous lines fade out", () -> c.fadePrevious, v -> c.fadePrevious = v);
        cy += 26;

        addButton(cx, cy, cw, String.format("Size: %.2fx", c.lyricsSize), b -> {
            c.lyricsSize = c.lyricsSize >= 2.8f ? 0.8f : c.lyricsSize + 0.2f;
            rebuildWidgets();
        });
        cy += 24;

        addButton(cx, cy, cw, "Opacity: " + Math.round(c.lyricsOpacity * 100) + "%", b -> {
            c.lyricsOpacity = c.lyricsOpacity >= 0.99f ? 0.25f : Math.min(1f, c.lyricsOpacity + 0.1f);
            rebuildWidgets();
        });
        cy += 26;

        int half = cw / 2 - 3;
        addButton(cx, cy, half, "Earlier -0.25s", b -> {
            c.lyricsOffsetMs -= 250;
            rebuildWidgets();
        });
        addButton(cx + half + 6, cy, half, "Later +0.25s", b -> {
            c.lyricsOffsetMs += 250;
            rebuildWidgets();
        });
        cy += 24;
        addButton(cx, cy, cw,
                String.format("Offset: %.2fs (reset)", c.lyricsOffsetMs / 1000.0),
                b -> { c.lyricsOffsetMs = 0; rebuildWidgets(); });
    }

    private void buildDisplayTab(int cx, int cy, int cw, Config c) {
        addToggle(cx, cy, cw, "Now-playing card", () -> c.showOverlay, v -> c.showOverlay = v);
        cy += 24;
        addToggle(cx, cy, cw, "Decorative bars", () -> c.showSpectrum, v -> c.showSpectrum = v);
        cy += 24;
        addButton(cx, cy, cw, "Card position: " + NowPlayingHud.CORNERS[Math.floorMod(c.cardCorner, 4)], b -> {
            c.cardCorner = (c.cardCorner + 1) % 4;
            rebuildWidgets();
        });
        cy += 26;
        addToggle(cx, cy, cw, "Song title intro", () -> c.showSongTitleIntro, v -> c.showSongTitleIntro = v);
        cy += 24;
        addToggle(cx, cy, cw, "Glow on lyric bar", () -> c.glowEnabled, v -> c.glowEnabled = v);
    }

    private void addToggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        addRenderableWidget(Button.builder(
                Component.literal(label + ": " + (get.getAsBoolean() ? "§aON" : "§cOFF")),
                b -> {
                    set.accept(!get.getAsBoolean());
                    rebuildWidgets();
                }).bounds(x, y, w, 20).build());
    }

    private void addButton(int x, int y, int w, String label, Button.OnPress action) {
        addRenderableWidget(Button.builder(Component.literal(label), action)
                .bounds(x, y, w, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        int left = (width - PANEL_W) / 2;
        int top  = (height - PANEL_H) / 2;

        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xF0121216);
        graphics.fill(left, top, left + 95, top + PANEL_H, 0xFF0A0A0C);

        graphics.drawCenteredString(font, title, width / 2, top + 12, 0xFFFFFF);

        String section = switch (current) {
            case LYRICS -> "LYRICS & KARAOKE BAR";
            case DISPLAY -> "CARD & STYLE";
            case PLAYER -> "NOW PLAYING";
        };
        graphics.drawString(font, "§7" + section, left + 110, top + 28, 0xAAAAAA, false);

        if (current == Tab.PLAYER) {
            int x = left + 110;
            int y = top + 50;
            NowPlaying.Snap s = NowPlaying.snap;
            if (s == null) {
                graphics.drawString(font, "Nothing playing", x, y, 0xFFFFFF, false);
                graphics.drawString(font, font.plainSubstrByWidth(NowPlaying.status, 220), x, y + 14, 0xAAAAAA, false);
            } else {
                graphics.drawString(font, font.plainSubstrByWidth(s.title(), 220), x, y, 0xFFFFFF, false);
                graphics.drawString(font, font.plainSubstrByWidth(s.artist(), 220), x, y + 14, 0xAAAAAA, false);
                String ls = Lyrics.lines.isEmpty()
                        ? Lyrics.status
                        : Lyrics.lines.size() + " lyric lines loaded";
                graphics.drawString(font, font.plainSubstrByWidth(ls, 220), x, y + 34, Config.I.accentColor, false);
            }
        }
    }

    @Override
    public void onClose() {
        Config.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Tab {
        LYRICS("Lyrics"),
        DISPLAY("Display"),
        PLAYER("Player");

        final String label;
        Tab(String l) { this.label = l; }
    }
}
