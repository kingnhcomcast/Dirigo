package io.drahlek.dirigo.services;

import io.drahlek.dirigo.services.services.IMenuTypeRegistrar;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

public class FabricMenuTypeRegistrar implements IMenuTypeRegistrar {
    @Override
    public synchronized <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenuType(
            String modId,
            String name,
            MenuFactory<T> factory
    ) {
        MenuType<T> type = Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(modId, name),
                createType(factory)
        );
        return () -> type;
    }

    private static <T extends AbstractContainerMenu> MenuType<T> createType(MenuFactory<T> factory) {
        return new MenuType<>(factory::create, FeatureFlagSet.of());
    }
}
