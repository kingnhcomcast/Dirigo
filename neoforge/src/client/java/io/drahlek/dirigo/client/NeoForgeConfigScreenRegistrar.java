package io.drahlek.dirigo.client;

import io.drahlek.dirigo.client.config.GeneratedConfigScreen;
import io.drahlek.dirigo.config.Config;
import io.drahlek.dirigo.config.ConfigFieldUtil;
import io.drahlek.dirigo.services.Services;
import io.drahlek.dirigo.services.services.IConfigScreenRegistrar;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;

public final class NeoForgeConfigScreenRegistrar implements IConfigScreenRegistrar {
    @Override
    public void initialize() {
        if (!Services.PLATFORM.isModLoaded("cloth_config")) {
            return;
        }

        Config.addRegistrationListener(this::register);
    }

    @Override
    public void register(Config<?> config) {
        if (ConfigFieldUtil.configSettingFields(config).findAny().isEmpty()) {
            return;
        }

        ModList.get()
                .getModContainerById(config.getModId())
                .ifPresent(container -> {
                    if (container.getCustomExtension(ConfigScreenHandler.ConfigScreenFactory.class).isPresent()) {
                        return;
                    }

                    container.registerExtensionPoint(
                            ConfigScreenHandler.ConfigScreenFactory.class,
                            () -> new ConfigScreenHandler.ConfigScreenFactory((ignoredMinecraft, parent) ->
                                    GeneratedConfigScreen.create(parent, config))
                    );
                });
    }
}