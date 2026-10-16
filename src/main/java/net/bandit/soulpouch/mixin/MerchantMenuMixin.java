package net.bandit.soulpouch.mixin;

import net.bandit.soulpouch.Config;
import net.bandit.soulpouch.pouch.PouchContents;
import net.bandit.soulpouch.pouch.PouchFinder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin extends AbstractContainerMenu {

    @Shadow @Final private Merchant trader;
    @Shadow @Final private MerchantContainer tradeContainer;
    @Shadow public abstract MerchantOffers getOffers();

    protected MerchantMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Unique
    private ServerPlayer soulpouch$player() {
        if (!Config.PAY_VILLAGER_TRADES.get()) return null;
        return trader.getTradingPlayer() instanceof ServerPlayer sp ? sp : null;
    }

    @Unique
    private void soulpouch$stashPayment(Player player) {
        ItemStack pouch = PouchFinder.find(player);
        if (pouch.isEmpty()) return;
        for (int i = 0; i < 2; i++) {
            ItemStack s = tradeContainer.getItem(i);
            if (PouchContents.canStore(s) && PouchContents.insertStack(pouch, s) > 0) {
                tradeContainer.setItem(i, s.isEmpty() ? ItemStack.EMPTY : s);
            }
        }
    }

    @Inject(method = "tryMoveItems", at = @At("HEAD"))
    private void soulpouch$beforeMove(int selected, CallbackInfo ci) {
        ServerPlayer player = soulpouch$player();
        if (player != null) soulpouch$stashPayment(player);
    }

    @Inject(method = "tryMoveItems", at = @At("TAIL"))
    private void soulpouch$afterMove(int selected, CallbackInfo ci) {
        ServerPlayer player = soulpouch$player();
        if (player == null) return;
        MerchantOffers offers = getOffers();
        if (selected < 0 || selected >= offers.size()) return;
        ItemStack pouch = PouchFinder.find(player);
        if (pouch.isEmpty()) return;

        MerchantOffer offer = offers.get(selected);
        ItemStack[] costs = { offer.getCostA(), offer.getCostB() };
        for (int i = 0; i < 2; i++) {
            ItemStack cost = costs[i];
            if (cost.isEmpty() || !PouchContents.canStore(cost)) continue;
            ItemStack inSlot = tradeContainer.getItem(i);
            if (!inSlot.isEmpty() && !ItemStack.isSameItemSameTags(inSlot, cost)) continue;

            int have = inSlot.isEmpty() ? 0 : inSlot.getCount();
            int want = cost.getMaxStackSize() - have; // fill a full stack like vanilla does
            int got = PouchContents.extract(pouch, cost.getItem(), want);
            if (got > 0) tradeContainer.setItem(i, new ItemStack(cost.getItem(), have + got));
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void soulpouch$onClose(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer && player.isAlive() && Config.PAY_VILLAGER_TRADES.get()) {
            soulpouch$stashPayment(player);
        }
    }
}
