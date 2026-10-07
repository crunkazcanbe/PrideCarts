package com.dogpound.pridecarts.carts;

import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/** Places its mini car on a rail, like a vanilla minecart item. */
public class ItemPrideCart extends Item {
    public final CartType type;

    public ItemPrideCart(CartType type) {
        this.type = type;
        setRegistryName("pridecarts", "cart_" + type.id);
        setUnlocalizedName("pridecarts.cart_" + type.id);
        setMaxStackSize(1);
        setCreativeTab(CartRegistry.TAB);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        IBlockState s = world.getBlockState(pos);
        if (!BlockRailBase.isRailBlock(s)) return EnumActionResult.FAIL;
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            BlockRailBase.EnumRailDirection dir = s.getBlock() instanceof BlockRailBase
                    ? ((BlockRailBase) s.getBlock()).getRailDirection(world, pos, s, null) : BlockRailBase.EnumRailDirection.NORTH_SOUTH;
            double up = dir.isAscending() ? 0.5 : 0;
            EntityMinecart cart = type.create(world, pos.getX() + 0.5, pos.getY() + 0.0625 + up, pos.getZ() + 0.5);
            if (stack.hasDisplayName()) cart.setCustomNameTag(stack.getDisplayName());
            world.spawnEntity(cart);
        }
        if (!player.capabilities.isCreativeMode) stack.shrink(1);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tip, ITooltipFlag flag) {
        tip.add("§7" + type.tip);
        switch (type.family) {
            case STORAGE: tip.add("§8" + type.size + " slots"); break;
            case TANK: tip.add("§8" + type.size / 1000 + " buckets"); break;
            case POWER: tip.add(String.format("§8%,d FE", type.size)); break;
            case ENGINE:
                tip.add(type == CartType.STEAM_ENGINE ? String.format("§8Fire box: %,d s of fuel", type.size / 20) : String.format("§8Battery: %,d FE", type.size));
                if (EntityEngineCart.solarRate(type) > 0) tip.add("§8Solar: " + EntityEngineCart.solarRate(type) + " FE/t in full sun");
                tip.add("§8Ride it: W/S throttle · sneak-right-click (empty hand): control panel");
                break;
            default:
        }
    }
}
