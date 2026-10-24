package com.example.skylyrics.client;

import com.example.skylyrics.Config;
import com.example.skylyrics.Lyrics;
import com.example.skylyrics.NowPlaying;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * "Sky" lyrics: each new line is pinned to a fixed direction in the sky, above where you were looking
 * when it started. Turn your head and it slides across the screen like it is really up there.
 * Lines fade out smoothly when they get near the middle of the screen (if "Keep center clear" is on),
 * so they never cover the crosshair or an opponent.
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

    private static float smooth(double e0, double e1, double x) {
        double t = Math.max(0, Math.min(1, (x - e0) / (e1 - e0)));
        return (float) (t * t * (3 - 2 * t));
    }

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

        // Use the real camera (already smoothed between ticks) so text does not swim against the world.
        Camera cam = mc.gameRenderer.getCamera();
        float camYaw = cam.getYaw();
        float camPitch = cam.getPitch();
        double tanHalf = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);

        long now = System.currentTimeMillis();
        int idx = Lyrics.indexAt(ls, pos);

        if (idx != lastIdx) {
            lastIdx = idx;
            for (Bubble b : bubbles) if (b.end == 0) b.end = now;
            if (idx >= 0 && !ls.get(idx).text().isEmpty()) {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                Bubble b = new Bubble();
                b.text = ls.get(idx).text();
                b.start = now;
                // place the line well above where you are looking (about 68% of the way to the top of the screen)
                double offDeg = Math.toDegrees(Math.atan(0.68 * tanHalf));
                b.yaw = camYaw + (float) r.nextDouble(-14, 14);
                b.pitch = (float) Math.max(-85, Math.min(-15, camPitch - offDeg + r.nextDouble(-2, 2)));
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
        double aspect = (double) sw / sh;
        float scale = c.lyricsSize * 1.3f;

        for (Bubble b : bubbles) {
            float t = b.end == 0 ? 0f : (now - b.end) / (float) FADE_OUT_MS;
            float te = smooth(0, 1, t);
            double by = Math.toRadians(b.yaw), bp = Math.toRadians(b.pitch - 6f * te);
            double dx = -Math.sin(by) * Math.cos(bp), dy = -Math.sin(bp), dz = Math.cos(by) * Math.cos(bp);
            double depth = dx * fx + dy * fy + dz * fz;
            if (depth < 0.05) continue;
            double nx = (dx * rx + dz * rz) / depth / (tanHalf * aspect);
            double ny = (dx * ux + dy * uy + dz * uz) / depth / tanHalf;
            if (Math.abs(nx) > 1.4 || Math.abs(ny) > 1.3) continue;

            float fit = LyricsHud.fitScale(ctx, tr, b.text, scale);

            // keep the middle of the screen clear: fade out smoothly near the crosshair
            float vis = 1f;
            if (c.keepCenterClear) {
                double halfW = tr.getWidth(b.text) * fit / sw; // half text width in -1..1 units
                double ex = Math.max(0, Math.abs(nx) - halfW);
                double r = Math.hypot(ex / 0.20, Math.abs(ny) / 0.45);
                vis = smooth(0.9, 1.35, r);
            }

            float fadeIn = smooth(0, 1, (now - b.start) / 450f);
            float a = fadeIn * (1f - te) * vis * (b.end == 0 ? 1f : 0.8f);
            int alpha = (int) (255 * a);
            if (alpha < 8) continue;
            int color = (alpha << 24) | (b.end == 0 ? (c.accentColor & 0xFFFFFF) : 0xFFFFFF);

            float sx = (float) (sw / 2.0 * (1 + nx));
            float sy = (float) (sh / 2.0 * (1 - ny));
            LyricsHud.drawScaled(ctx, tr, b.text, sx, sy, scale, color);
        }
    }
}
