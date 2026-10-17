package net.bandit.soulpouch.compat.bonfires;

import net.bandit.soulpouch.Config;
import net.bandit.soulpouch.pouch.PouchContents;
import net.bandit.soulpouch.pouch.PouchFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;


public final class BonfiresCompat {
    private BonfiresCompat() {}

    public static boolean enabled() {
        try {
            return Config.BONFIRE_ENABLED.get();
        } catch (IllegalStateException notLoaded) {
            return false;
        }
    }

    public static Item currency() {
        ResourceLocation id = ResourceLocation.tryParse(Config.BONFIRE_CURRENCY.get());
        Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        return item == null ? Items.AIR : item;
    }

    public static int cost(Player player, BlockPos dest, ResourceKey<Level> destDim) {
        if (!enabled() || player.getAbilities().instabuild) return 0;

        double cost;
        if (!player.level().dimension().location().equals(destDim.location())) {
            cost = Config.BONFIRE_DIMENSION_COST.get();
        } else {
            cost = Config.BONFIRE_BASE_COST.get();
            int perSoul = Config.BONFIRE_BLOCKS_PER_SOUL.get();
            if (perSoul > 0) {
                double dist = Math.sqrt(player.blockPosition().distSqr(dest));
                cost += Math.floor(dist / perSoul);
            }
            int max = Config.BONFIRE_MAX_COST.get();
            if (max > 0) cost = Math.min(cost, max);
        }
        return (int) Math.ceil(cost);
    }

    public static int have(Player player) {
        ItemStack pouch = PouchFinder.find(player);
        return pouch.isEmpty() ? 0 : PouchContents.get(pouch, currency());
    }

    public static boolean canAfford(Player player, int cost) {
        return cost <= 0 || player.getAbilities().instabuild || have(player) >= cost;
    }

    public static int charge(Player player, int cost) {
        if (cost <= 0) return 0;
        ItemStack pouch = PouchFinder.find(player);
        return pouch.isEmpty() ? 0 : PouchContents.extract(pouch, currency(), cost);
    }
}
