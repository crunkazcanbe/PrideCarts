package com.dogpound.pridecarts.carts;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.List;

/** Cart Chain / Cart Rope: right-click one cart, then the next, to couple them (see {@link CartLinks}). */
public class ItemCoupler extends Item {
    public final CartLinks.Kind kind;

    public ItemCoupler(CartLinks.Kind kind) {
        this.kind = kind;
        String id = kind == CartLinks.Kind.ROPE ? "cart_rope" : "cart_chain";
        setRegistryName("pridecarts", id);
        setUnlocalizedName("pridecarts." + id);
        setCreativeTab(CartRegistry.TAB);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tip, ITooltipFlag flag) {
        tip.add(kind == CartLinks.Kind.ROPE ? "§7Slack coupling: carts swing up to 3 blocks apart" : "§7Tight coupling: carts ride close together");
        tip.add("§8Right-click a cart, then the next one · up to " + CartLinks.MAX_TRAIN + " per train");
        tip.add("§8Sneak-right-click a cart with shears to uncouple it");
        if (stack.hasTagCompound() && stack.getTagCompound().hasUniqueId("first")) tip.add("§d▶ First cart picked: now click the next one");
    }
}
