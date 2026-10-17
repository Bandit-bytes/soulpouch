package net.bandit.soulpouch.mixin.compat.bonfires;

import net.bandit.soulpouch.SoulPouch;
import net.bandit.soulpouch.compat.bonfires.BonfiresCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
import wehavecookies56.bonfires.bonfire.Bonfire;
import wehavecookies56.bonfires.client.gui.BonfireScreen;
import wehavecookies56.bonfires.tiles.BonfireTileEntity;

import java.util.List;
import java.util.Optional;


@Mixin(BonfireScreen.class)
public abstract class BonfireScreenMixin {

    @Shadow(remap = false) private Button travel;
    @Shadow(remap = false) private boolean travelOpen;
    @Shadow(remap = false) @Final private BonfireTileEntity bonfire;
    @Shadow(remap = false) public Bonfire selectedInstance;

    /** Price of the selected destination, or 0 when there is nothing to show. */
    @Unique
    private int soulpouch$cost() {
        Player player = Minecraft.getInstance().player;
        if (player == null || !travelOpen || selectedInstance == null || travel == null || !travel.visible) return 0;
        if (selectedInstance.getId().equals(bonfire.getID())) return 0; // the bonfire you're at
        return BonfiresCompat.cost(player, selectedInstance.getPos(), selectedInstance.getDimension());
    }

    @Inject(method = "updateButtons", at = @At("TAIL"), remap = false)
    private void soulpouch$lockIfTooExpensive(CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        int cost = soulpouch$cost();
        if (player != null && cost > 0 && !BonfiresCompat.canAfford(player, cost)) {
            travel.active = false;
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void soulpouch$drawCost(GuiGraphics g, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        int cost = soulpouch$cost();
        if (cost <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        boolean canAfford = BonfiresCompat.canAfford(player, cost);
        if (!canAfford) travel.active = false; // keep it locked even if Bonfires re-enabled it this frame

        Item currency = BonfiresCompat.currency();
        int bx = travel.getX(), by = travel.getY(), bw = travel.getWidth();

        g.pose().pushPose();
        g.pose().translate(0, 0, 200); // above the button
        g.renderItem(new ItemStack(currency), bx + 2, by + 2);
        String text = Integer.toString(cost);
        g.drawString(mc.font, text, bx + bw - 4 - mc.font.width(text), by + 6, canAfford ? 0xE388DD : 0xFF5555);
        g.pose().popPose();

        if (mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + travel.getHeight()) {
            List<Component> tooltip = List.of(
                    SoulPouch.tr("gui.waystone_cost", cost, currency.getDescription())
                            .withStyle(canAfford ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED),
                    SoulPouch.tr("gui.waystone_have", BonfiresCompat.have(player)).withStyle(ChatFormatting.GRAY));
            g.renderTooltip(mc.font, tooltip, Optional.empty(), mouseX, mouseY);
        }
    }
}
