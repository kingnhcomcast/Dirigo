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
    //TODO do we add a custom creative tab just for this mod?

    @Override
    public synchronized <T extends Item> void registerItem(String modId, String name, Class<T> clazz, ResourceKey<CreativeModeTab> creativeModeTabResourceKey) {
        // Create the item key.
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(modId, name));

        // Set the item id before constructing the item (required by Item.Properties).
        try {
            Properties properties = new Properties();

            // Create the item instance.
            T item = clazz
                    .getDeclaredConstructor(Properties.class)
                    .newInstance(properties);

            // Register the item.
            //TODO do i need to save the key?
            Registry.register(BuiltInRegistries.ITEM, itemKey, item);

            // Add to the configured creative tab, if one was provided.
            if(creativeModeTabResourceKey != null) {
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
