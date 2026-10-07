package com.dogpound.pridecarts.blocks;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** PrideCarts' rail-side blocks (requested feature). */
@Mod.EventBusSubscriber(modid = "pridecarts")
public final class PcBlocks {
    private PcBlocks() {}
    public static final Block LOADER = new CartTransferBlock("cart_loader", true);
    public static final Block UNLOADER = new CartTransferBlock("cart_unloader", false);
    public static final Block OBSERVER = new RailwayObserverBlock();
    public static final Block SIGNAL_POST = new SignalPost();
    public static final CurveRail CURVE = new CurveRail();
    public static final CrossingTrack CROSSING = new CrossingTrack();
    static final Block[] ALL = { LOADER, UNLOADER, OBSERVER };
    public static final SmartRail[] RAILS = new SmartRail[SmartRail.Kind.values().length];
    static { for (SmartRail.Kind k : SmartRail.Kind.values()) RAILS[k.ordinal()] = new SmartRail(k); }
    public static final SwitchTrack[] SWITCHES = new SwitchTrack[SwitchTrack.Kind.values().length];
    static { for (SwitchTrack.Kind k : SwitchTrack.Kind.values()) SWITCHES[k.ordinal()] = new SwitchTrack(k); }
    public static final DetectorTrack[] DETECTORS = new DetectorTrack[DetectorTrack.Kind.values().length];
    static { for (DetectorTrack.Kind k : DetectorTrack.Kind.values()) DETECTORS[k.ordinal()] = new DetectorTrack(k); }

    @SubscribeEvent public static void blocks(RegistryEvent.Register<Block> e) { e.getRegistry().registerAll(ALL); e.getRegistry().registerAll(RAILS); e.getRegistry().register(SIGNAL_POST); e.getRegistry().register(CURVE); e.getRegistry().register(CROSSING);
        net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(CurveRail.Tile.class, new net.minecraft.util.ResourceLocation("pridecarts", "curve_rail")); e.getRegistry().registerAll(SWITCHES); e.getRegistry().registerAll(DETECTORS); }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        for (Block b : ALL) e.getRegistry().register(new ItemBlock(b).setRegistryName(b.getRegistryName()));
        for (Block b : RAILS) e.getRegistry().register(new ItemBlock(b).setRegistryName(b.getRegistryName()));
        for (Block b : SWITCHES) e.getRegistry().register(new ItemBlock(b).setRegistryName(b.getRegistryName()));
        e.getRegistry().register(new ItemBlock(SIGNAL_POST).setRegistryName(SIGNAL_POST.getRegistryName()));
        e.getRegistry().register(new ItemBlock(CROSSING).setRegistryName(CROSSING.getRegistryName()));
        for (int r : ItemCurve.RADII) e.getRegistry().register(new ItemCurve(r));
        for (Block b : DETECTORS) e.getRegistry().register(new ItemBlock(b).setRegistryName(b.getRegistryName()));
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void models(ModelRegistryEvent e) {
        for (Block b : ALL) ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(b), 0, new ModelResourceLocation(b.getRegistryName(), "facing=north,powered=false"));
        for (Block b : RAILS) ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(b), 0, new ModelResourceLocation(b.getRegistryName(), "inventory"));
        for (ItemCurve c : ItemCurve.ALL) ModelLoader.setCustomModelResourceLocation(c, 0, new ModelResourceLocation(c.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CROSSING), 0, new ModelResourceLocation(CROSSING.getRegistryName(), "inventory"));
        net.minecraftforge.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(CurveRail.Tile.class, new CurveRenderer());
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(SIGNAL_POST), 0, new ModelResourceLocation(SIGNAL_POST.getRegistryName(), "inventory"));
        for (Block b : SWITCHES) ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(b), 0, new ModelResourceLocation(b.getRegistryName(), "inventory"));
        for (Block b : DETECTORS) ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(b), 0, new ModelResourceLocation(b.getRegistryName(), "inventory"));
    }
}
