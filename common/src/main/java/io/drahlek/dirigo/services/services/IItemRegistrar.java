package io.drahlek.dirigo.services.services;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public interface IItemRegistrar {
    <T extends Item> void registerItem(String modId,
                      String name,
                      Class<T> clazz,
                      Function<Item.Properties, T> itemFactory,
                      ResourceKey<CreativeModeTab> resourceKey);
}
