package io.drahlek.examplemod;

import net.fabricmc.api.ClientModInitializer;

public class ExamplemodClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Constants.LOG.info("{} Client Initialize", Constants.MOD_NAME);    }
}
