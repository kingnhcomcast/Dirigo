package io.drahlek.dirigo.services;

import io.drahlek.dirigo.services.services.IBlockRegistrar;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

public class FabricBlockRegistrar implements IBlockRegistrar {
    @Override
    public synchronized <T extends Block> Supplier<Block> registerBlock(String modId, String name, Class<T> clazz, boolean shouldRegisterItem, ResourceKey<CreativeModeTab> creativeTab) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, new ResourceLocation(modId, name));
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of();

        try {
            Block block = clazz
                    .getDeclaredConstructor(BlockBehaviour.Properties.class)
                    .newInstance(properties);

            if (shouldRegisterItem) {
                ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, new ResourceLocation(modId, name));
                BlockItem blockItem = new BlockItem(block, new Item.Properties());
                Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
            }

            Block registeredBlock = Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

            if (creativeTab != null) {
                ItemGroupEvents.modifyEntriesEvent(creativeTab)
                        .register((itemGroup) -> itemGroup.accept(registeredBlock.asItem()));
            }
            return () -> registeredBlock;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}