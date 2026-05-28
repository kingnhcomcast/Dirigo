package io.drahlek.dirigo.registrars;

import io.drahlek.dirigo.Constants;

public class DataComponentRegistrar {
    public static void registerDataComponents(String modId, String packageName) {
        Constants.LOG.warn("Data components are not available on Minecraft 1.20.1; skipping {}", packageName);
    }
}