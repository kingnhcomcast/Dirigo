package io.drahlek.dirigo.registrars;

import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.annotation.Block;
import io.drahlek.dirigo.services.Services;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class BlockRegistrar {
    public static Map<String, Supplier<net.minecraft.world.level.block.Block>> blocks =  new HashMap<>();
    /**
     * Scans the given package for classes annotated with @Block
     * and registers them with Minecraft automatically.
     *
     * @param modId       The mod id to use for registration
     * @param packageName The package to scan (e.g., "com.mymod.item")
     */
    public static void registerBlocks(String modId, String packageName) {
        // Scan only the specified package — much faster and safer
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> blockClasses = reflections.getTypesAnnotatedWith(Block.class);

        for (Class<?> clazz : blockClasses) {
            // Must extend Minecraft's Item class
            if (!net.minecraft.world.level.block.Block.class.isAssignableFrom(clazz)) {
                Constants.LOG.warn("Failed to register block {} not an Block subclass", clazz.getSimpleName());
                continue;
            }

            try {
                @SuppressWarnings("unchecked")
                Class<? extends net.minecraft.world.level.block.Block> blockClass =
                        (Class<? extends net.minecraft.world.level.block.Block>) clazz;

                // Get the annotation and derive ID
                Block annotation = clazz.getAnnotation(Block.class);
                if (annotation != null) {
                    String id = annotation.id().isEmpty()
                            ? clazz.getSimpleName().toLowerCase()
                            : annotation.id();

                    //if a creative tab was provided, add it
//                    ResourceKey<CreativeModeTab> resourceKey;
//                    if (!annotation.creativeTab().isEmpty()) {
//                        resourceKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace(annotation.creativeTab()));
//                    } else {
//                        resourceKey = null;
//                    }

                    // Register in the Minecraft registries
                    blocks.put(annotation.id(), Services.BLOCK_REGISTRAR.registerBlock(modId, id, blockClass, annotation.registerItem()));

                    System.out.println("Registered block: " + modId + ":" + id);
                }
            } catch (Exception e) {
                Constants.LOG.error("Failed to register block {}", clazz.getName(), e);
            }
        }
    }
}
