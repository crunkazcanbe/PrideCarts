package com.dogpound.pridecarts.client;

import com.dogpound.pridecarts.carts.EntityEngineCart;
import com.dogpound.pridecarts.carts.Routes;
import com.dogpound.pridecarts.net.PcNet;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.List;

/**
 * The engine's route, all buttons: each row is "at station [N] → [action] [value]". Station 0 = every station.
 * Every click saves straight to the engine. Number the Station Tracks by right-clicking them with an empty hand.
 */
@SideOnly(Side.CLIENT)
public class GuiRoute extends GuiScreen {
    private static final String[] HELP = {
            "wait: stop this many seconds", "skip: roll straight through", "reverse: leave the way you came",
            "speed: leave at this notch (0-4)", "horn: sound off", "unload: storage cars → chests by the station",
            "load: chests by the station → storage cars", "hold: wait for a redstone signal"};
    private static final int[] DEF = {10, 0, 0, 2, 0, 0, 0, 0};
    private static final int[] COLOR = {0xFF5BCEFA, 0xFF8A8499, 0xFFF2C94C, 0xFF4AD06A, 0xFFB48CFF, 0xFFFF8A2A, 0xFFFF7AD8, 0xFFE8434F};

    private final EntityEngineCart engine;
    private final GuiScreen parent;
    private List<Routes.Step> steps;
    private String tip;

