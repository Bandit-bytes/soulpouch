package net.bandit.soulpouch.network;

import net.bandit.soulpouch.SoulPouch;
import net.bandit.soulpouch.item.SoulPouchItem;
import net.bandit.soulpouch.pouch.PouchFinder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public final class Network {
    private Network() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            SoulPouch.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        CHANNEL.registerMessage(0, WithdrawPacket.class, WithdrawPacket::encode, WithdrawPacket::decode,
                WithdrawPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public record WithdrawPacket(boolean single) {
        public static void encode(WithdrawPacket p, FriendlyByteBuf buf) { buf.writeBoolean(p.single); }

        public static WithdrawPacket decode(FriendlyByteBuf buf) { return new WithdrawPacket(buf.readBoolean()); }

        public static void handle(WithdrawPacket p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null || player.isSpectator()) return;
                ItemStack pouch = PouchFinder.find(player);
                if (pouch.isEmpty()) {
                    player.displayClientMessage(SoulPouch.tr("msg.not_worn"), true);
                    return;
                }
                SoulPouchItem.withdraw(player, pouch, p.single);
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
