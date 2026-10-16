package net.bandit.soulpouch;

import net.bandit.soulpouch.item.SoulPouchItem;
import net.bandit.soulpouch.network.Network;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.tags.TagKey;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(SoulPouch.MODID)
public class SoulPouch {
    public static final String MODID = "soulpouch";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Item> SOUL_POUCH = ITEMS.register("soul_pouch",
            () -> new SoulPouchItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));


    public static final TagKey<Item> POUCHABLE = TagKey.create(Registries.ITEM, id("pouchable"));

    public SoulPouch() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addToCreativeTab);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);

        MinecraftForge.EVENT_BUS.register(net.bandit.soulpouch.event.CommonEvents.class);
        MinecraftForge.EVENT_BUS.register(net.bandit.soulpouch.event.MobShardDrops.class);

        if (ModList.get().isLoaded("waystones")) {
            net.bandit.soulpouch.compat.waystones.WaystonesCompat.init();
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Network::register);
    }

    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(SOUL_POUCH);
        }
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    public static MutableComponent tr(String key, Object... args) {
        return Component.translatable(MODID + "." + key, args);
    }
}
