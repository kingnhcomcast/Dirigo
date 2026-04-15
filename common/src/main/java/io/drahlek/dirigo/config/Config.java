package io.drahlek.dirigo.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.services.Services;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public abstract class Config<T> {
    private static final Map<String, Config<?>> CONFIGS = new ConcurrentHashMap<>();
    private static final List<Consumer<Config<?>>> REGISTRATION_LISTENERS = new CopyOnWriteArrayList<>();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private final String modId;
    private final String modName;
    private final Path path;
    private final Class<T> type;

    protected T data;

    protected Config(String modId, String fileName, Class<T> type) {
        this(modId, modId, fileName, type);
    }

    protected Config(String modId, String modName, String fileName, Class<T> type) {
        Path configDirectory = Services.PLATFORM.getConfigDirectory();

        if (modId == null || modId.isBlank()) {
            throw new IllegalArgumentException("modId cannot be null or blank");
        }
        if (modName == null || modName.isBlank()) {
            throw new IllegalArgumentException("modName cannot be null or blank");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName cannot be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        this.modId = modId;
        this.modName = modName;
        this.path = configDirectory.resolve(modId).resolve(fileName);
        this.type = type;
        try {
            this.data = type.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        CONFIGS.put(modId, this);
        REGISTRATION_LISTENERS.forEach(listener -> listener.accept(this));
    }

    public static Optional<Config<?>> getRegistered(String modId) {
        if (modId == null || modId.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(CONFIGS.get(modId));
    }

    public static Map<String, Config<?>> registeredConfigs() {
        return Collections.unmodifiableMap(CONFIGS);
    }

    public static void loadRegistered(MinecraftServer server) {
        CONFIGS.values().forEach(config -> config.load(server));
    }

    public static void addRegistrationListener(Consumer<Config<?>> listener) {
        if (listener == null) {
            return;
        }

        REGISTRATION_LISTENERS.add(listener);
        CONFIGS.values().forEach(listener);
    }

    public final T get() {
        return data;
    }

    public final String getModId() {
        return modId;
    }

    public final String getModName() {
        return modName;
    }

    public final Path getPath() {
        return path;
    }

    public final Class<T> getDataClass() {
        return type;
    }

    public final void load(MinecraftServer server) {
        try {
            Files.createDirectories(path.getParent());

            if (!Files.exists(path)) {
                save(server);
            }

            String json = Files.readString(path, StandardCharsets.UTF_8);
            loadFromJson(json);
        } catch (Exception e) {
            onLoadFailed(e);

            try {
                save(server);
            } catch (Exception saveException) {
                onSaveFailed(saveException);
            }
        }
    }

    public final void save(MinecraftServer server) {
        try {
            Files.createDirectories(path.getParent());
            String json = GSON.toJson(data);
            Files.writeString(path, json, StandardCharsets.UTF_8);
            syncToPlayers(server);
        } catch (Exception e) {
            onSaveFailed(e);
        }
    }

    public final void reload(MinecraftServer server) {
        load(server);
    }

    public final ConfigPayload toPayload() {
        return new ConfigPayload(modId, GSON.toJson(data));
    }

    public static void applyPayload(ConfigPayload payload) {
        if (payload == null) {
            return;
        }

        Config.getRegistered(payload.modId())
                .ifPresent(config -> config.loadFromJson(payload.json()));
    }

    private void loadFromJson(String json) {
        T loadedData = GSON.fromJson(json, type);
        if (loadedData == null) {
            Constants.LOG.warn("Ignoring empty config data for {} at {}", modId, path);
            return;
        }

        rejectOutOfRangeValues(loadedData);
        data = loadedData;
    }

    private void rejectOutOfRangeValues(T loadedData) {
        ConfigFieldUtil.configSettingFields(this).forEach(field -> {
            Object loadedValue = readFieldValue(loadedData, field);
            String validationError = ConfigFieldUtil.validateRange(field, loadedValue);
            if (validationError == null) {
                return;
            }

            Object currentValue = readFieldValue(data, field);
            if (ConfigFieldUtil.validateRange(field, currentValue) != null) {
                currentValue = ConfigFieldUtil.defaultValue(this, field);
            }

            writeFieldValue(loadedData, field, currentValue);
            Constants.LOG.warn("Ignoring invalid config value for {} at {}: {}", modId, path, validationError);
        });
    }

    private static Object readFieldValue(Object owner, Field field) {
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read config field " + field.getName(), e);
        }
    }

    private static void writeFieldValue(Object owner, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(owner, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write config field " + field.getName(), e);
        }
    }

    protected void onLoadFailed(Exception e) {
        e.printStackTrace();
    }

    protected void onSaveFailed(Exception e) {
        e.printStackTrace();
    }

    private void syncToPlayers(MinecraftServer server) {
        ConfigPayload payload = toPayload();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Services.NETWORK_SERVICE.sendToClient(player, payload);
        }
    }
}
