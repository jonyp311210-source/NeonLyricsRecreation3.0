package com.example.skylyrics.client;

import com.example.skylyrics.Config;
import com.example.skylyrics.Lyrics;
import com.example.skylyrics.NowPlaying;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.List;

public class LyricsHud {

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        Config c = Config.I;
        if (!c.showLyrics) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.currentScreen != null) return;
        if (NowPlaying.snap == null) return;

        TextRenderer tr = mc.textRenderer;
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        List<Lyrics.Line> ls = Lyrics.lines;
        if (ls.isEmpty()) {
            String st = Lyrics.status;
            if (!st.isEmpty() && !st.equals("Loading lyrics...")) {
                drawCentered(ctx, tr, st, sw / 2f, sh * 0.5f, 1.0f, 0xFFAAAAAA);
            }
            return;
        }

        long pos = NowPlaying.positionMs() + c.lyricsOffsetMs;

        // Sky / Follow modes
        if (c.lyricsMode != 2) {
            SkyLyricsHud.render(ctx, tr, ls, pos, c);
            return;
        }

        // ===== SCREEN MODE – Floating Karaoke Bar (the thing in the picture) =====
        renderFloatingBar(ctx, tr, ls, pos, c, sw, sh);
    }

    private static void renderFloatingBar(DrawContext ctx, TextRenderer tr,
                                          List<Lyrics.Line> ls, long pos, Config c,
                                          int sw, int sh) {
        int idx = Lyrics.indexAt(ls, pos);
        if (idx < 0) return;

        Lyrics.Line cur = ls.get(idx);
        if (cur.text().isEmpty()) return;

        float size = c.lyricsSize;
        float barY = sh * c.lyricsY;

        // True split mode: split the current lyric into two readable halves,
        // anchored on the left and right sides instead of merely moving one bar.
        if (c.splitLyrics) {
            renderSplitLyrics(ctx, tr, ls, idx, cur, c, sw, sh, size, barY);
            return;
        }

        // Build the display text with word highlighting
        int activeWord = Lyrics.wordIndexAt(cur, pos);
        List<Lyrics.Word> words = cur.words();

        // Calculate total width for the bar
        float scale = size * 0.95f;
        int totalWidth = 0;
        if (words != null && !words.isEmpty()) {
            for (Lyrics.Word w : words) {
                totalWidth += tr.getWidth(w.text() + " ");
            }
        } else {
            totalWidth = tr.getWidth(cur.text());
        }
        totalWidth = (int) (totalWidth * scale);

        int padX = 18;
        int padY = 10;
        int barW = totalWidth + padX * 2 + 40; // extra space for progress dots
        int barH = (int) (tr.fontHeight * scale) + padY * 2;

        // Horizontal position based on alignment.
        float barX = switch (c.lyricsAlign) {
            case 1 -> sw * 0.08f + barW / 2f;
            case 2 -> sw * 0.92f - barW / 2f;
            default -> sw / 2f;
        };

        int left = (int) (barX - barW / 2f);
        int top  = (int) (barY - barH / 2f);

        // Background capsule (dark rounded bar like the screenshot)
        int bgColor = 0xE01A1A1E;
        drawRoundedRect(ctx, left, top, left + barW, top + barH, 14, bgColor);

        // Subtle border / glow
        if (c.glowEnabled) {
            int glow = (c.accentColor & 0x00FFFFFF) | 0x22000000;
            drawRoundedRect(ctx, left - 1, top - 1, left + barW + 1, top + barH + 1, 15, glow);
        }

        // Draw words with highlighting
        float textX = left + padX;
        float textY = top + padY;

        if (words != null && !words.isEmpty() && c.wordRevealMode == 0) {
            // Progressive: highlight current word, fade future words a bit
            for (int i = 0; i < words.size(); i++) {
                Lyrics.Word w = words.get(i);
                String part = w.text() + (i < words.size() - 1 ? " " : "");
                int color;
                if (i < activeWord) {
                    color = 0xFFCCCCCC; // past words
                } else if (i == activeWord) {
                    color = c.accentColor; // current word – purple
                } else {
                    color = 0x66FFFFFF; // future words dimmer
                }
                // Apply global opacity
                int a = (int) (((color >>> 24) & 0xFF) * c.lyricsOpacity);
                color = (a << 24) | (color & 0x00FFFFFF);

                ctx.getMatrices().push();
                ctx.getMatrices().translate(textX, textY, 0);
                ctx.getMatrices().scale(scale, scale, 1);
                ctx.drawText(tr, part, 0, 0, color, false);
                ctx.getMatrices().pop();
                textX += tr.getWidth(part) * scale;
            }
        } else {
            // Whole line mode (or no word data)
            int color = c.accentColor;
            int a = (int) (((color >>> 24) & 0xFF) * c.lyricsOpacity);
            color = (a << 24) | (color & 0x00FFFFFF);

            ctx.getMatrices().push();
            ctx.getMatrices().translate(textX, textY, 0);
            ctx.getMatrices().scale(scale, scale, 1);
            ctx.drawText(tr, cur.text(), 0, 0, color, false);
            ctx.getMatrices().pop();
        }

        // Progress dots on the right (like the screenshot)
        int dotsX = left + barW - 28;
        int dotsY = top + barH / 2;
        for (int i = 0; i < 3; i++) {
            int alpha = (i == 0) ? 0xFF : (i == 1 ? 0xAA : 0x55);
            int col = (alpha << 24) | (c.accentColor & 0x00FFFFFF);
            ctx.fill(dotsX + i * 6, dotsY - 1, dotsX + i * 6 + 3, dotsY + 2, col);
        }

        // Optional previous line above (faded)
        if (c.showContext && idx > 0) {
            Lyrics.Line prev = ls.get(idx - 1);
            if (!prev.text().isEmpty()) {
                float prevY = top - 22 * size;
                int prevColor = c.fadePrevious ? 0x55FFFFFF : 0xCCFFFFFF;
                drawCentered(ctx, tr, prev.text(), barX, prevY, size * 0.7f, prevColor);
            }
        }
    }

    private static void renderSplitLyrics(DrawContext ctx, TextRenderer tr,
                                          List<Lyrics.Line> ls, int idx, Lyrics.Line cur,
                                          Config c, int sw, int sh, float size, float barY) {
        String[] parts = cur.text().trim().split("\\s+");
        if (parts.length == 0) return;
        int midpoint = Math.max(1, (parts.length + 1) / 2);
        StringBuilder leftText = new StringBuilder();
        StringBuilder rightText = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            StringBuilder target = i < midpoint ? leftText : rightText;
            if (target.length() > 0) target.append(' ');
            target.append(parts[i]);
        }

        float halfWidth = sw * 0.43f;
        float scale = size * 0.88f;
        float leftScale = Math.min(scale, halfWidth / Math.max(1, tr.getWidth(leftText.toString())));
        float rightScale = rightText.length() == 0 ? scale
                : Math.min(scale, halfWidth / Math.max(1, tr.getWidth(rightText.toString())));
        int barH = Math.max(24, (int) (tr.fontHeight * scale) + 18);
        drawSplitPiece(ctx, tr, leftText.toString(), sw * 0.25f, barY, leftScale, barH, c);
        if (rightText.length() > 0) {
            drawSplitPiece(ctx, tr, rightText.toString(), sw * 0.75f, barY, rightScale, barH, c);
        }

        if (c.showContext && idx > 0) {
            String previous = ls.get(idx - 1).text();
            if (!previous.isEmpty()) {
                int color = c.fadePrevious ? 0x55FFFFFF : 0xCCFFFFFF;
                drawCentered(ctx, tr, previous, sw * 0.5f, barY - barH * 0.8f,
                        size * 0.58f, color);
            }
        }
    }

    private static void drawSplitPiece(DrawContext ctx, TextRenderer tr, String text,
                                       float centerX, float centerY, float scale,
                                       int barH, Config c) {
        int textW = (int) (tr.getWidth(text) * scale);
        int barW = Math.min(textW + 28, (int) (MinecraftClient.getInstance().getWindow().getScaledWidth() * 0.46));
        int left = (int) (centerX - barW / 2f);
        int top = (int) (centerY - barH / 2f);
        drawRoundedRect(ctx, left, top, left + barW, top + barH, 12, 0xE01A1A1E);
        if (c.glowEnabled) {
            int glow = (c.accentColor & 0x00FFFFFF) | 0x22000000;
            drawRoundedRect(ctx, left - 1, top - 1, left + barW + 1, top + barH + 1, 13, glow);
        }
        int color = (Math.max(0, Math.min(255, (int) (c.lyricsOpacity * 255))) << 24)
                | (c.accentColor & 0x00FFFFFF);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(centerX, top + (barH - tr.fontHeight * scale) / 2f, 0);
        ctx.getMatrices().scale(scale, scale, 1);
        ctx.getMatrices().translate(-tr.getWidth(text) / 2f, 0, 0);
        ctx.drawText(tr, text, 0, 0, color, false);
        ctx.getMatrices().pop();
    }

    private static void drawRoundedRect(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int color) {
        // Simple approximation of rounded rect using fills (good enough for HUD)
        ctx.fill(x1 + r, y1, x2 - r, y2, color);
        ctx.fill(x1, y1 + r, x2, y2 - r, color);
        // corners (square for simplicity – Minecraft HUD doesn't have easy rounded AA)
        ctx.fill(x1, y1, x1 + r, y1 + r, color);
        ctx.fill(x2 - r, y1, x2, y1 + r, color);
        ctx.fill(x1, y2 - r, x1 + r, y2, color);
        ctx.fill(x2 - r, y2 - r, x2, y2, color);
    }

    static void drawCentered(DrawContext ctx, TextRenderer tr, String text,
                             float cx, float y, float scale, int argb) {
        int a = (int) (((argb >>> 24) & 0xFF) * Config.I.lyricsOpacity);
        if (a < 8) return;
        int color = (a << 24) | (argb & 0x00FFFFFF);
        int w = tr.getWidth(text);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx, y, 0);
        ctx.getMatrices().scale(scale, scale, 1);
        ctx.getMatrices().translate(-w / 2f, 0, 0);
        ctx.drawText(tr, text, 0, 0, color, false);
        ctx.getMatrices().pop();
    }

    /** Used by SkyLyricsHud */
    static float fitScaleApprox(TextRenderer tr, String text, float scale, int sw) {
        int w = Math.max(1, tr.getWidth(text));
        float maxW = sw * 0.8f;
        return Math.min(scale, maxW / w);
    }
}

