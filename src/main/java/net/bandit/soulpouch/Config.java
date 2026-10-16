package net.bandit.soulpouch;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    private Config() {}

    public static final ForgeConfigSpec SERVER_SPEC;

    public static final ForgeConfigSpec.BooleanValue AUTO_PICKUP;
    public static final ForgeConfigSpec.IntValue CAPACITY_PER_TYPE;
    public static final ForgeConfigSpec.ConfigValue<String> PRIMARY_ITEM;

    public static final ForgeConfigSpec.DoubleValue DEATH_LOSS_PERCENT;
    public static final ForgeConfigSpec.BooleanValue DEATH_LOSS_WITH_KEEP_INVENTORY;
    public static final ForgeConfigSpec.BooleanValue DEATH_DROP_LOST;

    public static final ForgeConfigSpec.BooleanValue MOB_DROPS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue MOB_DROPS_REQUIRE_PLAYER;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends Integer>> MOB_DROPS_WEIGHTS;
    public static final ForgeConfigSpec.IntValue MOB_DROPS_LOOTING;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> MOB_DROPS_BLACKLIST;

    public static final ForgeConfigSpec.BooleanValue FEED_SOUL_ENCHANTER;
    public static final ForgeConfigSpec.BooleanValue PAY_VILLAGER_TRADES;

    public enum WaystoneMode { XP_ONLY, SOULS_THEN_XP, SOULS_ONLY }
    public static final ForgeConfigSpec.EnumValue<WaystoneMode> WAYSTONE_MODE;
    public static final ForgeConfigSpec.ConfigValue<String> WAYSTONE_CURRENCY;
    public static final ForgeConfigSpec.BooleanValue WAYSTONE_USE_XP_PRICE;
    public static final ForgeConfigSpec.DoubleValue WAYSTONE_SOULS_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue WAYSTONE_BASE_COST;
    public static final ForgeConfigSpec.IntValue WAYSTONE_BLOCKS_PER_SOUL;
    public static final ForgeConfigSpec.IntValue WAYSTONE_MAX_COST;
    public static final ForgeConfigSpec.IntValue WAYSTONE_DIMENSION_COST;
    public static final ForgeConfigSpec.DoubleValue WAYSTONE_GLOBAL_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue WAYSTONE_FREE_WITH_WARP_ITEMS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("pouch");
        AUTO_PICKUP = b.comment("Soul items picked up from the ground go straight into an equipped pouch.")
                .define("autoPickup", true);
        CAPACITY_PER_TYPE = b.comment("Max amount of each item type the pouch holds. 0 = unlimited.")
                .defineInRange("capacityPerType", 0, 0, Integer.MAX_VALUE);
        PRIMARY_ITEM = b.comment("Item taken out first when withdrawing, and shown first on the HUD.")
                .define("primaryItem", "eidolon:soul_shard");
        b.pop();

        b.push("death");
        DEATH_LOSS_PERCENT = b.comment("Percent of each stored item type lost when you die (the pouch itself is always kept).")
                .defineInRange("lossPercent", 25.0, 0.0, 100.0);
        DEATH_LOSS_WITH_KEEP_INVENTORY = b.comment("Still apply the loss when the keepInventory gamerule is on.")
                .define("lossWithKeepInventory", false);
        DEATH_DROP_LOST = b.comment("true = lost items drop on the ground where you died; false = they are destroyed.")
                .define("dropLostItems", false);
        b.pop();

        b.push("mobDrops");
        MOB_DROPS_ENABLED = b.comment("Every mob has a chance to drop soul shards when killed.")
                .define("enabled", true);
        MOB_DROPS_REQUIRE_PLAYER = b.comment("Only drop when a player (or their tamed pet / projectile) made the kill.",
                        "Stops mob farms with no player involvement from generating shards.")
                .define("requirePlayerKill", true);
        MOB_DROPS_WEIGHTS = b.comment(
                        "Relative weights for dropping 0, 1, 2 and 3 shards. Default 50/40/9/1 means",
                        "0 = 50%, 1 = 40%, 2 = 9%, 3 = 1%  (about 0.61 shards per kill on average).")
                .defineList("weights", java.util.List.of(50, 40, 9, 1),
                        o -> o instanceof Integer i && i >= 0);
        MOB_DROPS_LOOTING = b.comment("Each Looting level adds this many extra weight points to the 1/2/3 results.")
                .defineInRange("lootingBonusWeight", 2, 0, 1000);
        MOB_DROPS_BLACKLIST = b.comment("Entity ids that never drop shards, e.g. \"minecraft:silverfish\".",
                        "Use \"modid:*\" to block a whole mod.")
                .defineList("blacklist", java.util.List.of("minecraft:armor_stand"), o -> o instanceof String);
        b.pop();

        b.push("integration");
        FEED_SOUL_ENCHANTER = b.comment("Opening an Eidolon Soul Enchanter fills its shard slot from the pouch, and returns leftovers on close.")
                .define("feedSoulEnchanter", true);
        PAY_VILLAGER_TRADES = b.comment("Selecting a villager trade whose cost is a pouchable item fills the payment slot from the pouch.")
                .define("payVillagerTrades", true);
        b.pop();

        b.push("waystones");
        WAYSTONE_MODE = b.comment(
                        "XP_ONLY: Waystones works normally, no souls involved.",
                        "SOULS_ONLY: travel costs souls instead of XP; not enough souls = no teleport.",
                        "SOULS_THEN_XP: pay with souls if the pouch has enough, otherwise Waystones' normal XP cost",
                        "  (note: Waystones' own XP costs default to 0, so this falls back to free travel unless you raise them).")
                .defineEnum("mode", WaystoneMode.SOULS_ONLY);
        WAYSTONE_CURRENCY = b.comment("Item used to pay for travel.")
                .define("currencyItem", "eidolon:soul_shard");
        WAYSTONE_USE_XP_PRICE = b.comment(
                        "true  = price is Waystones' own XP cost x soulsPerXpLevel (set XP costs in waystones-common.toml).",
                        "false = use the soul price below (works no matter how Waystones' XP costs are configured).")
                .define("useWaystonesXpPrice", false);
        WAYSTONE_SOULS_PER_LEVEL = b.comment("Only with useWaystonesXpPrice = true: souls per XP level, rounded up.")
                .defineInRange("soulsPerXpLevel", 1.0, 0.0, 1000.0);
        WAYSTONE_BASE_COST = b.comment("Souls charged for every trip.")
                .defineInRange("baseCost", 1, 0, 100000);
        WAYSTONE_BLOCKS_PER_SOUL = b.comment("One extra soul per this many blocks travelled. 0 = distance doesn't matter.")
                .defineInRange("blocksPerSoul", 500, 0, 1000000);
        WAYSTONE_MAX_COST = b.comment("Cap for same-dimension trips. 0 = no cap.")
                .defineInRange("maxCost", 5, 0, 100000);
        WAYSTONE_DIMENSION_COST = b.comment("Flat price for travel to another dimension.")
                .defineInRange("dimensionalCost", 5, 0, 100000);
        WAYSTONE_GLOBAL_MULTIPLIER = b.comment("Price multiplier when the destination is a global waystone.")
                .defineInRange("globalWaystoneMultiplier", 1.0, 0.0, 100.0);
        WAYSTONE_FREE_WITH_WARP_ITEMS = b.comment("Warp scrolls / return scrolls / warp stones travel without a soul cost.")
                .define("freeWithWarpItems", false);
        b.pop();

        SERVER_SPEC = b.build();
    }

    public static final ForgeConfigSpec CLIENT_SPEC;

    public enum HudAnchor { HOTBAR_RIGHT, HOTBAR_LEFT, ABOVE_XP_BAR, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
    public static final ForgeConfigSpec.BooleanValue HUD_ENABLED;
    public static final ForgeConfigSpec.EnumValue<HudAnchor> HUD_ANCHOR;
    public static final ForgeConfigSpec.IntValue HUD_OFFSET_X;
    public static final ForgeConfigSpec.IntValue HUD_OFFSET_Y;
    public static final ForgeConfigSpec.ConfigValue<String> HUD_COLOR;
    public static final ForgeConfigSpec.BooleanValue HUD_SHOW_ICON;
    public static final ForgeConfigSpec.BooleanValue HUD_HIDE_WHEN_EMPTY;
    public static final ForgeConfigSpec.BooleanValue HUD_PRIMARY_ONLY;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("hud");
        HUD_ENABLED = b.define("enabled", true);
        HUD_ANCHOR = b.comment("Where the soul counter sits on screen.").defineEnum("anchor", HudAnchor.HOTBAR_RIGHT);
        HUD_OFFSET_X = b.comment("Pixels to nudge right (negative = left).").defineInRange("offsetX", 0, -4000, 4000);
        HUD_OFFSET_Y = b.comment("Pixels to nudge down (negative = up).").defineInRange("offsetY", 0, -4000, 4000);
        HUD_COLOR = b.comment("Counter text color, hex RGB like #7FE8FF (XP bar green is #80FF20).").define("color", "#7FE8FF");
        HUD_SHOW_ICON = b.comment("Draw the item icon next to the number.").define("showIcon", true);
        HUD_HIDE_WHEN_EMPTY = b.define("hideWhenEmpty", false);
        HUD_PRIMARY_ONLY = b.comment("Only show the primary item instead of every stored type.").define("primaryOnly", false);
        b.pop();
        CLIENT_SPEC = b.build();
    }

    public static int hudColor() {
        try {
            String s = HUD_COLOR.get().trim();
            if (s.startsWith("#")) s = s.substring(1);
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (Exception e) {
            return 0x7FE8FF;
        }
    }
}
