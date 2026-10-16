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

    public static final ForgeConfigSpec.BooleanValue FEED_SOUL_ENCHANTER;
    public static final ForgeConfigSpec.BooleanValue PAY_VILLAGER_TRADES;

    public enum WaystoneMode { XP_ONLY, SOULS_THEN_XP, SOULS_ONLY }
    public static final ForgeConfigSpec.EnumValue<WaystoneMode> WAYSTONE_MODE;
    public static final ForgeConfigSpec.ConfigValue<String> WAYSTONE_CURRENCY;
    public static final ForgeConfigSpec.DoubleValue WAYSTONE_SOULS_PER_LEVEL;

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

        b.push("integration");
        FEED_SOUL_ENCHANTER = b.comment("Opening an Eidolon Soul Enchanter fills its shard slot from the pouch, and returns leftovers on close.")
                .define("feedSoulEnchanter", true);
        PAY_VILLAGER_TRADES = b.comment("Selecting a villager trade whose cost is a pouchable item fills the payment slot from the pouch.")
                .define("payVillagerTrades", true);
        b.pop();

        b.push("waystones");
        WAYSTONE_MODE = b.comment(
                        "XP_ONLY: Waystones works normally.",
                        "SOULS_THEN_XP: pay with souls if the pouch has enough, otherwise normal XP cost.",
                        "SOULS_ONLY: travel always costs souls; no pouch / not enough souls = no teleport.")
                .defineEnum("mode", WaystoneMode.SOULS_THEN_XP);
        WAYSTONE_CURRENCY = b.comment("Item used to pay for travel.")
                .define("currencyItem", "eidolon:soul_shard");
        WAYSTONE_SOULS_PER_LEVEL = b.comment("Souls charged per XP level Waystones would have charged (rounded up).")
                .defineInRange("soulsPerXpLevel", 1.0, 0.0, 1000.0);
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
