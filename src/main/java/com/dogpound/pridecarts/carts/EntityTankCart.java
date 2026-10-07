package com.dogpound.pridecarts.carts;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraft.item.ItemStack;

/**
 * Mini tank car (list §1: tanker, liquid tanker, gas tanker). A real Forge fluid tank: buckets
 * and tanks fill/empty it on right-click, pipes and the Cart Loader/Unloader see it as a tank.
 */
public class EntityTankCart extends EntityMinecart implements IPrideCart {
    private static final DataParameter<Integer> TYPE = EntityDataManager.createKey(EntityTankCart.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LINK0 = EntityDataManager.createKey(EntityTankCart.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LINK1 = EntityDataManager.createKey(EntityTankCart.class, DataSerializers.VARINT);
    private static final DataParameter<Byte> ROPES = EntityDataManager.createKey(EntityTankCart.class, DataSerializers.BYTE);
    private final FluidTank tank = new FluidTank(64000) {
        @Override
        public boolean canFillFluidType(FluidStack f) {
            if (f == null || f.getFluid() == null) return false;
            CartType t = cartType();
            if (t == CartType.GAS_TANKER) return f.getFluid().isGaseous(f);
            if (t == CartType.LIQUID_TANKER) return !f.getFluid().isGaseous(f);
            return true;
        }
    };

    public EntityTankCart(World w) {
        super(w);
    }

    public EntityTankCart(World w, double x, double y, double z, CartType type) {
        super(w, x, y, z);
        dataManager.set(TYPE, type.ordinal());
        tank.setCapacity(type.size);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(TYPE, CartType.TANKER.ordinal());
        dataManager.register(LINK0, 0);
        dataManager.register(LINK1, 0);
        dataManager.register(ROPES, (byte) 0);
    }

    @Override
    public int linkedId(int slot) {
        return dataManager.get(slot == 0 ? LINK0 : LINK1);
    }

    @Override
    public boolean linkedRope(int slot) {
        return (dataManager.get(ROPES) & (1 << slot)) != 0;
    }

    @Override
    public void linksChanged() {
        int[] ids = CartLinks.resolve(this);
        dataManager.set(LINK0, ids[0]);
        dataManager.set(LINK1, ids[1]);
        dataManager.set(ROPES, (byte) ids[2]);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote && ticksExisted % 20 == 3) linksChanged();
    }

    @Override
    public CartType cartType() {
        return CartType.byOrdinal(dataManager.get(TYPE));
    }

    public FluidTank tank() {
        return tank;
    }

    @Override
    public Type getType() {
        return Type.RIDEABLE;   // nothing in vanilla matches; only used for vanilla's cart item
    }

    @Override
    public boolean canBeRidden() {
        return false;
    }

    @Override
    public IBlockState getDefaultDisplayTile() {
        return cartType().display();
    }

    @Override
    public int getDefaultDisplayTileOffset() {
        return 8;
    }

    @Override
    public ItemStack getCartItem() {
        return CartRegistry.stack(cartType());
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (world.isRemote) return true;
        if (!FluidUtil.interactWithFluidHandler(player, hand, tank)) {
            FluidStack f = tank.getFluid();
            player.sendStatusMessage(new TextComponentString("§d" + cartType().id.replace('_', ' ') + "§7 · "
                    + (f == null ? "empty" : f.getLocalizedName() + " " + f.amount + " / " + tank.getCapacity() + " mB")), true);
        }
        return true;
    }

    @Override
    public void killMinecart(DamageSource source) {
        setDead();
        if (world.getGameRules().getBoolean("doEntityDrops")) entityDropItem(CartRegistry.named(this, cartType()), 0);
    }

    @Override
    public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
        return cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(cap, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
        return cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY ? (T) tank : super.getCapability(cap, facing);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setString("pcType", cartType().id);
        nbt.setTag("tank", tank.writeToNBT(new NBTTagCompound()));
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        CartType t = CartRegistry.typeById(nbt.getString("pcType"));
        dataManager.set(TYPE, t.ordinal());
        tank.setCapacity(t.size);
        tank.readFromNBT(nbt.getCompoundTag("tank"));
    }
}
