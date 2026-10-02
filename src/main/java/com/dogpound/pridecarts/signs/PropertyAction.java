package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.CartData;
import com.dogpound.pridecarts.Train;
import net.minecraft.entity.item.EntityMinecart;

import java.util.Map;

/**
 * property — change a train's properties.
 *   line 3: "name value", e.g. "speedlimit 0.8", "playerexit false".
 *   line 4 (optional): another "name value" to set.
 *   Special forms:
 *     "tag add X" / "tag remove X" / "addtag X" / "remtag X" edit the comma list property "tags"
 *     "destination X" or "dest X" sets "destination"
 *   Only names that exist in CartData.PROPS are set; others are ignored.
 */
public class PropertyAction implements SignAction {
    @Override
    public String[] names() {
        return new String[]{"property", "prop", "set"};
    }

    @Override
    public void enter(ActionSign sign, EntityMinecart cart, Train train) {
        String[] args = sign.arg(2).trim().split("\\s+", 2);
        if (args.length < 2) return;
        String name = args[0].toLowerCase();
        String value = args[1];
        if (name.equals("tag")) {                                      // "tag add X" / "tag remove X"
            String[] tv = value.trim().split("\\s+", 2);
            if (tv.length == 2 && (tv[0].equalsIgnoreCase("remove") || tv[0].equalsIgnoreCase("rem"))) { name = "remtag"; value = tv[1]; }
            else if (tv.length == 2 && tv[0].equalsIgnoreCase("add")) { name = "addtag"; value = tv[1]; }
        }
        
        if (name.equals("tag") || name.equals("addtag") || name.equals("remtag")) {
            String tag = value.trim();
            if (tag.isEmpty()) return;
            if (name.equals("tag") || name.equals("addtag")) {
                addTag(train, tag);
            } else {
                removeTag(train, tag);
            }
        } else if (name.equals("destination") || name.equals("dest")) {
            train.set("destination", value);
        } else if (CartData.PROPS.containsKey(name)) {
            train.set(name, value);
        }
        
        if (sign.arg(3) != null && !sign.arg(3).trim().isEmpty()) {
            String[] args2 = sign.arg(3).trim().split("\\s+", 2);
            if (args2.length >= 2) {
                String name2 = args2[0].toLowerCase();
                String value2 = args2[1];
                if (name2.equals("tag")) {
                    String[] tv = value2.trim().split("\\s+", 2);
                    if (tv.length == 2 && (tv[0].equalsIgnoreCase("remove") || tv[0].equalsIgnoreCase("rem"))) { name2 = "remtag"; value2 = tv[1]; }
                    else if (tv.length == 2 && tv[0].equalsIgnoreCase("add")) { name2 = "addtag"; value2 = tv[1]; }
                }
                if (name2.equals("tag") || name2.equals("addtag") || name2.equals("remtag")) {
                    String tag2 = value2.trim();
                    if (tag2.isEmpty()) return;
                    if (name2.equals("tag") || name2.equals("addtag")) {
                        addTag(train, tag2);
                    } else {
                        removeTag(train, tag2);
                    }
                } else if (name2.equals("destination") || name2.equals("dest")) {
                    train.set("destination", value2);
                } else if (CartData.PROPS.containsKey(name2)) {
                    train.set(name2, value2);
                }
            }
        }
    }
    
    private void addTag(Train train, String tag) {
        for (EntityMinecart c : train.carts) {
            java.util.List<String> tags = CartData.list(c, "tags");
            if (!tags.contains(tag)) {
                tags.add(tag);
                CartData.setList(c, "tags", tags);
            }
        }
    }
    
    private void removeTag(Train train, String tag) {
        for (EntityMinecart c : train.carts) {
            java.util.List<String> tags = CartData.list(c, "tags");
            tags.remove(tag);
            CartData.setList(c, "tags", tags);
        }
    }
}