    public GuiRoute(EntityEngineCart engine, GuiScreen parent) {
        this.engine = engine;
        this.parent = parent;
        steps = Routes.parse(engine.route());
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    private PrideFrame frame() { return PrideFrame.sized(width, height, 460, 320); }

    private int rowY(PrideFrame f, int i) { return f.cy + 26 + i * 15; }

    private void save() { PcNet.CH.sendToServer(new PcNet.RouteSet(engine.getEntityId(), Routes.join(steps))); }

    private static int actionIndex(String a) {
        for (int i = 0; i < Routes.ACTIONS.length; i++) if (Routes.ACTIONS[i].equals(a)) return i;
        return 0;
    }

    private static boolean hasValue(String a) { return a.equals("wait") || a.equals("speed"); }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        PrideFrame f = frame();
        f.draw(this, "Route", "§7" + steps.size() + " / " + Routes.MAX_STEPS + " steps");
        fontRenderer.drawStringWithShadow("§7Number your Station Tracks (right-click, empty hand). Station 0 = every station.", f.cx, f.cy + 2, 0xFFFFFF);
        fontRenderer.drawStringWithShadow("§8Station", f.cx + 4, f.cy + 14, 0xFFFFFF);
        fontRenderer.drawStringWithShadow("§8Do", f.cx + 96, f.cy + 14, 0xFFFFFF);
        fontRenderer.drawStringWithShadow("§8Value", f.cx + 200, f.cy + 14, 0xFFFFFF);
        int maxRows = Math.max(1, (f.y + f.h - 34 - (f.cy + 26)) / 15);
        for (int i = 0; i < steps.size() && i < maxRows; i++) {
            Routes.Step s = steps.get(i);
            int y = rowY(f, i), ai = actionIndex(s.action);
            Gui.drawRect(f.cx, y - 1, f.cx + f.cw, y + 13, i % 2 == 0 ? 0x30FFFFFF : 0x18FFFFFF);
            PrideFrame.button(f.cx + 2, y, 14, 12, "-", PrideFrame.BUTTON, mx, my);
            String st = s.station == 0 ? "any" : "#" + s.station;
            fontRenderer.drawStringWithShadow(st, f.cx + 20 + (40 - fontRenderer.getStringWidth(st)) / 2f, y + 2, 0xF5A9B8);
            PrideFrame.button(f.cx + 62, y, 14, 12, "+", PrideFrame.BUTTON, mx, my);
            if (PrideFrame.button(f.cx + 92, y, 92, 12, s.action, COLOR[ai] & 0x00FFFFFF | 0x90000000, mx, my)) tip = HELP[ai] + "  (click: next, right-click: back)";
            if (hasValue(s.action)) {
                PrideFrame.button(f.cx + 196, y, 14, 12, "-", PrideFrame.BUTTON, mx, my);
                String v = s.action.equals("wait") ? s.value + " s" : "notch " + s.value;
                fontRenderer.drawStringWithShadow(v, f.cx + 214 + (50 - fontRenderer.getStringWidth(v)) / 2f, y + 2, 0xFFFFFF);
                PrideFrame.button(f.cx + 268, y, 14, 12, "+", PrideFrame.BUTTON, mx, my);
            }
            PrideFrame.button(f.cx + f.cw - 16, y, 14, 12, "✕", 0xFF8A1E2A, mx, my);
        }
        if (steps.isEmpty()) fontRenderer.drawStringWithShadow("§8No steps yet: the engine stops 5 s at every station. Add one below.", f.cx + 4, f.cy + 30, 0xFFFFFF);
        int by = f.y + f.h - 26;
        PrideFrame.button(f.cx, by, 100, 18, "+ Add step", 0xFF3D2168, mx, my);
        PrideFrame.button(f.cx + 106, by, 120, 18, "Example route", PrideFrame.BUTTON, mx, my);
        PrideFrame.button(f.cx + 232, by, 70, 18, "Clear", PrideFrame.BUTTON, mx, my);
        PrideFrame.button(f.cx + f.cw - 70, by, 70, 18, "Done", PrideFrame.BUTTON, mx, my);
        if (tip != null) drawHoveringText(tip, mx, my);
        tip = null;
        super.drawScreen(mx, my, pt);
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) { return mx >= x && my >= y && mx < x + w && my < y + h; }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        PrideFrame f = frame();
        int maxRows = Math.max(1, (f.y + f.h - 34 - (f.cy + 26)) / 15);
        for (int i = 0; i < steps.size() && i < maxRows; i++) {
            Routes.Step s = steps.get(i);
            int y = rowY(f, i);
            if (in(mx, my, f.cx + 2, y, 14, 12)) { s.station = Math.max(0, s.station - 1); save(); return; }
            if (in(mx, my, f.cx + 62, y, 14, 12)) { s.station = Math.min(99, s.station + 1); save(); return; }
            if (in(mx, my, f.cx + 92, y, 92, 12)) {
                int a = (actionIndex(s.action) + (button == 1 ? Routes.ACTIONS.length - 1 : 1)) % Routes.ACTIONS.length;
                s.action = Routes.ACTIONS[a];
                s.value = DEF[a];
                save();
                return;
            }
            if (hasValue(s.action)) {
                int step = s.action.equals("wait") ? (GuiScreen.isShiftKeyDown() ? 10 : 1) : 1, max = s.action.equals("wait") ? 3600 : EntityEngineCart.NOTCHES;
                if (in(mx, my, f.cx + 196, y, 14, 12)) { s.value = Math.max(s.action.equals("wait") ? 1 : 0, s.value - step); save(); return; }
                if (in(mx, my, f.cx + 268, y, 14, 12)) { s.value = Math.min(max, s.value + step); save(); return; }
            }
            if (in(mx, my, f.cx + f.cw - 16, y, 14, 12)) { steps.remove(i); save(); return; }
        }
        int by = f.y + f.h - 26;
        if (in(mx, my, f.cx, by, 100, 18) && steps.size() < Routes.MAX_STEPS) {
            int next = steps.isEmpty() ? 1 : Math.min(99, steps.get(steps.size() - 1).station + 1);
            steps.add(new Routes.Step(next, "wait", 10));
            save();
            return;
        }
        if (in(mx, my, f.cx + 106, by, 120, 18)) {      // a little shuttle: load at 1, unload at 2, turn round at both
            steps = Routes.parse("1:load:0;1:wait:8;1:reverse:0;1:horn:0;2:unload:0;2:wait:8;2:reverse:0;2:horn:0;0:speed:3");
            save();
            return;
        }
        if (in(mx, my, f.cx + 232, by, 70, 18)) { steps.clear(); save(); return; }
        if (in(mx, my, f.cx + f.cw - 70, by, 70, 18)) mc.displayGuiScreen(parent);
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == 1) { mc.displayGuiScreen(parent); return; }
        super.keyTyped(c, key);
    }
}
