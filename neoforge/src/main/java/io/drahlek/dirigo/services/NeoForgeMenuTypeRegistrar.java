package io.drahlek.dirigo.services;

import io.drahlek.dirigo.services.services.IMenuTypeRegistrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class NeoForgeMenuTypeRegistrar implements IMenuTypeRegistrar {
    private static final Map<String, DeferredRegister<MenuType<?>>> REGISTRIES = new HashMap<>();
    private static final Set<String> INITIALIZED_MODS = new HashSet<>();

    public void initialize(IEventBus eventBus, String modId) {
        if (INITIALIZED_MODS.add(modId)) {
            getOrCreateRegistry(modId).register(eventBus);
        }
    }

    @Override
    public <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenuType(
            String modId,
            String name,
            MenuFactory<T> factory
    ) {
        DeferredHolder<MenuType<?>, MenuType<T>> holder = getOrCreateRegistry(modId)
                .register(name, () -> IMenuTypeExtension.create((containerId, inventory, data) -> factory.create(containerId, inventory)));
        return holder::get;
    }

    private static DeferredRegister<MenuType<?>> getOrCreateRegistry(String modId) {
        return REGISTRIES.computeIfAbsent(modId, id -> DeferredRegister.create(Registries.MENU, id));
    }
}
