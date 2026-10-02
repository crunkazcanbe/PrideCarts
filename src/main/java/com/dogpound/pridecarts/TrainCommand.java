package com.dogpound.pridecarts;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TrainCommand extends CommandBase {
    private final String name;

    public TrainCommand(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<String> getAliases() {
        List<String> aliases = new ArrayList<>();
        if (name.equals("train")) aliases.add("cart");
        return aliases;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "Usage: /" + name + " <info|settings|set|tag|dest|route|launch|stop|destroy|eject|link|unlink>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) throw new CommandException(getUsage(sender));
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        EntityMinecart cart = null;
        if (player.getRidingEntity() instanceof EntityMinecart) {
            cart = (EntityMinecart) player.getRidingEntity();
        } else {
            Vec3d eyes = player.getPositionEyes(1.0F);
            Vec3d look = player.getLook(1.0F);
            Vec3d end = eyes.add(look.scale(6.0D));
            AxisAlignedBB box = player.getEntityBoundingBox().grow(6.0D);
            List<EntityMinecart> carts = player.world.getEntitiesWithinAABB(EntityMinecart.class, box, e -> {
                RayTraceResult intercept = e.getEntityBoundingBox().grow(0.3D).calculateIntercept(eyes, end);
                return intercept != null;
            });
            if (!carts.isEmpty()) cart = carts.get(0);
        }
        if (cart == null) throw new CommandException("Ride a minecart or look at one");
        Train t = Train.of(cart);

        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "info":
                info(player, t);
                break;
            case "settings":
                settings(player, t);
                break;
            case "set":
                if (args.length < 3) throw new CommandException("Usage: /" + name + " set <property> <value>");
                set(player, t, args[1], args, 2);
                break;
            case "tag":
                if (args.length < 3) throw new CommandException("Usage: /" + name + " tag add|remove <tag>");
                tag(player, t, args[1], args[2]);
                break;
            case "dest":
                if (args.length < 2) throw new CommandException("Usage: /" + name + " dest <destination>");
                t.set("destination", args[1]);
                player.sendMessage(new TextComponentString("Destination set to: §f" + args[1]));
                break;
            case "route":
                if (args.length < 2) throw new CommandException("Usage: /" + name + " route <a,b,c>");
                t.set("route", String.join(",", java.util.Arrays.copyOfRange(args, 1, args.length)));
                player.sendMessage(new TextComponentString("Route set to: §f" + String.join(",", java.util.Arrays.copyOfRange(args, 1, args.length))));
                break;
            case "launch":
                double speed = 0.4;
                if (args.length > 1) {
                    try {
                        speed = Double.parseDouble(args[1]);
                    } catch (NumberFormatException ignored) {}
                }
                String dir = args.length > 2 ? args[2] : "";
                EnumFacing facing = Train.direction(dir, t.moving(), null);
                t.launch(facing, speed);
                for (EntityMinecart c : t.carts) CartData.release(c);
                player.sendMessage(new TextComponentString("Launched train §f" + speed + " §f" + dir));
                break;
            case "stop":
                t.stop();
                for (EntityMinecart c : t.carts) CartData.hold(c, -1, 0L, false, "none", 0);
                player.sendMessage(new TextComponentString("Train stopped"));
                break;
            case "destroy":
                try {
                    CartEvents.ejecting = true;
                    t.passengers().forEach(net.minecraft.entity.Entity::dismountRidingEntity);
                    for (EntityMinecart c : t.carts) c.setDead();
                } finally {
                    CartEvents.ejecting = false;
                }
                player.sendMessage(new TextComponentString("Train destroyed"));
                break;
            case "eject":
                CartEvents.ejecting = true;
                try {
                    t.passengers().forEach(net.minecraft.entity.Entity::dismountRidingEntity);
                } finally {
                    CartEvents.ejecting = false;
                }
                player.sendMessage(new TextComponentString("Passengers ejected"));
                break;
            case "link":
                if (!Loader.isModLoaded("railcraft")) throw new CommandException("Needs Railcraft for linking");
                EntityMinecart target = null;
                if (player.getRidingEntity() instanceof EntityMinecart) {
                    target = (EntityMinecart) player.getRidingEntity();
                } else {
                    Vec3d eyes = player.getPositionEyes(1.0F);
                    Vec3d look = player.getLook(1.0F);
                    Vec3d end = eyes.add(look.scale(6.0D));
                    AxisAlignedBB box = player.getEntityBoundingBox().grow(6.0D);
                    List<EntityMinecart> carts = player.world.getEntitiesWithinAABB(EntityMinecart.class, box, e -> {
                        RayTraceResult intercept = e.getEntityBoundingBox().grow(0.3D).calculateIntercept(eyes, end);
                        return intercept != null;
                    });
                    if (!carts.isEmpty()) target = carts.get(0);
                }
                if (target == null) throw new CommandException("Looking at a cart to link to");
                if (RailcraftLinks.link(cart, target)) player.sendMessage(new TextComponentString("Carts linked"));
                else player.sendMessage(new TextComponentString("Failed to link carts"));
                break;
            case "unlink":
                if (!Loader.isModLoaded("railcraft")) throw new CommandException("Needs Railcraft for unlinking");
                RailcraftLinks.unlink(cart);
                player.sendMessage(new TextComponentString("Cart unlinked"));
                break;
            default:
                throw new CommandException("Unknown subcommand: " + subcommand);
        }
    }

    private void info(EntityPlayerMP player, Train t) {
        TextComponentString message = new TextComponentString("§dTrain Info:\n");
        message.appendText("Carts: §f" + t.carts.size() + "\n");
        message.appendText("Speed: §f" + String.format("%.2f", t.speed()) + "\n");
        message.appendText("Direction: §f" + (t.moving() != null ? t.moving().getName() : "none") + "\n");
        for (Map.Entry<String, String[]> prop : CartData.PROPS.entrySet()) {
            String value = t.get(prop.getKey());
            if (!value.equals(prop.getValue()[0])) {
                message.appendText(prop.getKey() + ": §f" + value + "\n");
            }
        }
        player.sendMessage(message);
    }

    private void settings(EntityPlayerMP player, Train t) {
        TextComponentString message = new TextComponentString("§dTrain Settings:\n");
        for (Map.Entry<String, String[]> prop : CartData.PROPS.entrySet()) {
            String value = t.get(prop.getKey());
            message.appendText("§f" + prop.getKey() + " §7(" + value + "): " + prop.getValue()[1] + "\n");
        }
        player.sendMessage(message);
    }

    private void set(EntityPlayerMP player, Train t, String name, String[] args, int start) throws CommandException {
        String value = String.join(" ", java.util.Arrays.copyOfRange(args, start, args.length));
        if (!CartData.PROPS.containsKey(name)) {
            TextComponentString message = new TextComponentString("§dValid properties:\n");
            for (String key : CartData.PROPS.keySet()) {
                message.appendText("§f" + key + "\n");
            }
            player.sendMessage(message);
            throw new CommandException("Unknown property: " + name);
        }
        t.set(name, value);
        player.sendMessage(new TextComponentString("Set " + name + " to: §f" + value));
    }

    private void tag(EntityPlayerMP player, Train t, String action, String tag) {
        List<String> tags = new ArrayList<>(CartData.list(t.carts.get(0), "tags"));
        if (action.equals("add")) {
            if (!tags.contains(tag)) tags.add(tag);
        } else if (action.equals("remove")) {
            tags.remove(tag);
        }
        for (EntityMinecart c : t.carts) CartData.setList(c, "tags", tags);
        player.sendMessage(new TextComponentString("Tags updated"));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, net.minecraft.util.math.BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "info", "settings", "set", "tag", "dest", "route", "launch", "stop", "destroy", "eject", "link", "unlink");
        } else if (args.length == 2 && args[0].equals("set")) {
            return getListOfStringsMatchingLastWord(args, CartData.PROPS.keySet());
        } else if (args.length == 2 && args[0].equals("tag")) {
            return getListOfStringsMatchingLastWord(args, "add", "remove");
        } else if (args.length == 3 && args[0].equals("launch")) {
            return getListOfStringsMatchingLastWord(args, "continue", "back", "left", "right", "north", "east", "south", "west");
        }
        return Collections.emptyList();
    }
}
