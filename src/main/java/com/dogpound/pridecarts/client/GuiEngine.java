package com.dogpound.pridecarts.client;

import com.dogpound.pridecarts.carts.CartType;
import com.dogpound.pridecarts.carts.EntityEngineCart;
import com.dogpound.pridecarts.carts.IPrideCart;
import com.dogpound.pridecarts.net.PcNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mini engine control panel (sneak-right-click an engine with an empty hand): throttle notches, reverse,
 * emergency stop, the fuel/battery gauge with solar charging, live speed and odometer, the cars in the
 * train with how each is coupled, horn, smart-track obedience and uncoupling.
 */
@SideOnly(Side.CLIENT)
public class GuiEngine extends GuiScreen {
    private static final String[] NOTCH = {"Stop", "Crawl", "Steady", "Fast", "Full"};
    private static final int[] NOTCH_COLOR = {0xFFE8434F, 0xFF5BCEFA, 0xFF4AD06A, 0xFFF2C94C, 0xFFFF7AD8};

    private final EntityEngineCart engine;
    private double lastX, lastZ, speed;          // blocks per second, smoothed
    private int scroll;

    public GuiEngine(EntityEngineCart engine) {
        this.engine = engine;
        lastX = engine.posX;
        lastZ = engine.posZ;
    }

