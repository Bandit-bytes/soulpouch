package net.bandit.soulpouch.mixin.compat.waystones;

import net.bandit.soulpouch.SoulPouch;
import net.bandit.soulpouch.compat.waystones.WaystonesCompat;
import net.bandit.soulpouch.pouch.PouchContents;
import net.bandit.soulpouch.pouch.PouchFinder;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.client.gui.widget.WaystoneButton;
import net.blay09.mods.waystones.core.WarpMode;
import net.blay09.mods.waystones.menu.WaystoneSelectionMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;


@Mixin(WaystoneButton.class)
public abstract class WaystoneButtonMixin {

    @Shadow(remap = false) @Final private IWaystone waystone;

    @Unique private int soulpouch$cost = -1;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void soulpouch$computeCost(int x, int y, IWaystone waystone, int xpLevelCost, Button.OnPress pressable, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        WarpMode mode = WarpMode.WAYSTONE_TO_WAYSTONE;
        IWaystone from = null;
        if (mc.screen instanceof AbstractContainerScreen<?> screen && screen.getMenu() instanceof WaystoneSelectionMenu menu) {
            mode = menu.getWarpMode();
            from = menu.getWaystoneFrom();
        }
        if (from != null && from.getWaystoneUid().equals(waystone.getWaystoneUid())) return;

        try {
            soulpouch$cost = WaystonesCompat.displayedSoulCost(player, waystone, mode, from);
        } catch (IllegalStateException configNotLoaded) {
            soulpouch$cost = -1;
        }

        Button self = (Button) (Object) this;
        if (soulpouch$cost > 0 && !soulpouch$canAfford(player) && !player.getAbilities().instabuild) {
            self.active = false;
        }
    }

    @Unique
    private boolean soulpouch$canAfford(Player player) {
        ItemStack pouch = PouchFinder.find(player);
        return !pouch.isEmpty() && PouchContents.get(pouch, WaystonesCompat.currency()) >= soulpouch$cost;
    }

    @Inject(method = "renderWidget", at = @At("TAIL"))
    private void soulpouch$renderCost(GuiGraphics g, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (soulpouch$cost <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Button self = (Button) (Object) this;
        int x = self.getX(), y = self.getY();
        boolean canAfford = soulpouch$canAfford(player) || player.getAbilities().instabuild;
        Item currency = WaystonesCompat.currency();

        g.renderItem(new ItemStack(currency), x + 2, y + 2);
        String text = Integer.toString(soulpouch$cost);
        g.drawString(mc.font, text, x + 19, y + 6, canAfford ? 0xE388DD : 0xFF5555);

        int iconAreaRight = x + 19 + mc.font.width(text);
        if (self.isHovered() && mouseX <= iconAreaRight) {
            int have = PouchContents.get(PouchFinder.find(player), currency);
            List<Component> tooltip = List.of(
                    SoulPouch.tr("gui.waystone_cost", soulpouch$cost, currency.getDescription())
                            .withStyle(canAfford ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED),
                    SoulPouch.tr("gui.waystone_have", have).withStyle(ChatFormatting.GRAY));
            g.renderTooltip(mc.font, tooltip, Optional.empty(), mouseX, mouseY + mc.font.lineHeight);
        }
    }
}
