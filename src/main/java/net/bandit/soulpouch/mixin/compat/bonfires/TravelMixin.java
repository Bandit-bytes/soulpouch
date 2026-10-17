package net.bandit.soulpouch.mixin.compat.bonfires;

import net.bandit.soulpouch.SoulPouch;
import net.bandit.soulpouch.compat.bonfires.BonfiresCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wehavecookies56.bonfires.packets.server.Travel;

/**
 * Bonfires sends this packet when you press Travel. We check the price before the teleport runs:
 * not enough souls cancels the trip (nothing happens, nothing is taken), otherwise the souls are
 * taken and Bonfires teleports as normal. The /bonfires travel command doesn't use this packet,
 * so admin commands stay free.
 *
 * Only applied when Bonfires is installed (SoulPouchMixinPlugin).
 */
@Mixin(value = Travel.class, remap = false)
public abstract class TravelMixin {

    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int z;
    @Shadow private ResourceKey<Level> dim;

    @Inject(method = "handle(Lnet/minecraftforge/network/NetworkEvent$Context;)V", at = @At("HEAD"), cancellable = true)
    private void soulpouch$payForTravel(NetworkEvent.Context context, CallbackInfo ci) {
        ServerPlayer player = context.getSender();
        if (player == null || !BonfiresCompat.enabled()) return;

        int cost = BonfiresCompat.cost(player, new BlockPos(x, y, z), dim);
        if (cost <= 0) return;

        if (!BonfiresCompat.canAfford(player, cost)) {
            ci.cancel();
            player.displayClientMessage(SoulPouch.tr("msg.waystone_need", cost, BonfiresCompat.currency().getDescription())
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        int paid = BonfiresCompat.charge(player, cost);
        player.displayClientMessage(SoulPouch.tr("msg.waystone_paid", paid, BonfiresCompat.currency().getDescription())
                .withStyle(ChatFormatting.DARK_AQUA), true);
    }
}
