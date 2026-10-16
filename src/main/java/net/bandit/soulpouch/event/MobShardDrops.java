package net.bandit.soulpouch.event;

import net.bandit.soulpouch.Config;
import elucent.eidolon.registries.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;


public final class MobShardDrops {
    private MobShardDrops() {}

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof Mob)) return;
        if (!Config.MOB_DROPS_ENABLED.get()) return;
        // isRecentlyHit = a player (or something credited to one, like a tamed wolf) hit it shortly before death.
        if (Config.MOB_DROPS_REQUIRE_PLAYER.get() && !event.isRecentlyHit()) return;
        if (isBlacklisted(entity)) return;

        int count = roll(entity.getRandom(), event.getLootingLevel());
        if (count <= 0) return;

        ItemEntity drop = new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.25, entity.getZ(),
                new ItemStack(Registry.SOUL_SHARD.get(), count));
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }

    static int roll(RandomSource random, int looting) {
        List<? extends Integer> cfg = Config.MOB_DROPS_WEIGHTS.get();
        int[] w = new int[4];
        for (int i = 0; i < 4; i++) w[i] = i < cfg.size() ? Math.max(0, cfg.get(i)) : 0;
        int bonus = Math.max(0, looting) * Config.MOB_DROPS_LOOTING.get();
        for (int i = 1; i < 4; i++) if (w[i] > 0) w[i] += bonus;

        int total = w[0] + w[1] + w[2] + w[3];
        if (total <= 0) return 0;
        int r = random.nextInt(total);
        for (int i = 0; i < 4; i++) {
            if (r < w[i]) return i;
            r -= w[i];
        }
        return 0;
    }

    private static boolean isBlacklisted(LivingEntity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) return false;
        String full = id.toString(), modWildcard = id.getNamespace() + ":*";
        for (String s : Config.MOB_DROPS_BLACKLIST.get()) {
            if (s.equals(full) || s.equals(modWildcard)) return true;
        }
        return false;
    }
}
