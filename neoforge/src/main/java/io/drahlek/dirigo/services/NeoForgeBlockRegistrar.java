package io.drahlek.dirigo.services;

import io.drahlek.dirigo.services.services.IBlockRegistrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class NeoForgeBlockRegistrar implements IBlockRegistrar {
    private static final Map<String, DeferredRegister.Blocks> BLOCK_REGISTRIES = new ConcurrentHashMap<>();
    private static final Map<String, DeferredRegister.Items> ITEM_REGISTRIES = new ConcurrentHashMap<>();
    private static final Set<String> INITIALIZED_MODS = ConcurrentHashMap.newKeySet();
    private static final Map<ResourceKey<CreativeModeTab>, List<DeferredItem<Item>>> TAB_ITEMS = new ConcurrentHashMap<>();
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
        DeferredRegister.Blocks blocks = getOrCreateBlockRegistry(modId);
        DeferredRegister.Items items = getOrCreateItemRegistry(modId);

        DeferredBlock<T> deferredBlock = blocks.register(name, registryName -> {
            ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, registryName);
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().setId(blockKey);
            try {
                return clazz
                        .getDeclaredConstructor(BlockBehaviour.Properties.class)
                        .newInstance(properties);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });

        if (shouldRegisterItem) {
            DeferredItem<Item> deferredItem = items.register(name, registryName -> {
                ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, registryName);
                return new BlockItem(deferredBlock.get(), new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
            });

            if (creativeModeTabResourceKey != null) {
                TAB_ITEMS.computeIfAbsent(creativeModeTabResourceKey, key -> new CopyOnWriteArrayList<>()).add(deferredItem);
            }
        }

        return deferredBlock::get;
    }

    private static DeferredRegister.Blocks getOrCreateBlockRegistry(String modId) {
        return BLOCK_REGISTRIES.computeIfAbsent(modId, DeferredRegister::createBlocks);
    }

    private static DeferredRegister.Items getOrCreateItemRegistry(String modId) {
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
