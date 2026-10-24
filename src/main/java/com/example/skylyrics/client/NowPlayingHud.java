package com.example.skylyrics.client;

import com.example.skylyrics.Config;
import com.example.skylyrics.NowPlaying;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** Draws the now-playing card: art placeholder, title, artist, progress bar, spectrum. */
public class NowPlayingHud {
    static final String[] CORNERS = {"Top-left", "Top-right", "Bottom-left", "Bottom-right"};
    private static final int W = 196, H = 68;

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        Config c = Config.I;
        if (!c.showOverlay) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.currentScreen != null) return;
        TextRenderer tr = mc.textRenderer;

        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int corner = Math.floorMod(c.cardCorner, 4);
        int ox = corner % 2 == 0 ? 8 : sw - W - 8;
        int oy = corner < 2 ? 40 : sh - H - 8;

        NowPlaying.Snap s = NowPlaying.snap;

        ctx.fill(ox, oy, ox + W, oy + H, 0x90000000);
        ctx.fill(ox + 4, oy + 4, ox + 52, oy + 52, 0xFF2A2A2A);
        ctx.drawTextWithShadow(tr, "\u266A", ox + 26 - 3, oy + 28 - 4, c.accentColor);

        int tx = ox + 60;
        if (s == null) {
            ctx.drawTextWithShadow(tr, "Nothing playing", tx, oy + 8, 0xFFFFFFFF);
            String st = NowPlaying.status;
            if (!st.isEmpty()) ctx.drawTextWithShadow(tr, tr.trimToWidth(st, W - 66), tx, oy + 22, 0xFFAAAAAA);
            return;
        }

        ctx.drawTextWithShadow(tr, tr.trimToWidth(s.title(), W - 66), tx, oy + 8, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, tr.trimToWidth(s.artist(), W - 66), tx, oy + 20, 0xFFAAAAAA);

        int bx = tx, by = oy + 36, bw = W - 68;
        ctx.fill(bx, by, bx + bw, by + 2, 0xFF444444);
        float p = s.durationMs() > 0 ? (float) NowPlaying.positionMs() / s.durationMs() : 0f;
        ctx.fill(bx, by, bx + (int) (bw * Math.min(1f, p)), by + 2, c.accentColor);

        // decorative bars (not real audio analysis yet)
        if (c.showSpectrum) {
            long t = System.currentTimeMillis();
            for (int i = 0; i < 24; i++) {
                double v = s.playing() ? (Math.sin(t / 140.0 + i * 0.7) + 1) / 2 : 0.1;
                int h = 2 + (int) (v * 10);
                ctx.fill(ox + 4 + i * 4, oy + H - 4 - h, ox + 4 + i * 4 + 2, oy + H - 4, c.accentColor);
            }
        }
    }
}
