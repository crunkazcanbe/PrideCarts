package com.dogpound.pridecarts.carts;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.EnumMap;
import java.util.Map;

/** Registers every mini car: one item per {@link CartType}, one entity per family, vanilla minecart rendering. */
@Mod.EventBusSubscriber(modid = "pridecarts")
public final class CartRegistry {
    private CartRegistry() {}

    public static final Map<CartType, ItemPrideCart> ITEMS = new EnumMap<>(CartType.class);

    public static final CreativeTabs TAB = new CreativeTabs("pridecarts") {
        @Override public ItemStack getTabIconItem() { return stack(CartType.PASSENGER); }
    };

    public static ItemCoupler CHAIN, ROPE;

    /** A Cart Chain or Cart Rope item (dropped when a coupling breaks). */
    public static ItemStack coupler(CartLinks.Kind k) {
        ItemCoupler i = k == CartLinks.Kind.ROPE ? ROPE : CHAIN;
        return i == null ? ItemStack.EMPTY : new ItemStack(i);
    }

    public static ItemStack stack(CartType t) {
        ItemPrideCart i = ITEMS.get(t);
        return i == null ? ItemStack.EMPTY : new ItemStack(i);
    }

    /** The item a broken car drops, keeping its name tag. */
    public static ItemStack named(Entity cart, CartType t) {
        ItemStack s = stack(t);
        if (cart.hasCustomName()) s.setStackDisplayName(cart.getCustomNameTag());
        return s;
    }

    public static CartType typeById(String id) {
        for (CartType t : CartType.values()) if (t.id.equals(id)) return t;
        return CartType.PASSENGER;
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        for (CartType t : CartType.values()) {
            ItemPrideCart i = new ItemPrideCart(t);
            ITEMS.put(t, i);
            e.getRegistry().register(i);
        }
        e.getRegistry().registerAll(CHAIN = new ItemCoupler(CartLinks.Kind.CHAIN), ROPE = new ItemCoupler(CartLinks.Kind.ROPE));
    }

    @SubscribeEvent
    public static void entities(RegistryEvent.Register<EntityEntry> e) {
        int id = 0;
        e.getRegistry().registerAll(
                entry(EntitySeatCart.class, "seat_cart", id++),
                entry(EntityStorageCart.class, "storage_cart", id++),
                entry(EntityTankCart.class, "tank_cart", id++),
                entry(EntityPowerCart.class, "power_cart", id++),
                entry(EntityEngineCart.class, "engine_cart", id++));
    }

    private static EntityEntry entry(Class<? extends EntityMinecart> c, String name, int id) {
        return EntityEntryBuilder.create().entity(c).id(new ResourceLocation("pridecarts", name), id)
                .name("pridecarts." + name).tracker(80, 3, true).build();
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void models(ModelRegistryEvent e) {
        for (ItemPrideCart i : ITEMS.values())
            ModelLoader.setCustomModelResourceLocation(i, 0, new ModelResourceLocation(i.getRegistryName(), "inventory"));
        for (Item i : new Item[]{CHAIN, ROPE})
            ModelLoader.setCustomModelResourceLocation(i, 0, new ModelResourceLocation(i.getRegistryName(), "inventory"));
        RenderingRegistry.registerEntityRenderingHandler(EntitySeatCart.class, RenderPrideCart::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityStorageCart.class, RenderPrideCart::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityTankCart.class, RenderPrideCart::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityPowerCart.class, RenderPrideCart::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityEngineCart.class, RenderPrideCart::new);
    }
}
