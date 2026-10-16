package net.bandit.soulpouch.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.bandit.soulpouch.SoulPouch;
import net.bandit.soulpouch.network.Network;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class ClientSetup {
    private ClientSetup() {}

    public static final KeyMapping WITHDRAW_KEY = new KeyMapping(
            "key.soulpouch.withdraw", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.soulpouch");

    @Mod.EventBusSubscriber(modid = SoulPouch.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(WITHDRAW_KEY);
        }

        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "soul_counter", SoulHudOverlay.INSTANCE);
        }
    }

    @Mod.EventBusSubscriber(modid = SoulPouch.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            while (WITHDRAW_KEY.consumeClick()) {
                Network.CHANNEL.sendToServer(new Network.WithdrawPacket(mc.player.isShiftKeyDown()));
            }
        }
    }
}
