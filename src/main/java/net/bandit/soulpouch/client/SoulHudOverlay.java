package net.bandit.soulpouch.client;

import net.bandit.soulpouch.Config;
import net.bandit.soulpouch.pouch.PouchContents;
import net.bandit.soulpouch.pouch.PouchFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public final class SoulHudOverlay implements IGuiOverlay {
    public static final SoulHudOverlay INSTANCE = new SoulHudOverlay();

    private static final int ICON = 16, ICON_GAP = 2, ENTRY_GAP = 6, MARGIN = 4;

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator() || !Config.HUD_ENABLED.get()) return;

        ItemStack pouch = PouchFinder.find(player);
        if (pouch.isEmpty()) return;

        List<Map.Entry<Item, Integer>> entries = new ArrayList<>(PouchContents.entries(pouch));
        Item primary = PouchContents.primaryItem();
        if (Config.HUD_PRIMARY_ONLY.get()) {
            int n = PouchContents.get(pouch, primary);
            entries.clear();
            entries.add(Map.entry(primary, n));
        } else if (entries.isEmpty()) {
            entries.add(Map.entry(primary, 0));
        }
        if (Config.HUD_HIDE_WHEN_EMPTY.get() && PouchContents.total(pouch) <= 0) return;

        Font font = mc.font;
        boolean icons = Config.HUD_SHOW_ICON.get();

        int rowWidth = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) rowWidth += ENTRY_GAP;
            if (icons) rowWidth += ICON + ICON_GAP;
            rowWidth += font.width(format(entries.get(i).getValue()));
        }

        int x, y;
        switch (Config.HUD_ANCHOR.get()) {
            case HOTBAR_LEFT -> {
                x = width / 2 - 91 - 6 - rowWidth;
                boolean offhandLeft = !player.getOffhandItem().isEmpty() && player.getMainArm() == HumanoidArm.RIGHT;
                if (offhandLeft) x -= 29;
                y = height - 19;
            }
            case ABOVE_XP_BAR -> {
                x = width / 2 - rowWidth / 2;
                y = height - 53; // just above the XP level number
            }
            case TOP_LEFT -> { x = MARGIN; y = MARGIN; }
            case TOP_RIGHT -> { x = width - rowWidth - MARGIN; y = MARGIN; }
            case BOTTOM_LEFT -> { x = MARGIN; y = height - ICON - MARGIN; }
            case BOTTOM_RIGHT -> { x = width - rowWidth - MARGIN; y = height - ICON - MARGIN; }
            default -> { // HOTBAR_RIGHT
                x = width / 2 + 91 + 6;
                boolean offhandRight = !player.getOffhandItem().isEmpty() && player.getMainArm() == HumanoidArm.LEFT;
                if (offhandRight) x += 29;
                y = height - 19;
            }
        }
        x += Config.HUD_OFFSET_X.get();
        y += Config.HUD_OFFSET_Y.get();

        int color = Config.hudColor();
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<Item, Integer> e = entries.get(i);
            if (i > 0) x += ENTRY_GAP;
            if (icons) {
                g.renderItem(new ItemStack(e.getKey()), x, y);
                x += ICON + ICON_GAP;
            }
            String text = format(e.getValue());
            drawOutlined(g, font, text, x, y + 4, color);
            x += font.width(text);
        }
    }

    private static void drawOutlined(GuiGraphics g, Font font, String s, int x, int y, int color) {
        g.drawString(font, s, x + 1, y, 0x000000, false);
        g.drawString(font, s, x - 1, y, 0x000000, false);
        g.drawString(font, s, x, y + 1, 0x000000, false);
        g.drawString(font, s, x, y - 1, 0x000000, false);
        g.drawString(font, s, x, y, color, false);
    }

    private static String format(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 100_000) return (n / 1000) + "k";
        return Integer.toString(n);
    }
}