    public static void open(EntityEngineCart e) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiEngine(e));
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    @Override
    public void updateScreen() {
        if (engine.isDead || mc.player.getDistanceSq(engine) > 16 * 16) { mc.displayGuiScreen(null); return; }
        double d = Math.sqrt((engine.posX - lastX) * (engine.posX - lastX) + (engine.posZ - lastZ) * (engine.posZ - lastZ)) * 20;
        speed = speed * 0.7 + d * 0.3;
        lastX = engine.posX;
        lastZ = engine.posZ;
    }

    private void send(int action, int value) {
        PcNet.CH.sendToServer(new PcNet.EngineAction(engine.getEntityId(), action, value));
    }

    // ---------------------------------------------------------------- layout

    private PrideFrame frame() { return PrideFrame.sized(width, height, 600, 330); }

    private int colW(PrideFrame f) { return (f.cw - 16) / 3; }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        PrideFrame f = frame();
        CartType t = engine.cartType();
        String name = I18n.format("item.pridecarts.cart_" + t.id + ".name");
        f.draw(this, name, engine.powered() ? "§a● running" : engine.throttle() > 0 ? "§c● no power" : "§7● idle");
        FontRenderer fr = fontRenderer;
        int cw = colW(f), x1 = f.cx, x2 = f.cx + cw + 8, x3 = f.cx + 2 * (cw + 8);
        int top = f.cy, bottom = f.y + f.h - 34;

        // -------- throttle
        PrideFrame.card(x1, top, cw, bottom - top, PrideFrame.PINK);
        fr.drawStringWithShadow("§lThrottle", x1 + 6, top + 6, 0xFFFFFF);
        int ty = top + 20, th = Math.max(14, (bottom - top - 76) / 5);
        for (int n = EntityEngineCart.NOTCHES; n >= 0; n--) {
            int y = ty + (EntityEngineCart.NOTCHES - n) * (th + 2);
            boolean on = engine.throttle() == n, over = in(mx, my, x1 + 6, y, cw - 12, th);
            PrideFrame.tile(x1 + 6, y, cw - 12, th, NOTCH_COLOR[n], over, on);
            fr.drawStringWithShadow((on ? "§f▶ " : "§7") + n + "  " + NOTCH[n], x1 + 12, y + (th - 8) / 2f, 0xFFFFFF);
            int bars = n;
            for (int b = 0; b < bars; b++) Gui.drawRect(x1 + cw - 14 - b * 5, y + th - 4 - b * 2, x1 + cw - 11 - b * 5, y + th - 3, NOTCH_COLOR[n]);
        }
        int by = ty + 5 * (th + 2) + 4;
        PrideFrame.button(x1 + 6, by, cw - 12, 16, "⇄ Reverse", PrideFrame.BUTTON, mx, my);
        PrideFrame.button(x1 + 6, by + 20, cw - 12, 16, "■ Emergency stop", 0xFF8A1E2A, mx, my);

        // -------- power
        PrideFrame.card(x2, top, cw, bottom - top, PrideFrame.BLUE);
        fr.drawStringWithShadow("§lPower", x2 + 6, top + 6, 0xFFFFFF);
        boolean steam = engine.isSteam();
        int cap = Math.max(1, engine.capacity()), fuel = engine.fuelShown();
        float pct = Math.min(1f, fuel / (float) cap);
        int gy = top + 22, gw = cw - 12;
        Gui.drawRect(x2 + 6, gy, x2 + 6 + gw, gy + 12, 0xFF1C1530);
        int fill = (int) (gw * pct), col = steam ? 0xFFFF8A2A : 0xFF5BCEFA;
        if (fill > 0) PrideFrame.gradient(x2 + 6, gy, x2 + 6 + fill, gy + 12, col, PrideFrame.brighten(col));
        String g = Math.round(pct * 100) + "%";
        fr.drawStringWithShadow(g, x2 + 6 + (gw - fr.getStringWidth(g)) / 2f, gy + 2, 0xFFFFFF);
        int ly = gy + 18;
        ly = line(fr, x2 + 6, ly, steam ? String.format("§6Fire box: §f%,d s", fuel / 20) : "§bBattery: §f" + kilo(fuel) + " / " + kilo(cap) + " FE");
        int solar = EntityEngineCart.solarRate(t);
        if (solar > 0) {
            boolean sun = engine.world.isDaytime() && engine.world.canSeeSky(engine.getPosition().up());
            ly = line(fr, x2 + 6, ly, sun ? "§e☀ Charging +" + (engine.world.isRaining() ? solar / 3 : solar) + " FE/t" : "§8☾ No sun: on battery");
        }
        if (engine.throttle() > 0) ly = line(fr, x2 + 6, ly, steam ? "§7Burning " + (engine.throttle() >= 3 ? 2 : 1) + " fuel tick/t" : "§7Using " + 20 * engine.throttle() + " FE/t");
        ly += 6;
        String sp = String.format("%.1f", speed * 3.6);
        fr.drawStringWithShadow("§7Speed", x2 + 6, ly, 0xFFFFFF);
        org.lwjgl.opengl.GL11.glPushMatrix();
        org.lwjgl.opengl.GL11.glTranslatef(x2 + 6, ly + 10, 0);
        org.lwjgl.opengl.GL11.glScalef(2, 2, 1);
        fr.drawStringWithShadow(sp, 0, 0, 0xF5A9B8);
        org.lwjgl.opengl.GL11.glPopMatrix();
        fr.drawStringWithShadow("§7km/h  §8(" + String.format("%.1f", speed) + " blocks/s)", x2 + 10 + fr.getStringWidth(sp) * 2, ly + 16, 0xFFFFFF);
        ly += 32;
        float odo = engine.odometer();
        line(fr, x2 + 6, ly, "§7Odometer: §f" + (odo >= 1000 ? String.format("%.2f km", odo / 1000) : String.format("%.0f m", odo)));

        // -------- train
        List<Entity> train = train();
        PrideFrame.card(x3, top, cw, bottom - top, 0xFF4AD06A);
        fr.drawStringWithShadow("§lTrain §7(" + train.size() + (train.size() == 1 ? " car)" : " cars)"), x3 + 6, top + 6, 0xFFFFFF);
        int rowH = 13, listTop = top + 20, listH = bottom - listTop - 6, visible = listH / rowH;
        scroll = Math.max(0, Math.min(scroll, train.size() - visible));
        PrideFrame.clip(x3 + 2, listTop, cw - 4, listH);
        for (int i = 0; i < train.size(); i++) {
            int ry = listTop + (i - scroll) * rowH;
            if (ry < listTop - rowH || ry > listTop + listH) continue;
            Entity c = train.get(i);
            String label = c instanceof IPrideCart ? I18n.format("item.pridecarts.cart_" + ((IPrideCart) c).cartType().id + ".name") : c.getName();
            if (c == engine) label = "§d★ §f" + label;
            fr.drawStringWithShadow(fr.trimStringToWidth(label, cw - 40), x3 + 8, ry + 2, 0xFFFFFF);
            if (i + 1 < train.size()) fr.drawStringWithShadow(rope(c, train.get(i + 1)) ? "§6~" : "§7≡", x3 + cw - 16, ry + 8, 0xFFFFFF);
        }
        PrideFrame.unclip();
        PrideFrame.scrollbar(x3 + cw - 5, listTop, listH, scroll, visible, train.size());

        // -------- bottom row
        int bw = (f.cw - 32) / 5, bx = f.cx, byy = f.y + f.h - 26;
        PrideFrame.button(bx, byy, bw, 18, engine.isSteam() ? "Whistle" : "Horn", 0xFF6A3FA0, mx, my);
        PrideFrame.button(bx + bw + 8, byy, bw, 18, "Tracks: " + (engine.obeysTracks() ? "§aON" : "§cOFF"), PrideFrame.BUTTON, mx, my);
        int steps = com.dogpound.pridecarts.carts.Routes.parse(engine.route()).size();
        PrideFrame.button(bx + 2 * (bw + 8), byy, bw, 18, "Route… " + (steps > 0 ? "§d(" + steps + ")" : ""), 0xFF3D2168, mx, my);
        PrideFrame.button(bx + 3 * (bw + 8), byy, bw, 18, "Uncouple", PrideFrame.BUTTON, mx, my);
        PrideFrame.button(bx + 4 * (bw + 8), byy, bw, 18, "Close", PrideFrame.BUTTON, mx, my);
        super.drawScreen(mx, my, pt);
    }

    /** 200700 → "200.7k", 2000000 → "2M" */
    private static String kilo(int v) {
        if (v >= 1_000_000) return trim(v / 1_000_000.0) + "M";
        if (v >= 1_000) return trim(v / 1_000.0) + "k";
        return Integer.toString(v);
    }

    private static String trim(double d) {
        String s = String.format("%.1f", d);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    private static int line(FontRenderer fr, int x, int y, String s) {
        fr.drawStringWithShadow(s, x, y, 0xFFFFFF);
        return y + 11;
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    /** The coupled cars, engine first, walking both couplers (synced entity ids). */
    private List<Entity> train() {
        List<Entity> out = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        List<Entity> todo = new ArrayList<>();
        todo.add(engine);
        while (!todo.isEmpty() && out.size() < 32) {
            Entity e = todo.remove(0);
            if (!seen.add(e.getEntityId())) continue;
            out.add(e);
            if (!(e instanceof IPrideCart)) continue;
            for (int s = 0; s < 2; s++) {
                int id = ((IPrideCart) e).linkedId(s);
                Entity o = id == 0 ? null : engine.world.getEntityByID(id);
                if (o != null && !seen.contains(id)) todo.add(o);
            }
        }
        return out;
    }

    private static boolean rope(Entity a, Entity b) {
        if (!(a instanceof IPrideCart)) return false;
        IPrideCart c = (IPrideCart) a;
        for (int s = 0; s < 2; s++) if (c.linkedId(s) == b.getEntityId()) return c.linkedRope(s);
        return false;
    }

    // ---------------------------------------------------------------- input

    @Override
    protected void mouseClicked(int mx, int my, int button) throws java.io.IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;
        PrideFrame f = frame();
        int cw = colW(f), x1 = f.cx, top = f.cy, bottom = f.y + f.h - 34;
        int ty = top + 20, th = Math.max(14, (bottom - top - 76) / 5);
        for (int n = 0; n <= EntityEngineCart.NOTCHES; n++) {
            int y = ty + (EntityEngineCart.NOTCHES - n) * (th + 2);
            if (in(mx, my, x1 + 6, y, cw - 12, th)) { send(EntityEngineCart.A_NOTCH, n); click(); return; }
        }
        int by = ty + 5 * (th + 2) + 4;
        if (in(mx, my, x1 + 6, by, cw - 12, 16)) { send(EntityEngineCart.A_REVERSE, 0); click(); return; }
        if (in(mx, my, x1 + 6, by + 20, cw - 12, 16)) { send(EntityEngineCart.A_ESTOP, 0); click(); return; }
        int bw = (f.cw - 32) / 5, bx = f.cx, byy = f.y + f.h - 26;
        if (in(mx, my, bx, byy, bw, 18)) { send(EntityEngineCart.A_HORN, 0); return; }
        if (in(mx, my, bx + bw + 8, byy, bw, 18)) { send(EntityEngineCart.A_OBEY, 0); click(); return; }
        if (in(mx, my, bx + 2 * (bw + 8), byy, bw, 18)) { click(); mc.displayGuiScreen(new GuiRoute(engine, this)); return; }
        if (in(mx, my, bx + 3 * (bw + 8), byy, bw, 18)) { send(EntityEngineCart.A_UNCOUPLE, 0); click(); return; }
        if (in(mx, my, bx + 4 * (bw + 8), byy, bw, 18)) mc.displayGuiScreen(null);
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        int d = org.lwjgl.input.Mouse.getEventDWheel();
        if (d != 0) scroll += d > 0 ? -1 : 1;
    }

    @Override
    protected void keyTyped(char c, int key) throws java.io.IOException {
        super.keyTyped(c, key);
        if (c >= '0' && c <= '4') send(EntityEngineCart.A_NOTCH, c - '0');
        else if (c == 'r' || c == 'R') send(EntityEngineCart.A_REVERSE, 0);
        else if (c == 'h' || c == 'H') send(EntityEngineCart.A_HORN, 0);
        else if (c == ' ') send(EntityEngineCart.A_ESTOP, 0);
    }

    private void click() {
        mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.getMasterRecord(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 1f));
    }
}
