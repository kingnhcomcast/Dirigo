package io.drahlek.dirigo.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.drahlek.dirigo.services.Services;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class Config<T> {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private Path path;
    private final Class<T> type;

    protected T data;

    protected Config(String modId, String fileName, Class<T> type) {
        Path configDirectory = Services.PLATFORM.getConfigDirectory();

        if (modId == null || modId.isBlank()) {
            throw new IllegalArgumentException("modId cannot be null or blank");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName cannot be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        this.path = configDirectory.resolve(modId).resolve(fileName);
        this.type = type;
        try {
            this.data = type.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public final T get() {
        return data;
    }

    public final Path getPath() {
        return path;
    }

    public final void load() {
        try {
            Files.createDirectories(path.getParent());

            if (!Files.exists(path)) {
                save();
            }

            String json = Files.readString(path, StandardCharsets.UTF_8);
            data = GSON.fromJson(json, type);
        } catch (Exception e) {
            onLoadFailed(e);

            try {
                save();
            } catch (Exception saveException) {
                onSaveFailed(saveException);
            }
        }
    }

    public final void save() {
        try {
            Files.createDirectories(path.getParent());
            String json = GSON.toJson(data);
            Files.writeString(path, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            onSaveFailed(e);
        }
    }

    public final void reload() {
        load();
    }

    protected void onLoadFailed(Exception e) {
        e.printStackTrace();
    }

    protected void onSaveFailed(Exception e) {
        e.printStackTrace();
    }
}
