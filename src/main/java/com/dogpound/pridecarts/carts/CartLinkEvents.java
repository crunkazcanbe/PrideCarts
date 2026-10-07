package com.dogpound.pridecarts.carts;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.UUID;

/** Right-clicking carts with a coupler (pick, then couple) or with shears (uncouple). */
@Mod.EventBusSubscriber(modid = "pridecarts")
public final class CartLinkEvents {
    private CartLinkEvents() {}

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getTarget() instanceof EntityMinecart)) return;
        ItemStack held = e.getItemStack();
        EntityPlayer p = e.getEntityPlayer();
        EntityMinecart cart = (EntityMinecart) e.getTarget();
        boolean coupler = held.getItem() instanceof ItemCoupler, shears = held.getItem() == Items.SHEARS && p.isSneaking();
        if (!coupler && !shears) return;
        e.setCanceled(true);
        e.setCancellationResult(net.minecraft.util.EnumActionResult.SUCCESS);
        if (e.getWorld().isRemote) return;

        if (shears) {
            int n = 0;
            for (CartLinks.Kind k : CartLinks.uncoupleAll(cart)) { cart.entityDropItem(CartRegistry.coupler(k), 0.5f); n++; }
            say(p, n == 0 ? "§7That cart isn't coupled" : "§a✂ Uncoupled (" + n + ")");
            if (n > 0) e.getWorld().playSound(null, cart.posX, cart.posY, cart.posZ, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.NEUTRAL, 1f, 1.2f);
            return;
        }
        CartLinks.Kind kind = ((ItemCoupler) held.getItem()).kind;
        NBTTagCompound t = held.hasTagCompound() ? held.getTagCompound() : new NBTTagCompound();
        if (!t.hasUniqueId("first")) {
            t.setUniqueId("first", cart.getUniqueID());
            held.setTagCompound(t);
            say(p, "§d▶ " + CartLinks.name(kind) + ": now right-click the cart to couple it to");
            return;
        }
        UUID first = t.getUniqueId("first");
        t.removeTag("firstMost"); t.removeTag("firstLeast");
        held.setTagCompound(t.hasNoTags() ? null : t);
        net.minecraft.entity.Entity a = ((WorldServer) e.getWorld()).getEntityFromUuid(first);
        if (!(a instanceof EntityMinecart)) { say(p, "§cThe first cart is gone: start again"); return; }
        String err = CartLinks.couple((EntityMinecart) a, cart, kind);
        if (err != null) { say(p, "§c" + err); return; }
        if (!p.capabilities.isCreativeMode) held.shrink(1);
        e.getWorld().playSound(null, cart.posX, cart.posY, cart.posZ,
                kind == CartLinks.Kind.ROPE ? SoundEvents.ENTITY_LEASHKNOT_PLACE : SoundEvents.BLOCK_METAL_PLACE, SoundCategory.NEUTRAL, 1f, 1f);
        say(p, "§a🔗 Coupled · train is now " + CartLinks.train(cart).size() + " carts");
    }

    private static void say(EntityPlayer p, String s) {
        p.sendStatusMessage(new TextComponentString(s), true);
    }
}
