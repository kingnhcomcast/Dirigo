package io.drahlek.dirigo.services.services;

import io.drahlek.dirigo.config.Config;

public interface IConfigScreenRegistrar {
    void initialize();

    void register(Config<?> config);
}
