package net.bandit.soulpouch.mixin.compat.waystones;

import net.bandit.soulpouch.Config;
import net.bandit.soulpouch.compat.waystones.WaystonesCompat;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.core.PlayerWaystoneManager;
import net.blay09.mods.waystones.core.WarpMode;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * predictExperienceLevelCost is what Waystones uses for the cost shown in the selection screen and for
 * the inventory-button "can I afford it" check. When souls will pay, report 0 levels so players with
 * no XP aren't blocked and the screen doesn't show an XP price.
 * The real charge goes through WaystoneTeleportEvent (see WaystonesCompat).
 *
 * Only applied when Waystones is present (SoulPouchMixinPlugin).
 */
@Mixin(value = PlayerWaystoneManager.class, remap = false)
public abstract class PlayerWaystoneManagerMixin {

    @Inject(method = "predictExperienceLevelCost", at = @At("RETURN"), cancellable = true)
    private static void soulpouch$soulsReplaceXp(Entity player, IWaystone waystone, WarpMode warpMode, IWaystone fromWaystone,
                                                 CallbackInfoReturnable<Integer> cir) {
        int xp = cir.getReturnValueI();
        if (xp <= 0) return;
        Config.WaystoneMode mode = WaystonesCompat.mode();
        if (mode == Config.WaystoneMode.SOULS_ONLY
                || (mode == Config.WaystoneMode.SOULS_THEN_XP && WaystonesCompat.canPayWithSouls(player, xp))) {
            cir.setReturnValue(0);
        }
    }
}
