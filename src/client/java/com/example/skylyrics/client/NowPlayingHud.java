package com.example.skylyrics.client;

import com.example.skylyrics.Config;
import com.example.skylyrics.NowPlaying;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;

public class NowPlayingHud {
    static final String[] CORNERS = {"Top-left", "Top-right", "Bottom-left", "Bottom-right"};
    private static final int W = 196;
    private static final int H = 68;

    public static void render(GuiGraphics graphics, DeltaTracker tick) {
        Config c = Config.I;
        if (!c.showOverlay) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null) return;

        Font font = mc.font;
        int sw = graphics.guiWidth();
        int sh = graphics.guiHeight();
        int corner = Math.floorMod(c.cardCorner, 4);
        int ox = corner % 2 == 0 ? 8 : sw - W - 8;
        int oy = corner < 2 ? 40 : sh - H - 8;

        NowPlaying.Snap s = NowPlaying.snap;

        graphics.fill(ox, oy, ox + W, oy + H, 0xE01A1A1E);
        graphics.fill(ox + 4, oy + 4, ox + 52, oy + 52, 0xFF2A2A30);
        graphics.drawString(font, "♪", ox + 23, oy + 24, c.accentColor, false);

        int tx = ox + 60;
        if (s == null) {
            graphics.drawString(font, "Nothing playing", tx, oy + 8, 0xFFFFFF, false);
            String st = NowPlaying.status;
            if (!st.isEmpty()) {
                graphics.drawString(font, font.plainSubstrByWidth(st, 130), tx, oy + 22, 0xAAAAAA, false);
            }
            return;
        }

        graphics.drawString(font, font.plainSubstrByWidth(s.title(), 130), tx, oy + 8, 0xFFFFFF, false);
        graphics.drawString(font, font.plainSubstrByWidth(s.artist(), 130), tx, oy + 22, 0xAAAAAA, false);

        if (s.durationMs() > 0) {
            float p = Math.min(1f, (float) NowPlaying.positionMs() / s.durationMs());
            int barW = 120;
            graphics.fill(tx, oy + 48, tx + barW, oy + 52, 0xFF333333);
            graphics.fill(tx, oy + 48, tx + (int) (barW * p), oy + 52, c.accentColor);
        }
    }
}
