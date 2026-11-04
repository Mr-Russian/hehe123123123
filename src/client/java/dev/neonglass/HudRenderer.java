package dev.neonglass;

import com.mojang.blaze3d.pipeline.RenderPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public final class HudRenderer {
    private static final Identifier COVER = NeonGlassClient.id("spotify_cover");
    private static final int WHITE = 0xFFF7F5FF;
    private static final int MUTED = 0xFFB9B4D1;

    public void render(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker delta) {
        if (!NeonGlassClient.CONFIG.hudEnabled || Minecraft.getInstance().options.hideGui) return;
        SpotifyClient.Track t = NeonGlassClient.SPOTIFY.track();
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        float scale = Math.max(0.65f, Math.min(1.25f, NeonGlassClient.CONFIG.hudScale / 100f));
        int w = Math.min((int)(sw * 0.90), (int)(760 * scale));
        int h = (int)(204 * scale);
        int x = (sw - w) / 2;
        int y = Math.max(18, (int)(sh * 0.24));
        int pad = (int)(18 * scale);
        int art = h - (int)(38 * scale);

        // Layered translucent fills approximate the frosted glass and neon bloom without shaders.
        g.fill(x-5, y-5, x+w+5, y+h+5, 0x2225BFFF);
        g.fill(x, y, x+w, y+h, 0xD9090620);
        g.fill(x+2, y+2, x+w-2, y+h-2, 0xAA09051D);
        g.fill(x+2, y+2, x+w-2, y+4, 0xAA24CFFF);
        g.fill(x+2, y+h-4, x+w-2, y+h-2, 0xCCEC32F4);
        g.fill(x+2, y+4, x+4, y+h-4, 0xCC25CFFF);
        g.fill(x+w-4, y+4, x+w-2, y+h-4, 0xCCFF4DB8);

        int ax = x + pad, ay = y + pad;
        g.fill(ax-2, ay-2, ax+art+2, ay+art+2, 0xFF5723D9);
        g.fill(ax, ay, ax+art, ay+art, 0xFF18102E);
        if (NeonGlassClient.SPOTIFY.hasArtwork()) {
            g.blit(RenderPipelines.GUI_TEXTURED, COVER, ax, ay, 0, 0, art, art, 256, 256);
        } else {
            g.fillGradient(ax, ay, ax+art, ay+art, 0xFF1429B8, 0xFFDA39B8);
            g.text(Minecraft.getInstance().font, "♪", ax + art/2 - 4, ay + art/2 - 5, WHITE, true);
        }

        int tx = ax + art + (int)(22*scale);
        int right = x+w-pad;
        int controlW = Math.max((int)(180*scale), w/3);
        int titleMax = right - controlW - tx - 12;
        String title = clip(t.title(), Math.max(8, titleMax / Math.max(5, Minecraft.getInstance().font.width("M"))));
        String artist = clip(t.artist(), Math.max(8, titleMax / Math.max(5, Minecraft.getInstance().font.width("M"))));
        g.text(Minecraft.getInstance().font, title, tx, ay + (int)(22*scale), WHITE, true);
        g.text(Minecraft.getInstance().font, artist, tx, ay + (int)(58*scale), MUTED, false);

        int cy = ay + (int)(14*scale);
        int cx = right - controlW;
        g.text(Minecraft.getInstance().font, "◀|", cx, cy + 17, WHITE, true);
        g.fill(cx+48, cy, cx+110, cy+62, 0x884F16B8);
        g.outline(cx+48, cy, 62, 62, 0xFFB94CFF);
        g.text(Minecraft.getInstance().font, t.playing() ? "Ⅱ" : "▶", cx+69, cy+20, WHITE, true);
        g.text(Minecraft.getInstance().font, "|▶", cx+130, cy+17, WHITE, true);

        int barY = y + h - (int)(46*scale);
        int barX = tx;
        int barW = right - tx;
        g.fill(barX, barY, barX+barW, barY+8, 0x665C3A75);
        float progress = t.durationMs() > 0 ? Math.min(1f, (float)t.progressMs()/t.durationMs()) : 0f;
        g.fillGradient(barX, barY, barX+(int)(barW*progress), barY+8, 0xFFDC35F5, 0xFFFFB9F3);
        g.text(Minecraft.getInstance().font, time(t.progressMs()), barX, barY+12, WHITE, false);
        g.text(Minecraft.getInstance().font, time(t.durationMs()), barX+barW-30, barY+12, WHITE, false);

        int volumeY = y+h-(int)(24*scale);
        int volumeX = x+(int)(art*0.72)+pad+art/2;
        // Volume display is configurable; Spotify Web API volume is not used because it is device-dependent.
        g.text(Minecraft.getInstance().font, "◖))", x+pad+art/2, volumeY-2, WHITE, true);
        int vx = tx, vw = Math.max(80, right-tx-(int)(60*scale));
        g.fill(vx, volumeY, vx+vw, volumeY+8, 0x665C3A75);
        g.fillGradient(vx, volumeY, vx+(int)(vw*NeonGlassClient.CONFIG.volume), volumeY+8, 0xFFB916F2, 0xFF1CCEFF);
        g.fill(vx+(int)(vw*NeonGlassClient.CONFIG.volume)-10, volumeY-7, vx+(int)(vw*NeonGlassClient.CONFIG.volume)+10, volumeY+15, WHITE);
        g.text(Minecraft.getInstance().font, Math.round(NeonGlassClient.CONFIG.volume*100)+"%", vx+vw+12, volumeY-2, WHITE, false);
    }

    private static String clip(String s, int max) { if (s == null) return ""; return s.length() <= max ? s : s.substring(0, Math.max(1,max-1))+"…"; }
    private static String time(int ms) { int sec=Math.max(0,ms/1000); return (sec/60)+":"+String.format("%02d",sec%60); }
}
