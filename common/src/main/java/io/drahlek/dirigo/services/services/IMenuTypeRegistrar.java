package io.drahlek.dirigo.services.services;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

public interface IMenuTypeRegistrar {
    <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenuType(
            String modId,
            String name,
            MenuFactory<T> factory
    );

    @FunctionalInterface
    interface MenuFactory<T extends AbstractContainerMenu> {
        T create(int containerId, Inventory inventory);
    }
}
