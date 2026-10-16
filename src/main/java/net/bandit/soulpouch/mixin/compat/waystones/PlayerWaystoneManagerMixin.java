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


@Mixin(value = PlayerWaystoneManager.class, remap = false)
public abstract class PlayerWaystoneManagerMixin {

    @Inject(method = "predictExperienceLevelCost", at = @At("RETURN"), cancellable = true)
    private static void soulpouch$soulsReplaceXp(Entity player, IWaystone waystone, WarpMode warpMode, IWaystone fromWaystone,
                                                 CallbackInfoReturnable<Integer> cir) {
        if (WaystonesCompat.RAW_XP.get()) return;
        int xp = cir.getReturnValueI();
        if (xp <= 0) return;
        Config.WaystoneMode mode = WaystonesCompat.mode();
        if (mode == Config.WaystoneMode.SOULS_ONLY) {
            cir.setReturnValue(0);
        } else if (mode == Config.WaystoneMode.SOULS_THEN_XP) {
            int souls = WaystonesCompat.soulCost(player, waystone, fromWaystone, xp, false);
            if (WaystonesCompat.pouchHas(player, souls)) cir.setReturnValue(0);
        }
    }
}
