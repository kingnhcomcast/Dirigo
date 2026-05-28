package io.drahlek.dirigo.registrars;

import io.drahlek.dirigo.annotation.Item;
import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.services.Services;
import io.drahlek.dirigo.services.services.IItemRegistrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import java.util.Set;

public class ItemRegistrar {
    /**
     * Scans the given package for classes annotated with @Item
     * and registers them with Minecraft automatically.
     *
     * @param modId       The mod id to use for registration
     * @param packageName The package to scan (e.g., "com.mymod.item")
     */
    public static void registerItems(String modId, String packageName) {
        IItemRegistrar itemRegistrar = Services.ITEM_REGISTRAR;

        // Scan only the specified package — much faster and safer
        Set<Class<?>> itemClasses = Services.CLASS_DISCOVERY.getTypesAnnotatedWith(packageName, Item.class);

        for (Class<?> clazz : itemClasses) {
            // Must extend Minecraft's Item class
            if (!net.minecraft.world.item.Item.class.isAssignableFrom(clazz)) {
                Constants.LOG.warn("Failed to register item {} not an Item subclass", clazz.getSimpleName());
                continue;
            }

            try {
                // Get the annotation and derive ID
                Item annotation = clazz.getAnnotation(Item.class);
                if(annotation != null) {
                    String id = annotation.id().isEmpty()
                            ? clazz.getSimpleName().toLowerCase()
                            : annotation.id();

                    ResourceKey<CreativeModeTab> creativeTab = resolveCreativeTab(annotation.creativeTab());

                    // Register in the Minecraft registries
                    registerItem(itemRegistrar, modId, id, clazz, creativeTab);
                    System.out.println("Registered item: " + modId + ":" + id);
                }
            } catch (Exception e) {
                Constants.LOG.error("Failed to register item {}", clazz.getName(), e);
            }
        }
    }

    static ResourceKey<CreativeModeTab> resolveCreativeTab(String creativeTabId) {
        if (creativeTabId == null || creativeTabId.isBlank()) {
            return null;
        }

        ResourceLocation tabId;
        if (creativeTabId.contains(":")) {
            String[] parts = creativeTabId.split(":", 2);
            tabId = new ResourceLocation(parts[0], parts[1]);
        } else {
            tabId = new ResourceLocation(creativeTabId);
        }

        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, tabId);
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.world.item.Item> void registerItem(
            IItemRegistrar itemRegistrar,
            String modId,
            String id,
            Class<?> clazz,
            ResourceKey<CreativeModeTab> creativeModeTabResourceKey
    ) {
        itemRegistrar.registerItem(
                modId,
                id,
                (Class<T>) clazz,
                creativeModeTabResourceKey
        );
    }

    private static ResourceLocation creativeTabId(String id) {
        return id.contains(":") ? new ResourceLocation(id) : new ResourceLocation(id);
    }
}
