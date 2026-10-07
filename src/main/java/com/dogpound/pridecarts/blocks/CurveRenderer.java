package com.dogpound.pridecarts.blocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a whole wide curve from its first piece: Minecraft's own rail texture bent smoothly along the arc, with a
 * half block of straight track at each end so it meets ordinary rails flush (2026-10-04: "the curve tracks don't
 * connect and don't look right"). Sleepers repeat every block of track length like straight rail.
 */
@SideOnly(Side.CLIENT)
public class CurveRenderer extends TileEntitySpecialRenderer<CurveRail.Tile> {
    @Override
    public void render(CurveRail.Tile t, double x, double y, double z, float pt, int destroy, float alpha) {
        if (!t.getPos().equals(t.origin)) return;                       // the first piece draws the whole curve
        BlockPos o = t.getPos();
        double ox = x - o.getX(), oy = y - o.getY(), oz = z - o.getZ();
        double baseY = o.getY() + oy + 0.0625;

        // ---- the centre line: straight half → arc → straight half, sampled with the distance travelled
        double r = t.radius, a0 = t.c0, a1 = t.c1, sign = Math.signum(a1 - a0);
        List<double[]> pts = new ArrayList<>();                      // {x, z, dirX, dirZ, s}
        double d0x = -Math.sin(a0) * sign, d0z = Math.cos(a0) * sign;
        double p0x = t.cx + Math.cos(a0) * r, p0z = t.cz + Math.sin(a0) * r;
        pts.add(new double[]{p0x - d0x * 0.5, p0z - d0z * 0.5, d0x, d0z, 0});
        double arcLen = Math.abs(a1 - a0) * r;
        int n = Math.max(8, (int) Math.ceil(arcLen * 8));
        for (int i = 0; i <= n; i++) {
            double a = a0 + (a1 - a0) * i / n;
            pts.add(new double[]{t.cx + Math.cos(a) * r, t.cz + Math.sin(a) * r, -Math.sin(a) * sign, Math.cos(a) * sign, 0.5 + arcLen * i / n});
        }
        double[] last = pts.get(pts.size() - 1);
        pts.add(new double[]{last[0] + last[2] * 0.5, last[1] + last[3] * 0.5, last[2], last[3], 0.5 + arcLen + 0.5});

        TextureAtlasSprite sp = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/rail_normal");
        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        int light = t.getWorld().getCombinedLight(o.up(), 0);
        float b = Math.max(0.2f, Math.max((light >> 20) & 15, (light >> 4) & 15) / 15f);
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        Tessellator tes = Tessellator.getInstance();
        BufferBuilder buf = tes.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i + 1 < pts.size(); i++) {
            double[] p = pts.get(i), q = pts.get(i + 1);
            // split at whole blocks of track so the texture repeats like straight rail
            double s0 = p[4], s1 = q[4];
            int k0 = (int) Math.floor(s0 + 1e-9), k1 = (int) Math.floor(s1 - 1e-9);
            if (k1 > k0) {
                double f = (k0 + 1 - s0) / (s1 - s0);
                double[] m = lerp(p, q, f);
                quad(buf, sp, p, m, k0, ox, oz, baseY, b);
                quad(buf, sp, m, q, k0 + 1, ox, oz, baseY, b);
            } else quad(buf, sp, p, q, k0, ox, oz, baseY, b);
        }
        tes.draw();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private static double[] lerp(double[] p, double[] q, double f) {
        double[] m = new double[5];
        for (int i = 0; i < 5; i++) m[i] = p[i] + (q[i] - p[i]) * f;
        return m;
    }

    /** one textured strip of track from p to q (a full block wide, across the travel direction) */
    private static void quad(BufferBuilder buf, TextureAtlasSprite sp, double[] p, double[] q, int k, double ox, double oz, double y, float b) {
        double v0 = p[4] - k, v1 = q[4] - k;
        double pnx = -p[3], pnz = p[2], qnx = -q[3], qnz = q[2];      // across the track
        float u0 = sp.getInterpolatedU(0), u1 = sp.getInterpolatedU(16);
        float tv0 = sp.getInterpolatedV(16 - v0 * 16), tv1 = sp.getInterpolatedV(16 - v1 * 16);
        buf.pos(p[0] - pnx * 0.5 + ox, y, p[1] - pnz * 0.5 + oz).tex(u0, tv0).color(b, b, b, 1f).endVertex();
        buf.pos(p[0] + pnx * 0.5 + ox, y, p[1] + pnz * 0.5 + oz).tex(u1, tv0).color(b, b, b, 1f).endVertex();
        buf.pos(q[0] + qnx * 0.5 + ox, y, q[1] + qnz * 0.5 + oz).tex(u1, tv1).color(b, b, b, 1f).endVertex();
        buf.pos(q[0] - qnx * 0.5 + ox, y, q[1] - qnz * 0.5 + oz).tex(u0, tv1).color(b, b, b, 1f).endVertex();
    }

    @Override public boolean isGlobalRenderer(CurveRail.Tile t) { return true; }
}
