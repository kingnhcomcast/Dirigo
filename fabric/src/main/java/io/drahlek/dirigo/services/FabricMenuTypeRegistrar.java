package io.drahlek.dirigo.services;

import io.drahlek.dirigo.services.services.IMenuTypeRegistrar;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
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

    @SuppressWarnings("unchecked")
    private static <T extends AbstractContainerMenu> MenuType<T> createType(MenuFactory<T> factory) {
        try {
            Class<?> supplierClass = Class.forName("net.minecraft.world.inventory.MenuType$MenuSupplier");
            Object supplier = Proxy.newProxyInstance(
                    MenuType.class.getClassLoader(),
                    new Class<?>[]{supplierClass},
                    (proxy, method, args) -> {
                        if ("create".equals(method.getName())) {
                            return factory.create((int) args[0], (Inventory) args[1]);
                        }
                        if ("toString".equals(method.getName())) {
                            return factory.toString();
                        }
                        if ("hashCode".equals(method.getName())) {
                            return System.identityHashCode(proxy);
                        }
                        if ("equals".equals(method.getName())) {
                            return proxy == args[0];
                        }
                        throw new UnsupportedOperationException(method.toString());
                    }
            );
            Constructor<?> constructor = MenuType.class.getDeclaredConstructor(supplierClass, FeatureFlagSet.class);
            constructor.setAccessible(true);
            return (MenuType<T>) constructor.newInstance(supplier, FeatureFlagSet.of());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create menu type", e);
        }
    }
}
