package io.drahlek.dirigo.services;

import io.drahlek.dirigo.services.services.IItemRegistrar;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;

import java.lang.reflect.InvocationTargetException;

public class FabricItemRegistrar implements IItemRegistrar {
    @Override
    public synchronized <T extends Item> void registerItem(String modId, String name, Class<T> clazz, ResourceKey<CreativeModeTab> creativeModeTabResourceKey) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, new ResourceLocation(modId, name));

        try {
            Properties properties = new Properties();
            T item = clazz
                    .getDeclaredConstructor(Properties.class)
                    .newInstance(properties);

            Registry.register(BuiltInRegistries.ITEM, itemKey, item);

            if (creativeModeTabResourceKey != null) {
                ItemGroupEvents.modifyEntriesEvent(creativeModeTabResourceKey)
                        .register((entries) -> {
                            entries.getDisplayStacks().add(item.getDefaultInstance());
                            entries.getSearchTabStacks().add(item.getDefaultInstance());
                        });
            }
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}