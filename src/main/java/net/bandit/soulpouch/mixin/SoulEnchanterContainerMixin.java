package net.bandit.soulpouch.mixin;

import net.bandit.soulpouch.Config;
import net.bandit.soulpouch.pouch.PouchContents;
import net.bandit.soulpouch.pouch.PouchFinder;
import elucent.eidolon.gui.SoulEnchanterContainer;
import elucent.eidolon.registries.Registry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(SoulEnchanterContainer.class)
public abstract class SoulEnchanterContainerMixin extends AbstractContainerMenu {

    @Shadow(remap = false) @Final private Container tableInventory;

    @Unique private Player soulpouch$player;

    protected SoulEnchanterContainerMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Unique
    private static boolean soulpouch$enabled() {
        return Config.FEED_SOUL_ENCHANTER.get();
    }

    @Unique
    private void soulpouch$refill() {
        if (!(soulpouch$player instanceof ServerPlayer player) || !soulpouch$enabled()) return;
        ItemStack pouch = PouchFinder.find(player);
        if (pouch.isEmpty()) return;

        Item shard = Registry.SOUL_SHARD.get();
        ItemStack slot = tableInventory.getItem(1);
        if (!slot.isEmpty() && !slot.is(shard)) return;

        int have = slot.isEmpty() ? 0 : slot.getCount();
        int want = shard.getMaxStackSize(new ItemStack(shard)) - have;
        int got = PouchContents.extract(pouch, shard, want);
        if (got > 0) tableInventory.setItem(1, new ItemStack(shard, have + got));
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("TAIL"))
    private void soulpouch$onOpen(int id, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        this.soulpouch$player = inventory.player;
        soulpouch$refill();
    }

    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void soulpouch$afterEnchant(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) soulpouch$refill();
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void soulpouch$onClose(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer) || !player.isAlive() || !soulpouch$enabled()) return;
        ItemStack slot = tableInventory.getItem(1);
        if (slot.isEmpty()) return;
        ItemStack pouch = PouchFinder.find(player);
        if (pouch.isEmpty()) return;
        PouchContents.insertStack(pouch, slot);
        if (slot.isEmpty()) tableInventory.setItem(1, ItemStack.EMPTY);
    }
}
