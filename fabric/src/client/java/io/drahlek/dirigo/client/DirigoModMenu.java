package io.drahlek.dirigo.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.drahlek.dirigo.client.config.GeneratedConfigScreen;
import io.drahlek.dirigo.config.Config;
import io.drahlek.dirigo.config.ConfigFieldUtil;
import net.fabricmc.loader.api.FabricLoader;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DirigoModMenu implements ModMenuApi {
    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        Map<String, ConfigScreenFactory<?>> factories = new LinkedHashMap<>();
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return factories;
        }

        Map<String, Config<?>> configs = FabricConfigScreenRegistrar.registeredConfigs();
        if (configs.isEmpty()) {
            configs = Config.registeredConfigs();
        }

        configs.forEach((modId, config) -> {
            if (ConfigFieldUtil.configSettingFields(config).findAny().isPresent()) {
                factories.put(modId, parent -> GeneratedConfigScreen.create(parent, config));
            }
        });
        return factories;
    }
}
