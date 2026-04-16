package io.drahlek.dirigo.client;

import io.drahlek.dirigo.client.services.ClientServices;
import io.drahlek.dirigo.services.services.IConfigScreenRegistrar;

public final class NeoForgeDirigoClient {
    private NeoForgeDirigoClient() {
    }

    public static void init() {
        ClientServices.configScreenRegistrar().ifPresent(IConfigScreenRegistrar::initialize);
    }
}
