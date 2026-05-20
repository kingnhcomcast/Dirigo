package io.drahlek.dirigo.registrars;

import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.annotation.BlockEntity;
import io.drahlek.dirigo.services.Services;
import io.drahlek.dirigo.services.services.IBlockEntityTypeRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class BlockEntityRegistrar {
    public static final Map<String, Supplier<BlockEntityType<?>>> blockEntityTypes = new ConcurrentHashMap<>();

    /**
     * Scans the given package for classes annotated with @BlockEntity
     * and registers them with Minecraft automatically.
     *
     * @param modId       The mod id to use for registration
     * @param packageName The package to scan
     */
    public static void registerBlockEntities(String modId, String packageName) {
        IBlockEntityTypeRegistrar blockEntityTypeRegistrar = Services.BLOCK_ENTITY_TYPE_REGISTRAR;
        Set<Class<?>> blockEntityClasses = Services.CLASS_DISCOVERY.getTypesAnnotatedWith(packageName, BlockEntity.class);

        for (Class<?> clazz : blockEntityClasses) {
            if (!net.minecraft.world.level.block.entity.BlockEntity.class.isAssignableFrom(clazz)) {
                Constants.LOG.warn("Failed to register block entity {} not a BlockEntity subclass", clazz.getSimpleName());
                continue;
            }

            try {
                BlockEntity annotation = clazz.getAnnotation(BlockEntity.class);
                if (annotation != null) {
                    String id = annotation.id().isEmpty()
                            ? clazz.getSimpleName().toLowerCase()
                            : annotation.id();

                    registerBlockEntityType(blockEntityTypeRegistrar, modId, id, clazz);
                    Constants.LOG.info("Registered block entity type: {} : {}", modId, id);
                }
            } catch (Exception e) {
                Constants.LOG.error("Failed to register block entity {}", clazz.getName(), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> get(String id, Class<T> type) {
        Supplier<BlockEntityType<?>> supplier = blockEntityTypes.get(id);
        if (supplier == null) {
            throw new IllegalStateException("Block entity type has not been registered: " + id);
        }

        return (BlockEntityType<T>) supplier.get();
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> void registerBlockEntityType(
            IBlockEntityTypeRegistrar blockEntityTypeRegistrar,
            String modId,
            String id,
            Class<?> clazz
    ) {
        Supplier<BlockEntityType<T>> supplier = blockEntityTypeRegistrar.registerBlockEntityType(
                modId,
                id,
                (blockPos, blockState) -> createBlockEntity((Class<T>) clazz, blockPos, blockState)
        );
        blockEntityTypes.put(id, () -> supplier.get());
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> T createBlockEntity(
            Class<T> clazz,
            BlockPos blockPos,
            BlockState blockState
    ) {
        try {
            return clazz
                    .getDeclaredConstructor(BlockPos.class, BlockState.class)
                    .newInstance(blockPos, blockState);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Failed to create block entity " + clazz.getName(), e);
        }
    }
}
