package com.example.skylyrics.client;

import com.example.skylyrics.Config;
import com.example.skylyrics.Lyrics;
import com.example.skylyrics.NowPlaying;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * "Sky" lyrics: each new line is pinned to a fixed direction in the sky, where you were looking when
 * it started. Turn your head and it slides across the screen like it is really up there, then it
 * fades and drifts upward when the next line starts.
 * (This projects a direction onto the screen, so it is drawn over the world, not behind terrain.)
 */
public class SkyLyricsHud {
    private static class Bubble {
        String text;
        long start;
        long end = 0;
        float yaw, pitch;
    }

    private static final long FADE_OUT_MS = 2500;
    private static final List<Bubble> bubbles = new ArrayList<>();
    private static int lastIdx = -2;
    private static String lastKey = "";

    static void render(DrawContext ctx, TextRenderer tr, List<Lyrics.Line> ls, long pos, Config c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        NowPlaying.Snap snap = NowPlaying.snap;
        String key = snap == null ? "" : snap.title() + "|" + snap.artist();
        if (!key.equals(lastKey)) {
            bubbles.clear();
            lastKey = key;
            lastIdx = -2;
        }

        long now = System.currentTimeMillis();
        int idx = Lyrics.indexAt(ls, pos);
        float camYaw = mc.player.getYaw();
        float camPitch = mc.player.getPitch();

        if (idx != lastIdx) {
            lastIdx = idx;
            for (Bubble b : bubbles) if (b.end == 0) b.end = now;
            if (idx >= 0 && !ls.get(idx).text().isEmpty()) {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                Bubble b = new Bubble();
                b.text = ls.get(idx).text();
                b.start = now;
                b.yaw = camYaw + (float) r.nextDouble(-14, 14);
                b.pitch = Math.max(-55f, Math.min(-12f, camPitch)) + (float) r.nextDouble(-4, 4);
                bubbles.add(b);
            }
        }
        bubbles.removeIf(b -> b.end != 0 && now - b.end > FADE_OUT_MS);

        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        double yr = Math.toRadians(camYaw), pr = Math.toRadians(camPitch);
        // camera basis: forward, right, up
        double fx = -Math.sin(yr) * Math.cos(pr), fy = -Math.sin(pr), fz = Math.cos(yr) * Math.cos(pr);
        double rx = -Math.cos(yr), rz = -Math.sin(yr);
        double ux = -rz * fy, uy = rz * fx - rx * fz, uz = rx * fy;
        double tanHalf = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        double aspect = (double) sw / sh;

        for (Bubble b : bubbles) {
            float t = b.end == 0 ? 0f : (now - b.end) / (float) FADE_OUT_MS;
            double by = Math.toRadians(b.yaw), bp = Math.toRadians(b.pitch - 6f * t);
            double dx = -Math.sin(by) * Math.cos(bp), dy = -Math.sin(bp), dz = Math.cos(by) * Math.cos(bp);
            double depth = dx * fx + dy * fy + dz * fz;
            if (depth < 0.05) continue;
            double nx = (dx * rx + dz * rz) / depth / (tanHalf * aspect);
            double ny = (dx * ux + dy * uy + dz * uz) / depth / tanHalf;
            if (Math.abs(nx) > 1.3 || Math.abs(ny) > 1.3) continue;

            float fadeIn = Math.min(1f, (now - b.start) / 350f);
            float a = fadeIn * (1f - t) * (b.end == 0 ? 1f : 0.8f);
            int alpha = (int) (255 * a);
            if (alpha < 8) continue; // very low alpha would be drawn fully opaque
            int color = (alpha << 24) | (b.end == 0 ? (c.accentColor & 0xFFFFFF) : 0xFFFFFF);

            int sx = (int) (sw / 2.0 * (1 + nx));
            int sy = (int) (sh / 2.0 * (1 - ny));
            LyricsHud.drawScaled(ctx, tr, b.text, sx, sy, c.lyricsSize * 1.3f, color);
        }
    }
}
