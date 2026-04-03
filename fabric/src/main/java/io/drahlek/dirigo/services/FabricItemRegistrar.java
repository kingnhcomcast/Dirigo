package io.drahlek.dirigo.services;

import io.drahlek.dirigo.platform.services.IItemRegistrar;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;

import java.util.function.Function;

public class FabricItemRegistrar implements IItemRegistrar {
    //TODO do we add a custom creative tab just for this mod?

    @Override
    public <T extends Item> void registerItem(String modId, String name, Class<T> clazz, Function<Item.Properties, T> itemFactory, ResourceKey<CreativeModeTab> resourceKey) {
        // Create the item key.
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(modId, name));

        // Set the item id before constructing the item (required by Item.Properties).
        try {
            Properties properties = (Properties) clazz.getField("PROPERTIES").get(null);
            properties.setId(itemKey);

            // Create the item instance.
            T item = itemFactory.apply(properties);

            // Register the item.
            //TODO do i need to save the key?
            Registry.register(BuiltInRegistries.ITEM, itemKey, item);

            //add to creative
           // if(resourceKey != null) {
                CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
                        .register((itemGroup) -> itemGroup.accept(item));
          //  }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
