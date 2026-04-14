package io.drahlek.dirigo.client;

import io.drahlek.dirigo.config.Config;
import io.drahlek.dirigo.config.ConfigFieldUtil;
import io.drahlek.dirigo.services.services.IConfigScreenRegistrar;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricConfigScreenRegistrar implements IConfigScreenRegistrar {
    private static final Map<String, Config<?>> CONFIGS = new ConcurrentHashMap<>();

    public static Map<String, Config<?>> registeredConfigs() {
        return Collections.unmodifiableMap(CONFIGS);
    }

    @Override
    public void initialize() {
        Config.addRegistrationListener(this::register);
    }

    @Override
    public void register(Config<?> config) {
        if (ConfigFieldUtil.configSettingFields(config).findAny().isEmpty()) {
            return;
        }

        CONFIGS.put(config.getModId(), config);
    }
}
