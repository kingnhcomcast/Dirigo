package io.drahlek.dirigo.services;

import io.drahlek.dirigo.services.services.IItemRegistrar;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class NeoForgeItemRegistrar implements IItemRegistrar {
    private static final Map<String, DeferredRegister.Items> ITEM_REGISTRIES = new ConcurrentHashMap<>();
    private static final Set<String> INITIALIZED_MODS = ConcurrentHashMap.newKeySet();
    private static final Map<ResourceKey<CreativeModeTab>, List<DeferredItem<Item>>> TAB_ITEMS = new ConcurrentHashMap<>();
    private static final AtomicBoolean CREATIVE_TAB_LISTENER_REGISTERED = new AtomicBoolean();

    public synchronized void initialize(IEventBus eventBus, String modId) {
        if (INITIALIZED_MODS.add(modId)) {
            getOrCreateRegistry(modId).register(eventBus);
        }
        if (CREATIVE_TAB_LISTENER_REGISTERED.compareAndSet(false, true)) {
            eventBus.addListener(NeoForgeItemRegistrar::buildContents);
        }
    }

    @Override
    public synchronized <T extends Item> void registerItem(String modId, String name, Class<T> clazz, ResourceKey<CreativeModeTab> creativeModeTabResourceKey) {
        DeferredRegister.Items items = getOrCreateRegistry(modId);
        DeferredItem<Item> deferredItem = items.registerItem(name, properties -> {
            try {
                return clazz.getDeclaredConstructor(Item.Properties.class).newInstance(properties);
            } catch (NoSuchMethodException ignored) {
                try {
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to instantiate item: " + clazz.getName(), e);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate item: " + clazz.getName(), e);
            }
        });
        if (creativeModeTabResourceKey != null) {
            TAB_ITEMS.computeIfAbsent(creativeModeTabResourceKey, key -> new CopyOnWriteArrayList<>()).add(deferredItem);
        }
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
