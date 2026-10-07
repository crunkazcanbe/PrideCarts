package com.dogpound.pridecarts.net;

import com.dogpound.pridecarts.carts.EntityEngineCart;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/** PrideCarts' network channel: control-panel buttons go to the server as one small message. */
public final class PcNet {
    private PcNet() {}

    public static final SimpleNetworkWrapper CH = NetworkRegistry.INSTANCE.newSimpleChannel("pridecarts");

    public static void init() {
        CH.registerMessage(EngineAction.Handler.class, EngineAction.class, 0, Side.SERVER);
        CH.registerMessage(RouteSet.Handler.class, RouteSet.class, 1, Side.SERVER);
    }

    /** A button on the engine control panel: which engine, which action, an optional value. */
    public static class EngineAction implements IMessage {
        public int entity, action, value;

        public EngineAction() {}

        public EngineAction(int entity, int action, int value) {
            this.entity = entity;
            this.action = action;
            this.value = value;
        }

        @Override public void fromBytes(ByteBuf b) { entity = b.readInt(); action = b.readByte(); value = b.readInt(); }

        @Override public void toBytes(ByteBuf b) { b.writeInt(entity); b.writeByte(action); b.writeInt(value); }

        public static class Handler implements IMessageHandler<EngineAction, IMessage> {
            @Override
            public IMessage onMessage(EngineAction m, MessageContext ctx) {
                EntityPlayerMP p = ctx.getServerHandler().player;
                p.getServerWorld().addScheduledTask(() -> {
                    Entity e = p.world.getEntityByID(m.entity);
                    if (e instanceof EntityEngineCart && p.getDistanceSq(e) < 12 * 12) ((EntityEngineCart) e).panel(p, m.action, m.value);
                });
                return null;
            }
        }
    }

    /** The Route screen's whole route for one engine. */
    public static class RouteSet implements IMessage {
        public int entity;
        public String route = "";

        public RouteSet() {}

        public RouteSet(int entity, String route) { this.entity = entity; this.route = route; }

        @Override public void fromBytes(ByteBuf b) { entity = b.readInt(); route = net.minecraftforge.fml.common.network.ByteBufUtils.readUTF8String(b); }

        @Override public void toBytes(ByteBuf b) { b.writeInt(entity); net.minecraftforge.fml.common.network.ByteBufUtils.writeUTF8String(b, route.length() > 1024 ? route.substring(0, 1024) : route); }

        public static class Handler implements IMessageHandler<RouteSet, IMessage> {
            @Override
            public IMessage onMessage(RouteSet m, MessageContext ctx) {
                EntityPlayerMP p = ctx.getServerHandler().player;
                p.getServerWorld().addScheduledTask(() -> {
                    Entity e = p.world.getEntityByID(m.entity);
                    if (e instanceof EntityEngineCart && p.getDistanceSq(e) < 12 * 12) ((EntityEngineCart) e).setRoute(m.route);
                });
                return null;
            }
        }
    }
}
