package com.dogpound.pridecarts.carts;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderMinecart;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws a mini car with its own Blockbench model (the same model is its 3D item icon). Position,
 * slope tilt, turning and the hit wobble are vanilla's minecart maths; only the body differs.
 * Cars without a model yet fall back to the vanilla look.
 */
@SideOnly(Side.CLIENT)
public class RenderPrideCart<T extends EntityMinecart> extends RenderMinecart<T> {
    public RenderPrideCart(RenderManager m) {
        super(m);
    }

    @Override
    public void doRender(T cart, double x, double y, double z, float yaw, float pt) {
        if (!(cart instanceof IPrideCart) || !((IPrideCart) cart).cartType().modelled) {
            super.doRender(cart, x, y, z, yaw, pt);
            return;
        }
        GlStateManager.pushMatrix();
        long i = cart.getEntityId() * 493286711L;
        i = i * i * 4392167121L + i * 98761L;
        GlStateManager.translate((((i >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F,
                (((i >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F, (((i >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F);
        double d0 = cart.lastTickPosX + (cart.posX - cart.lastTickPosX) * pt;
        double d1 = cart.lastTickPosY + (cart.posY - cart.lastTickPosY) * pt;
        double d2 = cart.lastTickPosZ + (cart.posZ - cart.lastTickPosZ) * pt;
        Vec3d on = cart.getPos(d0, d1, d2);
        float pitch = cart.prevRotationPitch + (cart.rotationPitch - cart.prevRotationPitch) * pt;
        if (on != null) {
            Vec3d a = cart.getPosOffset(d0, d1, d2, 0.3D), b = cart.getPosOffset(d0, d1, d2, -0.3D);
            if (a == null) a = on;
            if (b == null) b = on;
            x += on.x - d0;
            y += (a.y + b.y) / 2.0D - d1;
            z += on.z - d2;
            Vec3d dir = b.addVector(-a.x, -a.y, -a.z);
            if (dir.lengthVector() != 0.0D) {
                dir = dir.normalize();
                yaw = (float) (Math.atan2(dir.z, dir.x) * 180.0D / Math.PI);
                pitch = (float) (Math.atan(dir.y) * 73.0D);
            }
        }
        GlStateManager.translate((float) x, (float) y + 0.375F, (float) z);
        GlStateManager.rotate(180.0F - yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-pitch, 0.0F, 0.0F, 1.0F);
        float roll = cart.getRollingAmplitude() - pt, dmg = Math.max(0, cart.getDamage() - pt);
        if (roll > 0.0F) GlStateManager.rotate(MathHelper.sin(roll) * roll * dmg / 10.0F * cart.getRollingDirection(), 1.0F, 0.0F, 0.0F);

        ItemStack stack = CartRegistry.stack(((IPrideCart) cart).cartType());
        IBakedModel model = Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getItemModel(stack);
        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.translate(-0.5F, -0.375F, -0.5F);   // model 0..16 → cart centred, wheels on the rail
        Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer()
                .renderModelBrightnessColor(model, cart.getBrightness(), 1f, 1f, 1f);
        GlStateManager.popMatrix();
        if (!renderOutlines) renderName(cart, x, y, z);
    }
}
