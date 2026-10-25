package com.example.skylyrics.client;

import com.example.skylyrics.Config;
import com.example.skylyrics.Lyrics;
import com.example.skylyrics.NowPlaying;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.List;

public class LyricsHud {

    public static void render(GuiGraphics graphics, DeltaTracker tick) {
        Config c = Config.I;
        if (!c.showLyrics) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null) return;
        if (NowPlaying.snap == null) return;

        Font font = mc.font;
        int sw = graphics.guiWidth();
        int sh = graphics.guiHeight();

        List<Lyrics.Line> ls = Lyrics.lines;
        if (ls.isEmpty()) {
            String st = Lyrics.status;
            if (!st.isEmpty() && !st.equals("Loading lyrics...")) {
                drawCentered(graphics, font, st, sw / 2f, sh * 0.5f, 1.0f, 0xFFAAAAAA);
            }
            return;
        }

        long pos = NowPlaying.positionMs() + c.lyricsOffsetMs;

        if (c.lyricsMode != 2) {
            // Sky/Follow not fully ported in this fixed version - fall through to bar
        }

        renderFloatingBar(graphics, font, ls, pos, c, sw, sh);
    }

    private static void renderFloatingBar(GuiGraphics graphics, Font font,
                                          List<Lyrics.Line> ls, long pos, Config c,
                                          int sw, int sh) {
        int idx = Lyrics.indexAt(ls, pos);
        if (idx < 0) return;

        Lyrics.Line cur = ls.get(idx);
        if (cur.text().isEmpty()) return;

        float size = c.lyricsSize;
        float barY = sh * c.lyricsY;

        int activeWord = Lyrics.wordIndexAt(cur, pos);
        List<Lyrics.Word> words = cur.words();

        float scale = size * 0.95f;
        int totalWidth = 0;
        if (words != null && !words.isEmpty()) {
            for (Lyrics.Word w : words) {
                totalWidth += font.width(w.text() + " ");
            }
        } else {
            totalWidth = font.width(cur.text());
        }
        totalWidth = (int) (totalWidth * scale);

        int padX = 18;
        int padY = 10;
        int barW = totalWidth + padX * 2 + 40;
        int barH = (int) (font.lineHeight * scale) + padY * 2;

        float barX;
        if (c.splitLyrics) {
            barX = sw * 0.55f;
        } else {
            barX = switch (c.lyricsAlign) {
                case 1 -> sw * 0.08f + barW / 2f;
                case 2 -> sw * 0.92f - barW / 2f;
                default -> sw / 2f;
            };
        }

        int left = (int) (barX - barW / 2f);
        int top  = (int) (barY - barH / 2f);

        // Background
        graphics.fill(left, top, left + barW, top + barH, 0xE01A1A1E);

        if (c.glowEnabled) {
            int glow = (c.accentColor & 0x00FFFFFF) | 0x22000000;
            graphics.fill(left - 1, top - 1, left + barW + 1, top + barH + 1, glow);
        }

        float textX = left + padX;
        float textY = top + padY;

        PoseStack pose = graphics.pose();

        if (words != null && !words.isEmpty() && c.wordRevealMode == 0) {
            for (int i = 0; i < words.size(); i++) {
                Lyrics.Word w = words.get(i);
                String part = w.text() + (i < words.size() - 1 ? " " : "");
                int color;
                if (i < activeWord) {
                    color = 0xFFCCCCCC;
                } else if (i == activeWord) {
                    color = c.accentColor;
                } else {
                    color = 0x66FFFFFF;
                }
                int a = (int) (((color >>> 24) & 0xFF) * c.lyricsOpacity);
                color = (a << 24) | (color & 0x00FFFFFF);

                pose.pushPose();
                pose.translate(textX, textY, 0);
                pose.scale(scale, scale, 1);
                graphics.drawString(font, part, 0, 0, color, false);
                pose.popPose();
                textX += font.width(part) * scale;
            }
        } else {
            int color = c.accentColor;
            int a = (int) (((color >>> 24) & 0xFF) * c.lyricsOpacity);
            color = (a << 24) | (color & 0x00FFFFFF);

            pose.pushPose();
            pose.translate(textX, textY, 0);
            pose.scale(scale, scale, 1);
            graphics.drawString(font, cur.text(), 0, 0, color, false);
            pose.popPose();
        }

        // Progress dots
        int dotsX = left + barW - 28;
        int dotsY = top + barH / 2;
        for (int i = 0; i < 3; i++) {
            int alpha = (i == 0) ? 0xFF : (i == 1 ? 0xAA : 0x55);
            int col = (alpha << 24) | (c.accentColor & 0x00FFFFFF);
            graphics.fill(dotsX + i * 6, dotsY - 1, dotsX + i * 6 + 3, dotsY + 2, col);
        }

        if (c.showContext && idx > 0 && c.fadePrevious) {
            Lyrics.Line prev = ls.get(idx - 1);
            if (!prev.text().isEmpty()) {
                float prevY = top - 22 * size;
                drawCentered(graphics, font, prev.text(), barX, prevY, size * 0.7f, 0x55FFFFFF);
            }
        }
    }

    static void drawCentered(GuiGraphics graphics, Font font, String text,
                             float cx, float y, float scale, int argb) {
        int a = (int) (((argb >>> 24) & 0xFF) * Config.I.lyricsOpacity);
        if (a < 8) return;
        int color = (a << 24) | (argb & 0x00FFFFFF);
        int w = font.width(text);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(cx, y, 0);
        pose.scale(scale, scale, 1);
        pose.translate(-w / 2f, 0, 0);
        graphics.drawString(font, text, 0, 0, color, false);
        pose.popPose();
    }
}
