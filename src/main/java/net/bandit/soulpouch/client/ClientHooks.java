package net.bandit.soulpouch.client;

import net.minecraft.client.gui.screens.Screen;

public final class ClientHooks {
    private ClientHooks() {}

    public static boolean isShiftDown() {
        return Screen.hasShiftDown();
    }
}
