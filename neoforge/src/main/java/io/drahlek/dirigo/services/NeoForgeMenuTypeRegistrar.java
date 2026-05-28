package io.drahlek.dirigo.services;

import io.drahlek.dirigo.services.services.IMenuTypeRegistrar;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class NeoForgeMenuTypeRegistrar implements IMenuTypeRegistrar {
    private static final Map<String, DeferredRegister<MenuType<?>>> REGISTRIES = new ConcurrentHashMap<>();
    private static final Set<String> INITIALIZED_MODS = ConcurrentHashMap.newKeySet();

    public synchronized void initialize(IEventBus eventBus, String modId) {
        if (INITIALIZED_MODS.add(modId)) {
            getOrCreateRegistry(modId).register(eventBus);
        }
    }

    @Override
    public synchronized <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenuType(
            String modId,
            String name,
            MenuFactory<T> factory
    ) {
        RegistryObject<MenuType<T>> holder = getOrCreateRegistry(modId)
                .register(name, () -> IForgeMenuType.create((containerId, inventory, data) -> factory.create(containerId, inventory)));
        return holder::get;
    }

    private static DeferredRegister<MenuType<?>> getOrCreateRegistry(String modId) {
        return REGISTRIES.computeIfAbsent(modId, id -> DeferredRegister.create(ForgeRegistries.MENU_TYPES, id));
    }
}