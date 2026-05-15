package io.drahlek.dirigo.blockentity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Adds extra valid blocks to an existing block entity type.
 * <p>
 * Vanilla {@link BlockEntityType} instances are built with a fixed set of valid blocks. That is
 * fine until a mod wants a custom block subclass to reuse an existing block entity type, such as a
 * custom campfire block that should still use the vanilla campfire block entity. This registry lets
 * Dirigo's {@code BlockEntityTypeMixin} answer "yes" for those extra blocks without replacing the
 * original block entity type.
 * <p>
 * Preferred usage is declarative through {@code @Block(validBlockEntityTypes = {...})}. The block
 * registrar reads that metadata automatically and registers the compatibility mapping for you. Call
 * the methods here directly only when the mapping needs to be decided dynamically at runtime.
 */
public final class BlockEntityTypeCompat {
    private static final Map<ResourceLocation, List<Supplier<? extends Block>>> EXTRA_VALID_BLOCKS = new HashMap<>();

    private BlockEntityTypeCompat() {
    }

    /**
     * Registers a block supplier as valid for the given block entity type.
     * <p>
     * Use this overload when you already have the live {@link BlockEntityType} instance.
     */
    public static void addValidBlock(BlockEntityType<?> type, Supplier<? extends Block> blockSupplier) {
        Objects.requireNonNull(type, "type");
        ResourceLocation blockEntityTypeId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
        if (blockEntityTypeId == null) {
            throw new IllegalArgumentException("Unregistered block entity type: " + type);
        }

        addValidBlock(blockEntityTypeId, blockSupplier);
    }

    /**
     * Registers a block supplier as valid for the block entity type stored at the given registry id.
     * <p>
     * This is what Dirigo uses behind {@code @Block(validBlockEntityTypes = {...})}. Storing the
     * mapping by registry id keeps the registration order flexible and avoids forcing mods to look up
     * the target block entity type eagerly.
     */
    public static void addValidBlock(ResourceLocation blockEntityTypeId, Supplier<? extends Block> blockSupplier) {
        Objects.requireNonNull(blockEntityTypeId, "blockEntityTypeId");
        Objects.requireNonNull(blockSupplier, "blockSupplier");
        EXTRA_VALID_BLOCKS.computeIfAbsent(blockEntityTypeId, ignored -> new CopyOnWriteArrayList<>()).add(blockSupplier);
    }

    /**
     * Returns whether the given state should be treated as valid for the supplied block entity type.
     * <p>
     * This method is called by Dirigo's block entity type mixin after vanilla has already said the
     * state is not valid. Mods should normally not need to call this directly.
     */
    public static boolean isValid(BlockEntityType<?> type, BlockState state) {
        ResourceLocation blockEntityTypeId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
        if (blockEntityTypeId == null) {
            return false;
        }

        List<Supplier<? extends Block>> blockSuppliers = EXTRA_VALID_BLOCKS.get(blockEntityTypeId);
        if (blockSuppliers == null || blockSuppliers.isEmpty()) {
            return false;
        }

        for (Supplier<? extends Block> blockSupplier : blockSuppliers) {
            Block block = blockSupplier.get();
            if (block != null && state.is(block)) {
                return true;
            }
        }

        return false;
    }
}
