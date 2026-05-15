package io.drahlek.dirigo.client.services;

import io.drahlek.dirigo.services.Services;
import io.drahlek.dirigo.services.services.IConfigScreenRegistrar;

import java.util.Optional;

public final class ClientServices {
    private static volatile IConfigScreenRegistrar configScreenRegistrar;
    private static volatile boolean loadedConfigScreenRegistrar;

    private ClientServices() {
    }

    public static Optional<IConfigScreenRegistrar> configScreenRegistrar() {
        if (!loadedConfigScreenRegistrar) {
            synchronized (ClientServices.class) {
                if (!loadedConfigScreenRegistrar) {
                    configScreenRegistrar = Services.loadOptional(IConfigScreenRegistrar.class).orElse(null);
                    loadedConfigScreenRegistrar = true;
                }
            }
        }

        return Optional.ofNullable(configScreenRegistrar);
    }
}
