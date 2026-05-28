package io.drahlek.dirigo.services;

import io.drahlek.dirigo.services.services.IItemRegistrar;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class NeoForgeItemRegistrar implements IItemRegistrar {
    private static final Map<String, DeferredRegister<Item>> ITEM_REGISTRIES = new ConcurrentHashMap<>();
    private static final Set<String> INITIALIZED_MODS = ConcurrentHashMap.newKeySet();
    private static final Map<ResourceKey<CreativeModeTab>, List<RegistryObject<Item>>> TAB_ITEMS = new ConcurrentHashMap<>();
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
        RegistryObject<Item> deferredItem = getOrCreateRegistry(modId).register(name, () -> {
            try {
                return clazz.getDeclaredConstructor(Item.Properties.class).newInstance(new Item.Properties());
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

    private static DeferredRegister<Item> getOrCreateRegistry(String modId) {
        return ITEM_REGISTRIES.computeIfAbsent(modId, id -> DeferredRegister.create(ForgeRegistries.ITEMS, id));
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        List<RegistryObject<Item>> tabItems = TAB_ITEMS.get(event.getTabKey());
        if (tabItems != null) {
            tabItems.forEach(item -> event.accept(item.get()));
        }
    }
}