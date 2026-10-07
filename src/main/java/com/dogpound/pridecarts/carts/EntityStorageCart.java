package com.dogpound.pridecarts.carts;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecartContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;

/**
 * A mini freight car that holds items (list §1, Freight): the size, the allowed cargo and the
 * special behaviour come from its {@link CartType}. Opens like a chest; the Cart Loader and
 * Unloader and hoppers see it as an inventory.
 */
public class EntityStorageCart extends EntityMinecartContainer implements IPrideCart {
    private static final DataParameter<Integer> TYPE = EntityDataManager.createKey(EntityStorageCart.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LINK0 = EntityDataManager.createKey(EntityStorageCart.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LINK1 = EntityDataManager.createKey(EntityStorageCart.class, DataSerializers.VARINT);
    private static final DataParameter<Byte> ROPES = EntityDataManager.createKey(EntityStorageCart.class, DataSerializers.BYTE);
    private boolean dumpedOnThisRail;

    public EntityStorageCart(World w) {
        super(w);
        widen();
    }

    public EntityStorageCart(World w, double x, double y, double z, CartType type) {
        super(w, x, y, z);
        widen();
        dataManager.set(TYPE, type.ordinal());
    }

    /** Vanilla's container cart always allocates exactly 36 slots; the warehouse car has 54. */
    private void widen() {
        net.minecraftforge.fml.common.ObfuscationReflectionHelper.setPrivateValue(EntityMinecartContainer.class, this,
                net.minecraft.util.NonNullList.withSize(54, ItemStack.EMPTY), "field_94113_a");
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(TYPE, CartType.BAGGAGE.ordinal());
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
    public Type getType() {
        return cartType() == CartType.HOPPER ? Type.HOPPER : Type.CHEST;
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
    public int getSizeInventory() {
        // the type isn't synced yet while the superclass constructor sizes the inventory: start at the biggest
        return dataManager == null ? 54 : cartType().size;
    }

    @Override
    public String getGuiID() {
        return "minecraft:chest";
    }

    @Override
    public Container createContainer(InventoryPlayer inv, EntityPlayer player) {
        return new ContainerChest(inv, this, player);
    }

    @Override
    public ItemStack getCartItem() {
        return CartRegistry.stack(cartType());
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack s) {
        switch (cartType()) {
            case REFRIGERATED: return s.getItem() instanceof ItemFood || hasOre(s, "food", "crop", "list");
            case ORE: return hasOre(s, "ore", "cluster", "crushed", "raw", "dust", "gem", "cobblestone", "stone", "gravel", "sand", "dirt", "clay", "coal");
            case LOG: return hasOre(s, "logWood", "plankWood", "treeSapling", "stickWood", "treeLeaves", "slabWood");
            case EXPLOSIVES: return hasOre(s, "gunpowder", "tnt", "blockTNT") || s.getItem() == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.TNT)
                    || s.getItem() == net.minecraft.init.Items.GUNPOWDER || s.getItem() == net.minecraft.init.Items.FIRE_CHARGE;
            default: return true;
        }
    }

    private static boolean hasOre(ItemStack s, String... prefixes) {
        if (s.isEmpty()) return false;
        for (int id : OreDictionary.getOreIDs(s)) {
            String name = OreDictionary.getOreName(id);
            for (String p : prefixes) if (name.startsWith(p)) return true;
        }
        return false;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote && ticksExisted % 20 == 3) linksChanged();
        if (world.isRemote || isDead) return;
        if (cartType() == CartType.HOPPER && ticksExisted % 4 == 0) suckItems();
    }

    /** Hopper car: pull loose items from the track it rolls over. */
    private void suckItems() {
        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, getEntityBoundingBox().grow(0.25, 0, 0.25));
        for (EntityItem it : items) {
            if (it.isDead) continue;
            ItemStack left = CartInv.insert(this, it.getItem().copy());
            if (left.isEmpty()) it.setDead();
            else it.setItem(left);
        }
    }

    /** Dump car: a powered activator rail tips everything out beside the track (once per pass). */
    @Override
    public void onActivatorRailPass(int x, int y, int z, boolean powered) {
        if (cartType() != CartType.DUMP || world.isRemote) return;
        if (powered && !dumpedOnThisRail) {
            dumpedOnThisRail = true;
            for (int i = 0; i < getSizeInventory(); i++) {
                ItemStack s = removeStackFromSlot(i);
                if (!s.isEmpty()) {
                    EntityItem e = new EntityItem(world, x + 0.5 + (rand.nextBoolean() ? 1.2 : -1.2), y + 0.6, z + 0.5, s);
                    world.spawnEntity(e);
                }
            }
        } else if (!powered) dumpedOnThisRail = false;
    }

    @Override
    public void killMinecart(DamageSource source) {
        // explosives car: fire or an explosion sets off what it carries
        if (cartType() == CartType.EXPLOSIVES && (source.isExplosion() || source.isFireDamage())) {
            int tnt = 0;
            for (int i = 0; i < getSizeInventory(); i++) tnt += getStackInSlot(i).getCount();
            if (tnt > 0) {
                clear();
                setDead();
                world.newExplosion(this, posX, posY, posZ, (float) Math.min(8, 2 + Math.sqrt(tnt)), true, true);
                return;
            }
        }
        setDead();
        if (world.getGameRules().getBoolean("doEntityDrops")) {
            InventoryHelper.dropInventoryItems(world, this, this);
            entityDropItem(CartRegistry.named(this, cartType()), 0);
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setString("pcType", cartType().id);
        super.writeEntityToNBT(nbt);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        dataManager.set(TYPE, CartRegistry.typeById(nbt.getString("pcType")).ordinal());
        super.readEntityFromNBT(nbt);
    }
}
