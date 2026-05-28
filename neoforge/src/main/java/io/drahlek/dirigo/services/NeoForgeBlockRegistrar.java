package io.drahlek.dirigo.services;

import io.drahlek.dirigo.services.services.IBlockRegistrar;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class NeoForgeBlockRegistrar implements IBlockRegistrar {
    private static final Map<String, DeferredRegister<Block>> BLOCK_REGISTRIES = new ConcurrentHashMap<>();
    private static final Map<String, DeferredRegister<Item>> ITEM_REGISTRIES = new ConcurrentHashMap<>();
    private static final Set<String> INITIALIZED_MODS = ConcurrentHashMap.newKeySet();
    private static final Map<ResourceKey<CreativeModeTab>, List<RegistryObject<Item>>> TAB_ITEMS = new ConcurrentHashMap<>();
    private static final AtomicBoolean CREATIVE_TAB_LISTENER_REGISTERED = new AtomicBoolean();

    public synchronized void initialize(IEventBus eventBus, String modId) {
        if (INITIALIZED_MODS.add(modId)) {
            getOrCreateBlockRegistry(modId).register(eventBus);
            getOrCreateItemRegistry(modId).register(eventBus);
        }
        if (CREATIVE_TAB_LISTENER_REGISTERED.compareAndSet(false, true)) {
            eventBus.addListener(NeoForgeBlockRegistrar::buildContents);
        }
    }

    @Override
    public synchronized <T extends Block> Supplier<Block> registerBlock(String modId, String name, Class<T> clazz, boolean shouldRegisterItem, ResourceKey<CreativeModeTab> creativeModeTabResourceKey) {
        RegistryObject<T> deferredBlock = getOrCreateBlockRegistry(modId).register(name, () -> {
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of();
            try {
                return clazz
                        .getDeclaredConstructor(BlockBehaviour.Properties.class)
                        .newInstance(properties);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });

        if (shouldRegisterItem) {
            RegistryObject<Item> deferredItem = getOrCreateItemRegistry(modId).register(name, () ->
                    new BlockItem(deferredBlock.get(), new Item.Properties()));

            if (creativeModeTabResourceKey != null) {
                TAB_ITEMS.computeIfAbsent(creativeModeTabResourceKey, key -> new CopyOnWriteArrayList<>()).add(deferredItem);
            }
        }

        return deferredBlock::get;
    }

    private static DeferredRegister<Block> getOrCreateBlockRegistry(String modId) {
        return BLOCK_REGISTRIES.computeIfAbsent(modId, id -> DeferredRegister.create(ForgeRegistries.BLOCKS, id));
    }

    private static DeferredRegister<Item> getOrCreateItemRegistry(String modId) {
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