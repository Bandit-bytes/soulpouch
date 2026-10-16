package net.bandit.soulpouch.pouch;

import net.bandit.soulpouch.item.SoulPouchItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

public final class PouchFinder {
    private PouchFinder() {}

    public static ItemStack find(LivingEntity entity) {
        if (entity == null) return ItemStack.EMPTY;
        return CuriosApi.getCuriosInventory(entity).resolve()
                .flatMap(handler -> handler.findFirstCurio(s -> s.getItem() instanceof SoulPouchItem))
                .map(SlotResult::stack)
                .orElse(ItemStack.EMPTY);
    }
}
