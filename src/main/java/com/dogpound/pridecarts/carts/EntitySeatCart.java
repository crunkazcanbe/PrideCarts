package com.dogpound.pridecarts.carts;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/**
 * A mini passenger car (list §1, Passenger). Same ride as a minecart, plus the car's perk for
 * whoever sits in it, checked once a second on the server.
 */
public class EntitySeatCart extends EntityMinecartEmpty implements IPrideCart {
    private static final DataParameter<Integer> TYPE = EntityDataManager.createKey(EntitySeatCart.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LINK0 = EntityDataManager.createKey(EntitySeatCart.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LINK1 = EntityDataManager.createKey(EntitySeatCart.class, DataSerializers.VARINT);
    private static final DataParameter<Byte> ROPES = EntityDataManager.createKey(EntitySeatCart.class, DataSerializers.BYTE);

    public EntitySeatCart(World w) {
        super(w);
    }

    public EntitySeatCart(World w, double x, double y, double z, CartType type) {
        super(w, x, y, z);
        dataManager.set(TYPE, type.ordinal());
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(TYPE, 0);
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
    public CartType cartType() {
        return CartType.byOrdinal(dataManager.get(TYPE));
    }

    @Override
    public IBlockState getDefaultDisplayTile() {
        return cartType() == CartType.PASSENGER ? super.getDefaultDisplayTile() : cartType().display();
    }

    @Override
    public int getDefaultDisplayTileOffset() {
        return 6;
    }

    @Override
    public ItemStack getCartItem() {
        return CartRegistry.stack(cartType());
    }

    @Override
    public void killMinecart(DamageSource source) {
        setDead();
        if (world.getGameRules().getBoolean("doEntityDrops")) entityDropItem(CartRegistry.named(this, cartType()), 0);
    }

    @Override
    public float getMaxCartSpeedOnRail() {
        return cartType() == CartType.HIGH_SPEED ? 2.4f : super.getMaxCartSpeedOnRail();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote && ticksExisted % 20 == 3) linksChanged();
        if (world.isRemote || ticksExisted % 20 != 0) return;
        for (Entity e : getPassengers()) if (e instanceof EntityPlayer) perk((EntityPlayer) e);
        if (cartType() == CartType.SLEEPER) CartPerks.sleeperNight(world);
    }

    private void perk(EntityPlayer p) {
        switch (cartType()) {
            case FIRST_CLASS:
                if (ticksExisted % 100 == 0) p.heal(1f);
                if (ticksExisted % 200 == 0) p.getFoodStats().addStats(1, 0.4f);
                break;
            case DINING:
                if (ticksExisted % 200 == 0 && p.getFoodStats().needFood()) p.getFoodStats().addStats(2, 0.6f);
                break;
            case AMBULANCE:
                p.heal(1f);
                p.removePotionEffect(MobEffects.POISON);
                p.removePotionEffect(MobEffects.WITHER);
                break;
            case OBSERVATION:
            case CABOOSE:
                p.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 300, 0, true, false));
                break;
            case CREW:
                p.addPotionEffect(new PotionEffect(MobEffects.HASTE, 60, 1, true, false));
                break;
            case MAINTENANCE_COACH: {
                ItemStack held = p.getHeldItemMainhand();
                if (held.isItemStackDamageable() && held.getItemDamage() > 0) held.setItemDamage(held.getItemDamage() - 1);
                break;
            }
            default:
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setString("pcType", cartType().id);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        dataManager.set(TYPE, CartRegistry.typeById(nbt.getString("pcType")).ordinal());
    }
}
