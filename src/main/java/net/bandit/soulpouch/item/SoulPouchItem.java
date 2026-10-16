package net.bandit.soulpouch.item;

import net.bandit.soulpouch.Config;
import net.bandit.soulpouch.SoulPouch;
import net.bandit.soulpouch.pouch.PouchContents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Map;


public class SoulPouchItem extends Item implements ICurioItem {

    public SoulPouchItem(Properties props) {
        super(props);
    }


    @Override
    public ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source, int lootingLevel, boolean recentlyHit, ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return false;
    }


    @Override
    public boolean overrideStackedOnOther(ItemStack pouch, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || pouch.getCount() != 1) return false;
        ItemStack inSlot = slot.getItem();
        if (!PouchContents.canStore(inSlot) || !slot.allowModification(player)) return false;
        int fits = PouchContents.space(pouch, inSlot.getItem());
        if (fits <= 0) return true;
        ItemStack taken = slot.safeTake(inSlot.getCount(), fits, player);
        int moved = PouchContents.insert(pouch, taken.getItem(), taken.getCount());
        if (moved > 0) playInsert(player);
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack pouch, ItemStack carried, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || pouch.getCount() != 1 || !slot.allowModification(player)) return false;
        if (carried.isEmpty()) {
            Item item = PouchContents.firstToWithdraw(pouch);
            if (item == null) return false;
            int n = PouchContents.extract(pouch, item, new ItemStack(item).getMaxStackSize());
            if (n > 0) {
                access.set(new ItemStack(item, n));
                playRemove(player);
            }
            return true;
        }
        if (PouchContents.canStore(carried)) {
            if (PouchContents.insertStack(pouch, carried) > 0) playInsert(player);
            return true;
        }
        return false;
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack pouch = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                int moved = depositInventory(player, pouch);
                if (moved > 0) {
                    playInsert(player);
                    player.displayClientMessage(SoulPouch.tr("msg.deposited", moved), true);
                }
            } else {
                withdraw(player, pouch, false);
            }
        }
        return InteractionResultHolder.sidedSuccess(pouch, level.isClientSide);
    }

    public static int depositInventory(Player player, ItemStack pouch) {
        int moved = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (PouchContents.canStore(s)) moved += PouchContents.insertStack(pouch, s);
        }
        if (moved > 0) inv.setChanged();
        return moved;
    }

    public static void withdraw(Player player, ItemStack pouch, boolean single) {
        Item item = PouchContents.firstToWithdraw(pouch);
        if (item == null) {
            player.displayClientMessage(SoulPouch.tr("msg.empty"), true);
            return;
        }
        int want = single ? 1 : new ItemStack(item).getMaxStackSize();
        int n = PouchContents.extract(pouch, item, want);
        if (n > 0) {
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(item, n));
            playRemove(player);
        }
    }

    private static void playInsert(Player player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS,
                0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playRemove(Player player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS,
                0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
    }


    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        List<Map.Entry<Item, Integer>> entries = PouchContents.entries(stack);
        if (entries.isEmpty()) {
            tooltip.add(SoulPouch.tr("tooltip.empty").withStyle(ChatFormatting.GRAY));
        } else {
            for (Map.Entry<Item, Integer> e : entries) {
                tooltip.add(Component.literal("  ")
                        .append(Component.literal(String.format("%,d", e.getValue())).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" × ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(e.getKey().getDescription().copy().withStyle(ChatFormatting.GRAY)));
            }
            if (entries.size() > 1) {
                tooltip.add(SoulPouch.tr("tooltip.total", String.format("%,d", PouchContents.total(stack))).withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        boolean shift = net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()
                && net.bandit.soulpouch.client.ClientHooks.isShiftDown();
        if (shift) {
            tooltip.add(SoulPouch.tr("tooltip.help1").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(SoulPouch.tr("tooltip.help2").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(SoulPouch.tr("tooltip.help3").withStyle(ChatFormatting.DARK_GRAY));
            try {
                double loss = Config.DEATH_LOSS_PERCENT.get();
                if (loss > 0) tooltip.add(SoulPouch.tr("tooltip.death", trimPercent(loss)).withStyle(ChatFormatting.DARK_RED));
            } catch (IllegalStateException ignored) {
            }
        } else {
            tooltip.add(SoulPouch.tr("tooltip.shift").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static String trimPercent(double d) {
        return d == Math.floor(d) ? String.valueOf((int) d) : String.format("%.1f", d);
    }
}
