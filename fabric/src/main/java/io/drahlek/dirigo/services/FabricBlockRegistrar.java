package io.drahlek.dirigo.services;

import io.drahlek.dirigo.services.services.IBlockRegistrar;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

public class FabricBlockRegistrar implements IBlockRegistrar {
    @Override
    public <T extends Block> Supplier<Block> registerBlock(String modId, String name, Class<T> clazz, boolean shouldRegisterItem) {
        // Create a registry key for the block
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(modId, name));
        // Create the block properties
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of();

        // Create the Block instance.
        try {
            Block block = clazz
                    .getDeclaredConstructor(BlockBehaviour.Properties.class)
                    .newInstance(properties);

            // Sometimes, you may not want to register an item for the block.
            // Eg: if it's a technical block like `minecraft:moving_piston` or `minecraft:end_gateway`
            if (shouldRegisterItem) {
                // Items need to be registered with a different type of registry key, but the ID
                // can be the same.
                ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(modId, name));

                BlockItem blockItem = new BlockItem(block, new Item.Properties());
                Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
            }

            Block registeredBlock = Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
            return () -> registeredBlock;

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }

}
