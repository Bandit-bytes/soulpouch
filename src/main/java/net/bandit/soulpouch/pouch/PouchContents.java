package net.bandit.soulpouch.pouch;

import net.bandit.soulpouch.Config;
import net.bandit.soulpouch.SoulPouch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class PouchContents {
    private PouchContents() {}

    private static final String KEY = "SoulPouch";

    public static boolean canStore(ItemStack stack) {
        return !stack.isEmpty() && stack.is(SoulPouch.POUCHABLE) && !stack.hasTag();
    }

    public static boolean canStore(Item item) {
        return new ItemStack(item).is(SoulPouch.POUCHABLE);
    }

    private static CompoundTag data(ItemStack pouch, boolean create) {
        CompoundTag root = create ? pouch.getOrCreateTag() : pouch.getTag();
        if (root == null) return null;
        if (!root.contains(KEY, Tag.TAG_COMPOUND)) {
            if (!create) return null;
            root.put(KEY, new CompoundTag());
        }
        return root.getCompound(KEY);
    }

    private static String key(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id == null ? "minecraft:air" : id.toString();
    }

    public static int get(ItemStack pouch, Item item) {
        CompoundTag d = data(pouch, false);
        return d == null ? 0 : d.getInt(key(item));
    }

    public static int total(ItemStack pouch) {
        CompoundTag d = data(pouch, false);
        if (d == null) return 0;
        long sum = 0;
        for (String k : d.getAllKeys()) sum += d.getInt(k);
        return (int) Math.min(Integer.MAX_VALUE, sum);
    }

    public static List<Map.Entry<Item, Integer>> entries(ItemStack pouch) {
        List<Map.Entry<Item, Integer>> out = new ArrayList<>();
        CompoundTag d = data(pouch, false);
        if (d == null) return out;
        Map<Item, Integer> map = new LinkedHashMap<>();
        for (String k : d.getAllKeys()) {
            int n = d.getInt(k);
            if (n <= 0) continue;
            ResourceLocation id = ResourceLocation.tryParse(k);
            Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
            if (item == null || item == net.minecraft.world.item.Items.AIR) continue;
            map.merge(item, n, Integer::sum);
        }
        Item primary = primaryItem();
        out.addAll(map.entrySet());
        out.sort((a, b) -> {
            if (a.getKey() == primary) return -1;
            if (b.getKey() == primary) return 1;
            return Integer.compare(b.getValue(), a.getValue());
        });
        return out;
    }

    public static Item primaryItem() {
        try {
            ResourceLocation id = ResourceLocation.tryParse(Config.PRIMARY_ITEM.get());
            Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
            return item == null ? net.minecraft.world.item.Items.AIR : item;
        } catch (IllegalStateException notLoaded) {
            return net.minecraft.world.item.Items.AIR;
        }
    }

    private static int capacity() {
        try {
            int c = Config.CAPACITY_PER_TYPE.get();
            return c <= 0 ? Integer.MAX_VALUE : c;
        } catch (IllegalStateException notLoaded) {
            return Integer.MAX_VALUE;
        }
    }

    public static int space(ItemStack pouch, Item item) {
        return Math.max(0, capacity() - get(pouch, item));
    }

    public static int insert(ItemStack pouch, Item item, int amount) {
        if (amount <= 0 || !canStore(item)) return 0;
        int add = Math.min(amount, space(pouch, item));
        if (add <= 0) return 0;
        CompoundTag d = data(pouch, true);
        String k = key(item);
        d.putInt(k, d.getInt(k) + add);
        return add;
    }

    public static int extract(ItemStack pouch, Item item, int amount) {
        if (amount <= 0) return 0;
        CompoundTag d = data(pouch, false);
        if (d == null) return 0;
        String k = key(item);
        int have = d.getInt(k);
        int take = Math.min(have, amount);
        if (take <= 0) return 0;
        if (have - take <= 0) d.remove(k);
        else d.putInt(k, have - take);
        return take;
    }

    public static int insertStack(ItemStack pouch, ItemStack stack) {
        if (!canStore(stack)) return 0;
        int moved = insert(pouch, stack.getItem(), stack.getCount());
        stack.shrink(moved);
        return moved;
    }

    public static Item firstToWithdraw(ItemStack pouch) {
        List<Map.Entry<Item, Integer>> list = entries(pouch);
        return list.isEmpty() ? null : list.get(0).getKey();
    }
}
