package com.dogpound.pridecarts.carts;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

/** Draws chains (grey links) and ropes (sagging brown) between coupled PrideCarts. */
@Mod.EventBusSubscriber(modid = "pridecarts", value = Side.CLIENT)
public final class RenderCartLinks {
    private RenderCartLinks() {}

    @SubscribeEvent
    public static void render(RenderWorldLastEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.getRenderViewEntity() == null) return;
        float pt = e.getPartialTicks();
        Entity view = mc.getRenderViewEntity();
        double cx = view.lastTickPosX + (view.posX - view.lastTickPosX) * pt;
        double cy = view.lastTickPosY + (view.posY - view.lastTickPosY) * pt;
        double cz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        boolean any = false;
        for (Entity en : mc.world.loadedEntityList) {
            if (!(en instanceof IPrideCart) || en.getDistanceSq(view) > 64 * 64) continue;
            IPrideCart c = (IPrideCart) en;
            for (int s = 0; s < 2; s++) {
                int id = c.linkedId(s);
                if (id == 0 || id < en.getEntityId()) continue;          // each pair drawn once (from the lower id)
                Entity o = mc.world.getEntityByID(id);
                if (o == null) continue;
                Vec3d ca = pos(en, pt), cz2 = pos(o, pt);
                Vec3d dir = cz2.subtract(ca);
                double full = dir.lengthVector();
                if (full < 1e-3) continue;
                Vec3d u0 = dir.scale(1 / full);
                // hang it between the couplers on the cars' ends, not through their middles
                Vec3d a = ca.add(u0.scale(0.56)), z = cz2.subtract(u0.scale(0.56));
                boolean rope = c.linkedRope(s);
                double len = a.distanceTo(z);
                if (rope) {
                    double sag = Math.max(0.04, 0.35 - len * 0.12);
                    int seg = Math.max(6, (int) (len * 10));
                    for (int i = 0; i < seg; i++) {
                        Vec3d p0 = point(a, z, i / (double) seg, sag), p1 = point(a, z, (i + 1) / (double) seg, sag);
                        int tw = i % 2 == 0 ? 0 : 25;                               // twisted look
                        strip(b, p0, p1, cx, cy, cz, 0.05, true, 150 + tw, 108 + tw, 62 + tw);
                        strip(b, p0, p1, cx, cy, cz, 0.05, false, 130 + tw, 92 + tw, 50 + tw);
                    }
                } else {
                    // real chain: oval links, each turned 90° from the last
                    double linkLen = 0.16;
                    int links = Math.max(2, (int) Math.round(len / (linkLen * 0.8)));
                    for (int i = 0; i < links; i++) {
                        double f0 = i / (double) links, f1 = (i + 1) / (double) links;
                        Vec3d p0 = point(a, z, f0, 0.05), p1 = point(a, z, f1, 0.05);
                        link(b, p0, p1, cx, cy, cz, i % 2 == 0, i % 2 == 0 ? 168 : 128);
                    }
                }
                any = true;
            }
        }
        if (any) t.draw(); else b.finishDrawing();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    /** One oval chain link spanning p0..p1 (overlapping a little), lying flat or standing up. */
    private static void link(BufferBuilder b, Vec3d p0, Vec3d p1, double cx, double cy, double cz, boolean flat, int shade) {
        Vec3d d = p1.subtract(p0);
        double l = d.lengthVector();
        if (l < 1e-4) return;
        Vec3d u = d.scale(1 / l);
        Vec3d side = flat ? new Vec3d(-u.z, 0, u.x) : new Vec3d(0, 1, 0);
        double sl = side.lengthVector();
        if (sl < 1e-4) side = new Vec3d(1, 0, 0); else side = side.scale(1 / sl);
        Vec3d s = side.scale(0.045), ext = u.scale(l * 0.15);
        Vec3d a = p0.subtract(ext), z = p1.add(ext);
        int r = shade, g = shade + 2, bl = shade + 14;
        strip(b, a.add(s), z.add(s), cx, cy, cz, 0.014, !flat, r, g, bl);        // two long sides
        strip(b, a.subtract(s), z.subtract(s), cx, cy, cz, 0.014, !flat, r, g, bl);
        strip(b, a.add(s), a.subtract(s), cx, cy, cz, 0.014, !flat, r, g, bl);    // two rounded ends
        strip(b, z.add(s), z.subtract(s), cx, cy, cz, 0.014, !flat, r, g, bl);
    }

    /** A thin ribbon from a to b, lying flat (horizontal) or standing up. */
    private static void strip(BufferBuilder b, Vec3d a, Vec3d z, double cx, double cy, double cz, double w, boolean flat, int r, int g, int bl) {
        Vec3d d = z.subtract(a);
        Vec3d side = flat ? new Vec3d(-d.z, 0, d.x) : new Vec3d(0, 1, 0);
        double len = side.lengthVector();
        if (len < 1e-6) return;
        side = side.scale(w / len);
        int dr = r * 3 / 4, dg = g * 3 / 4, db = bl * 3 / 4;
        b.pos(a.x + side.x - cx, a.y + side.y - cy, a.z + side.z - cz).color(r, g, bl, 255).endVertex();
        b.pos(z.x + side.x - cx, z.y + side.y - cy, z.z + side.z - cz).color(r, g, bl, 255).endVertex();
        b.pos(z.x - side.x - cx, z.y - side.y - cy, z.z - side.z - cz).color(dr, dg, db, 255).endVertex();
        b.pos(a.x - side.x - cx, a.y - side.y - cy, a.z - side.z - cz).color(dr, dg, db, 255).endVertex();
    }

    private static Vec3d pos(Entity e, float pt) {
        return new Vec3d(e.lastTickPosX + (e.posX - e.lastTickPosX) * pt,
                e.lastTickPosY + (e.posY - e.lastTickPosY) * pt + 0.2,                  // coupler height
                e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * pt);
    }

    /** Point along the link with a parabolic sag. */
    private static Vec3d point(Vec3d a, Vec3d b, double f, double sag) {
        return new Vec3d(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f - sag * 4 * f * (1 - f), a.z + (b.z - a.z) * f);
    }
}
