package com.example.skylyrics.client;

import com.example.skylyrics.Config;
import com.example.skylyrics.Lyrics;
import com.example.skylyrics.NowPlaying;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.List;

/** Draws the synced lyrics on screen (previous line, current line, next line). */
public class LyricsHud {
    public static void render(DrawContext ctx, RenderTickCounter tick) {
        Config c = Config.I;
        if (!c.showLyrics) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.currentScreen != null) return;
        if (NowPlaying.snap == null) return;
        TextRenderer tr = mc.textRenderer;

        int cx = ctx.getScaledWindowWidth() / 2;
        int baseY = (int) (ctx.getScaledWindowHeight() * 0.22f);

        List<Lyrics.Line> ls = Lyrics.lines;
        if (ls.isEmpty()) {
            String st = Lyrics.status;
            if (!st.isEmpty()) drawScaled(ctx, tr, st, cx, baseY, 1.0f, 0xFFAAAAAA);
            return;
        }

        long pos = NowPlaying.positionMs() + c.lyricsOffsetMs;
        int idx = Lyrics.indexAt(ls, pos);
        float size = c.lyricsSize;

        if (idx >= 0) {
            Lyrics.Line cur = ls.get(idx);
            float fade = Math.min(1f, (pos - cur.timeMs()) / 300f);
            int alpha = 90 + (int) (165 * fade);
            int color = (alpha << 24) | (c.accentColor & 0xFFFFFF);
            String text = cur.text().isEmpty() ? "\u266A" : cur.text();
            drawScaled(ctx, tr, text, cx, baseY, size, color);
        }
        if (c.showContext) {
            if (idx - 1 >= 0 && !ls.get(idx - 1).text().isEmpty())
                drawScaled(ctx, tr, ls.get(idx - 1).text(), cx, baseY - (int) (14 * size), size * 0.6f, 0x99FFFFFF);
            if (idx + 1 < ls.size() && !ls.get(idx + 1).text().isEmpty())
                drawScaled(ctx, tr, ls.get(idx + 1).text(), cx, baseY + (int) (14 * size), size * 0.6f, 0x99FFFFFF);
        }
    }

    private static void drawScaled(DrawContext ctx, TextRenderer tr, String text, int cx, int y, float scale, int argb) {
        int w = Math.max(1, tr.getWidth(text));
        float maxW = ctx.getScaledWindowWidth() * 0.8f;
        float s = Math.min(scale, maxW / w);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) cx, (float) y);
        ctx.getMatrices().scale(s, s);
        ctx.drawTextWithShadow(tr, text, -w / 2, 0, argb);
        ctx.getMatrices().popMatrix();
    }
}
