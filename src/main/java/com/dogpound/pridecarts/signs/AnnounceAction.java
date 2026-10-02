package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.CartData;
import com.dogpound.pridecarts.Train;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextComponentString;

/**
 * announce — send a message to everyone on the train.
 *   line 3: the message to send (with & colour codes).
 *   line 4: optional extra text (appended with a space).
 * The message can contain {name} (replaced with the train's name) and {dest} (replaced with the destination).
 */
public class AnnounceAction implements SignAction {
    @Override
    public String[] names() {
        return new String[]{"announce"};
    }

    @Override
    public void enter(ActionSign sign, EntityMinecart cart, Train train) {
        String message = sign.arg(2);
        String extra = sign.arg(3);
        if (!extra.isEmpty()) {
            message += " " + extra;
        }
        
        // Replace placeholders
        String trainName = train.get("name");
        String destination = train.get("destination");
        message = message.replace("{name}", trainName)
                        .replace("{dest}", destination);
        
        // Replace & with § for Minecraft colour codes
        message = message.replace('&', '§');
        
        // Send message to all passengers
        for (net.minecraft.entity.Entity e : train.passengers()) {
            if (e instanceof EntityPlayer) e.sendMessage(new TextComponentString(message));
        }
    }
}
