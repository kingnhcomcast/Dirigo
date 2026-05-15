package io.drahlek.dirigo.services;

import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.services.services.IBlockRegistrar;
import io.drahlek.dirigo.services.services.IClassDiscoveryService;
import io.drahlek.dirigo.services.services.IDataComponentRegistrar;
import io.drahlek.dirigo.services.services.IItemRegistrar;
import io.drahlek.dirigo.services.services.IMenuTypeRegistrar;
import io.drahlek.dirigo.services.services.IPlatformHelper;

import java.util.Optional;
import java.util.ServiceLoader;

// Service loaders are a built-in Java feature that allow us to locate implementations of an interface that vary from one
// environment to another. In the context of MultiLoader we use this feature to access a mock API in the common code that
// is swapped out for the platform specific implementation at runtime.
public class Services {

    // In this example we provide a platform helper which provides information about what platform the mod is running on.
    // For example this can be used to check if the code is running on Forge vs Fabric, or to ask the modloader if another
    // mod is loaded.
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final IItemRegistrar ITEM_REGISTRAR = load(IItemRegistrar.class);
    public static final IBlockRegistrar BLOCK_REGISTRAR = load(IBlockRegistrar.class);
    public static final IClassDiscoveryService CLASS_DISCOVERY = load(IClassDiscoveryService.class);
    public static final INetworkService NETWORK_SERVICE = load(INetworkService.class);
    public static final IDataComponentRegistrar DATA_COMPONENT_REGISTRAR = load(IDataComponentRegistrar.class);
    public static final IMenuTypeRegistrar MENU_TYPE_REGISTRAR = load(IMenuTypeRegistrar.class);

    // This code is used to load a service for the current environment. Your implementation of the service must be defined
    // manually by including a text file in META-INF/services named with the fully qualified class name of the service.
    // Inside the file you should write the fully qualified class name of the implementation to load for the platform. For
    // example our file on Forge points to ForgePlatformHelper while Fabric points to FabricPlatformHelper.
    public static <T> T load(Class<T> clazz) {

        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }

    public static <T> Optional<T> loadOptional(Class<T> clazz) {
        Optional<T> loadedService = ServiceLoader.load(clazz).findFirst();
        loadedService.ifPresent(service -> Constants.LOG.debug("Loaded {} for service {}", service, clazz));
        return loadedService;
    }
}
