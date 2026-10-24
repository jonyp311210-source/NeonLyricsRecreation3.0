package com.example.skylyrics.client;

import com.example.skylyrics.Config;
import com.example.skylyrics.NowPlaying;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** Draws the now-playing card in the top-left: art placeholder, title, artist, progress, spectrum. */
public class NowPlayingHud {
    public static void render(DrawContext ctx, RenderTickCounter tick) {
        Config c = Config.I;
        if (!c.showOverlay || NowPlaying.track == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.currentScreen != null) return;
        TextRenderer tr = mc.textRenderer;

        int x = 8, y = 8, size = 48;
        // card background + art placeholder
        ctx.fill(x - 4, y - 4, x + 190, y + size + 8, 0x90000000);
        ctx.fill(x, y, x + size, y + size, 0xFF2A2A2A);
        ctx.drawTextWithShadow(tr, "\u266A", x + size / 2 - 3, y + size / 2 - 4, c.accentColor);

        int tx = x + size + 8;
        ctx.drawTextWithShadow(tr, NowPlaying.track.title(), tx, y + 4, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, NowPlaying.track.artist(), tx, y + 16, 0xFFAAAAAA);

        // progress bar
        int bx = tx, by = y + 32, bw = 120;
        ctx.fill(bx, by, bx + bw, by + 2, 0xFF444444);
        float p = (float) NowPlaying.positionMs / NowPlaying.track.durationMs();
        ctx.fill(bx, by, bx + (int) (bw * p), by + 2, c.accentColor);

        // fake spectrum (replace with real audio analysis later)
        if (c.showSpectrum) {
            long t = System.currentTimeMillis();
            for (int i = 0; i < 24; i++) {
                double v = NowPlaying.playing ? (Math.sin(t / 140.0 + i * 0.7) + 1) / 2 : 0.1;
                int h = 2 + (int) (v * 10);
                ctx.fill(x + i * 4, y + size + 6 - h, x + i * 4 + 2, y + size + 6, c.accentColor);
            }
        }
    }
}
