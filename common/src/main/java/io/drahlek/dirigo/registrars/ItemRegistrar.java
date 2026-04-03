package io.drahlek.dirigo.registrars;

import io.drahlek.dirigo.annotation.Item;
import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.platform.services.IItemRegistrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import org.reflections.Reflections;
import java.util.Set;
import java.util.function.Function;

public class ItemRegistrar {
    /**
     * Scans the given package for classes annotated with @Item
     * and registers them with Minecraft automatically.
     *
     * @param modId       The mod id to use for registration
     * @param packageName The package to scan (e.g., "com.mymod.item")
     */
    public static void registerItems(IItemRegistrar itemRegistrar, String modId, String packageName) {
        // Scan only the specified package — much faster and safer
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> itemClasses = reflections.getTypesAnnotatedWith(Item.class);

        for (Class<?> clazz : itemClasses) {
            // Must extend Minecraft's Item class
            if (!net.minecraft.world.item.Item.class.isAssignableFrom(clazz)) {
                Constants.LOG.warn("Failed to register item {} not an Item subclass", clazz.getSimpleName());
                continue;
            }

            try {
                // Get the annotation and derive ID
                Item annotation = clazz.getAnnotation(Item.class);
                String id = annotation.id().isEmpty()
                        ? clazz.getSimpleName().toLowerCase()
                        : annotation.id();

                // Construct a factory function for the item
                Function<net.minecraft.world.item.Item.Properties, ? extends net.minecraft.world.item.Item> factory = props -> {
                    try {
                        // Assume the item class has a constructor that takes Item.Properties
                        return (net.minecraft.world.item.Item) clazz.getConstructor(net.minecraft.world.item.Item.Properties.class)
                                .newInstance(props);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to instantiate item: " + clazz.getName(), e);
                    }
                };

                //if a creative tab was provided, add it
                ResourceKey<CreativeModeTab> resourceKey;
                if(!annotation.creativeTab().isEmpty()) {
                    resourceKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace(annotation.creativeTab()));
                } else  {
                    resourceKey = null;
                }

                // Register in the Minecraft registries
                registerItem(itemRegistrar, modId, id, clazz, factory, resourceKey);
                System.out.println("Registered item: " + modId + ":" + id);

            } catch (Exception e) {
                Constants.LOG.error("Failed to register item {}", clazz.getName(), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.world.item.Item> void registerItem(
            IItemRegistrar itemRegistrar,
            String modId,
            String id,
            Class<?> clazz,
            Function<net.minecraft.world.item.Item.Properties, ? extends net.minecraft.world.item.Item> factory,
            ResourceKey<CreativeModeTab> resourceKey
    ) {
        itemRegistrar.registerItem(
                modId,
                id,
                (Class<T>) clazz,
                (Function<net.minecraft.world.item.Item.Properties, T>) factory,
                resourceKey
        );
    }
}
