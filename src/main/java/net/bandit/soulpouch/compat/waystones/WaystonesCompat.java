package net.bandit.soulpouch.compat.waystones;

import net.bandit.soulpouch.Config;
import net.bandit.soulpouch.SoulPouch;
import net.bandit.soulpouch.pouch.PouchContents;
import net.bandit.soulpouch.pouch.PouchFinder;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.IWaystoneTeleportContext;
import net.blay09.mods.waystones.api.WaystoneTeleportEvent;
import net.blay09.mods.waystones.core.PlayerWaystoneManager;
import net.blay09.mods.waystones.core.WarpMode;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replaces Waystones' XP-level travel cost with soul items from the pouch.
 *
 * Price: by default our own distance-based soul price (see the [waystones] server config), because
 * Waystones 1.20.1 ships with every XP cost multiplier at 0 = all travel is free, so converting its
 * XP cost would always give 0 souls. Set useWaystonesXpPrice = true to convert its XP cost instead.
 *
 * Flow: Pre event -> work out the price, check the pouch, zero the XP cost and remember the price.
 * Waystones then runs its own checks and teleports -> Post event -> take the souls.
 * Charging in Post means a teleport that gets rejected later never costs anything.
 *
 * Only loaded when Waystones is installed (see SoulPouch constructor).
 */
public final class WaystonesCompat {
    private WaystonesCompat() {}

    private static final Map<UUID, Integer> PENDING = new ConcurrentHashMap<>();

    /** While true, PlayerWaystoneManagerMixin leaves Waystones' XP price untouched (used to read the real price). */
    public static final ThreadLocal<Boolean> RAW_XP = ThreadLocal.withInitial(() -> false);

    /** Waystones' own XP price, without our "souls replace XP" change. */
    public static int rawXpCost(Entity player, IWaystone target, WarpMode mode, @Nullable IWaystone from) {
        RAW_XP.set(true);
        try {
            return PlayerWaystoneManager.predictExperienceLevelCost(player, target, mode, from);
        } finally {
            RAW_XP.set(false);
        }
    }

    public static boolean isWarpItemMode(WarpMode mode) {
        return mode == WarpMode.WARP_SCROLL || mode == WarpMode.RETURN_SCROLL
                || mode == WarpMode.BOUND_SCROLL || mode == WarpMode.WARP_STONE;
    }

    /** What the selection screen should show: the soul price, or -1 if souls won't be used for this trip. */
    public static int displayedSoulCost(Entity player, IWaystone target, WarpMode mode, @Nullable IWaystone from) {
        Config.WaystoneMode m = mode();
        if (m == Config.WaystoneMode.XP_ONLY) return -1;
        int xp = Config.WAYSTONE_USE_XP_PRICE.get() ? rawXpCost(player, target, mode, from) : 0;
        int souls = soulCost(player, target, from, xp, isWarpItemMode(mode));
        if (souls <= 0) return -1;
        if (m == Config.WaystoneMode.SOULS_THEN_XP && !pouchHas(player, souls)) return -1; // XP will be charged instead
        return souls;
    }

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

    /**
     * Soul price of a trip. 0 = free.
     *
     * @param xpCost what Waystones itself would charge in XP levels (only used with useWaystonesXpPrice)
     */
    public static int soulCost(Entity entity, IWaystone target, @Nullable IWaystone from, int xpCost, boolean usingWarpItem) {
        if (!(entity instanceof Player player) || player.getAbilities().instabuild) return 0;
        if (target == null) return 0;
        if (from != null && from.getWaystoneUid().equals(target.getWaystoneUid())) return 0;
        if (usingWarpItem && Config.WAYSTONE_FREE_WITH_WARP_ITEMS.get()) return 0;

        if (Config.WAYSTONE_USE_XP_PRICE.get()) {
            return xpCost <= 0 ? 0 : (int) Math.ceil(xpCost * Config.WAYSTONE_SOULS_PER_LEVEL.get());
        }

        double cost;
        if (!entity.level().dimension().equals(target.getDimension())) {
            cost = Config.WAYSTONE_DIMENSION_COST.get();
        } else {
            cost = Config.WAYSTONE_BASE_COST.get();
            int perSoul = Config.WAYSTONE_BLOCKS_PER_SOUL.get();
            if (perSoul > 0) {
                double dist = Math.sqrt(entity.blockPosition().distSqr(target.getPos()));
                cost += Math.floor(dist / perSoul);
            }
            int max = Config.WAYSTONE_MAX_COST.get();
            if (max > 0) cost = Math.min(cost, max);
        }
        if (target.isGlobal()) cost *= Config.WAYSTONE_GLOBAL_MULTIPLIER.get();
        return (int) Math.ceil(cost);
    }

    /** True if the worn pouch holds at least {@code souls} of the currency. Works client side too. */
    public static boolean pouchHas(Entity entity, int souls) {
        if (souls <= 0) return true;
        if (!(entity instanceof Player player)) return false;
        ItemStack pouch = PouchFinder.find(player);
        return !pouch.isEmpty() && PouchContents.get(pouch, currency()) >= souls;
    }

    private static void onPre(WaystoneTeleportEvent.Pre event) {
        Config.WaystoneMode mode = mode();
        if (mode == Config.WaystoneMode.XP_ONLY) return;
        IWaystoneTeleportContext ctx = event.getContext();
        if (!(ctx.getEntity() instanceof Player player) || player.level().isClientSide) return;

        PENDING.remove(player.getUUID());
        int souls = soulCost(player, ctx.getTargetWaystone(), ctx.getFromWaystone(), event.getXpCost(),
                !ctx.getWarpItem().isEmpty());

        if (souls <= 0) {
            if (mode == Config.WaystoneMode.SOULS_ONLY) event.setXpCost(0);
            return;
        }

        if (pouchHas(player, souls)) {
            event.setXpCost(0);
            PENDING.put(player.getUUID(), souls);
        } else if (mode == Config.WaystoneMode.SOULS_ONLY) {
            event.setCanceled(true);
            player.displayClientMessage(SoulPouch.tr("msg.waystone_need", souls, currency().getDescription())
                    .withStyle(ChatFormatting.RED), true);
        }
        // SOULS_THEN_XP without enough souls: Waystones charges its normal XP cost.
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
