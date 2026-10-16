package net.bandit.soulpouch.compat.waystones;

import net.bandit.soulpouch.Config;
import net.bandit.soulpouch.SoulPouch;
import net.bandit.soulpouch.pouch.PouchContents;
import net.bandit.soulpouch.pouch.PouchFinder;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.WaystoneTeleportEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class WaystonesCompat {
    private WaystonesCompat() {}

    private static final Map<UUID, Integer> PENDING = new ConcurrentHashMap<>();

    public static void init() {
        Balm.getEvents().onEvent(WaystoneTeleportEvent.Pre.class, WaystonesCompat::onPre);
        Balm.getEvents().onEvent(WaystoneTeleportEvent.Post.class, WaystonesCompat::onPost);
    }

    public static Config.WaystoneMode mode() {
        try {
            return Config.WAYSTONE_MODE.get();
        } catch (IllegalStateException notLoaded) {
            return Config.WaystoneMode.XP_ONLY;
        }
    }

    public static Item currency() {
        ResourceLocation id = ResourceLocation.tryParse(Config.WAYSTONE_CURRENCY.get());
        Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        return item == null ? net.minecraft.world.item.Items.AIR : item;
    }

    public static int soulCost(int xpLevels) {
        if (xpLevels <= 0) return 0;
        return (int) Math.ceil(xpLevels * Config.WAYSTONE_SOULS_PER_LEVEL.get());
    }

    public static boolean canPayWithSouls(Entity entity, int xpLevels) {
        if (!(entity instanceof Player player)) return false;
        ItemStack pouch = PouchFinder.find(player);
        return !pouch.isEmpty() && PouchContents.get(pouch, currency()) >= soulCost(xpLevels);
    }

    private static void onPre(WaystoneTeleportEvent.Pre event) {
        Config.WaystoneMode mode = mode();
        if (mode == Config.WaystoneMode.XP_ONLY) return;
        if (!(event.getContext().getEntity() instanceof Player player) || player.level().isClientSide) return;

        PENDING.remove(player.getUUID());
        int xp = event.getXpCost();
        if (xp <= 0) return;

        int souls = soulCost(xp);
        if (canPayWithSouls(player, xp)) {
            event.setXpCost(0);
            if (souls > 0) PENDING.put(player.getUUID(), souls);
        } else if (mode == Config.WaystoneMode.SOULS_ONLY) {
            event.setCanceled(true);
            player.displayClientMessage(SoulPouch.tr("msg.waystone_need", souls, currency().getDescription())
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    private static void onPost(WaystoneTeleportEvent.Post event) {
        if (!(event.getContext().getEntity() instanceof Player player) || player.level().isClientSide) return;
        Integer souls = PENDING.remove(player.getUUID());
        if (souls == null || souls <= 0) return;
        ItemStack pouch = PouchFinder.find(player);
        if (pouch.isEmpty()) return;
        int paid = PouchContents.extract(pouch, currency(), souls);
        player.displayClientMessage(SoulPouch.tr("msg.waystone_paid", paid, currency().getDescription())
                .withStyle(ChatFormatting.DARK_AQUA), true);
    }
}
