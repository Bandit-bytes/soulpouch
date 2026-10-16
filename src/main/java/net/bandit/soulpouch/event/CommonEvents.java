package net.bandit.soulpouch.event;

import net.bandit.soulpouch.Config;
import net.bandit.soulpouch.SoulPouch;
import net.bandit.soulpouch.pouch.PouchContents;
import net.bandit.soulpouch.pouch.PouchFinder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Map;

public final class CommonEvents {
    private CommonEvents() {}


    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || event.isCanceled() || !Config.AUTO_PICKUP.get()) return;

        ItemEntity entity = event.getItem();
        ItemStack stack = entity.getItem();
        if (!PouchContents.canStore(stack)) return;

        ItemStack pouch = PouchFinder.find(player);
        if (pouch.isEmpty()) return;

        int count = stack.getCount();
        int moved = PouchContents.insert(pouch, stack.getItem(), count);
        if (moved <= 0) return;

        if (moved >= count) {
            stack.setCount(0);
            event.setResult(Event.Result.ALLOW);
        } else {
            stack.shrink(moved);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isSpectator()) return;

        double percent = Config.DEATH_LOSS_PERCENT.get();
        if (percent <= 0) return;
        boolean keepInv = player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        if (keepInv && !Config.DEATH_LOSS_WITH_KEEP_INVENTORY.get()) return;

        ItemStack pouch = PouchFinder.find(player);
        if (pouch.isEmpty()) return;

        RandomSource random = player.getRandom();
        boolean drop = Config.DEATH_DROP_LOST.get();
        int totalLost = 0;

        List<Map.Entry<Item, Integer>> entries = PouchContents.entries(pouch);
        for (Map.Entry<Item, Integer> e : entries) {
            double exact = e.getValue() * (percent / 100.0);
            int lose = (int) Math.floor(exact);
            if (random.nextDouble() < exact - lose) lose++; // fair rounding for small amounts
            lose = PouchContents.extract(pouch, e.getKey(), lose);
            totalLost += lose;

            if (drop && lose > 0) {
                int max = new ItemStack(e.getKey()).getMaxStackSize();
                while (lose > 0) {
                    int n = Math.min(max, lose);
                    lose -= n;
                    ItemEntity ie = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), new ItemStack(e.getKey(), n));
                    ie.setDefaultPickUpDelay();
                    player.level().addFreshEntity(ie);
                }
            }
        }

        if (totalLost > 0) {
            player.sendSystemMessage(SoulPouch.tr(drop ? "msg.death_dropped" : "msg.death_lost", totalLost)
                    .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        }
    }
}
