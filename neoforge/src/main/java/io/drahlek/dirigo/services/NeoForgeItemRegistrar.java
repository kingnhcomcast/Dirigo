package io.drahlek.dirigo.services;

import io.drahlek.dirigo.services.services.IItemRegistrar;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class NeoForgeItemRegistrar implements IItemRegistrar {
    private static final Map<String, DeferredRegister.Items> ITEM_REGISTRIES = new HashMap<>();
    private static final Map<ResourceKey<CreativeModeTab>, List<DeferredItem<Item>>> TAB_ITEMS = new HashMap<>();
    private static boolean creativeTabListenerRegistered = false;

    public void initialize(IEventBus eventBus, String modId) {
        getOrCreateRegistry(modId).register(eventBus);
        if (!creativeTabListenerRegistered) {
            eventBus.addListener(NeoForgeItemRegistrar::buildContents);
            creativeTabListenerRegistered = true;
        }
    }

    @Override
    public <T extends Item> void registerItem(String modId, String name, Class<T> clazz, Function<Item.Properties, T> itemFactory, ResourceKey<CreativeModeTab> resourceKey) {
        DeferredRegister.Items items = getOrCreateRegistry(modId);
        DeferredItem<Item> deferredItem = items.registerItem(name, itemFactory);
        ResourceKey<CreativeModeTab> tab = resourceKey != null ? resourceKey : CreativeModeTabs.COMBAT;
        TAB_ITEMS.computeIfAbsent(tab, key -> new ArrayList<>()).add(deferredItem);
    }

    private static DeferredRegister.Items getOrCreateRegistry(String modId) {
        return ITEM_REGISTRIES.computeIfAbsent(modId, DeferredRegister::createItems);
    }

    @SubscribeEvent // on the mod event bus
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        List<DeferredItem<Item>> tabItems = TAB_ITEMS.get(event.getTabKey());
        if (tabItems != null) {
            tabItems.forEach(event::accept);
        }
    }
}
