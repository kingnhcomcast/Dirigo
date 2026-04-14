package io.drahlek.dirigo.registrars;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.annotation.Command;
import io.drahlek.dirigo.annotation.CommandArgument;
import io.drahlek.dirigo.annotation.CommandArgumentType;
import io.drahlek.dirigo.annotation.ConfigSetting;
import io.drahlek.dirigo.config.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.Permissions;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CommandRegistrar {
    private static final String DIRIGO_COMMAND_PACKAGE = "io.drahlek.dirigo.commands";

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, String modId, String packageName) {
        if (modId == null || modId.isBlank()) {
            Constants.LOG.error("Failed to register commands because modId is blank");
            return;
        }

        Set<Class<?>> commandClasses = collectCommandClasses(packageName);
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(modId);
        boolean hasCommands = false;

        for (Class<?> clazz : commandClasses) {
            Command annotation = clazz.getAnnotation(Command.class);
            if (annotation == null) {
                continue;
            }
            if (!shouldRegisterCommand(annotation, modId)) {
                continue;
            }

            String commandPath = annotation.value();
            String[] pathSegments = parsePath(commandPath, clazz);
            if (pathSegments.length == 0) {
                continue;
            }

            CommandArgument[] arguments = annotation.arguments();
            if (!isValidArgumentLayout(arguments, clazz)) {
                continue;
            }

            try {
                root.then(buildCommandBranch(pathSegments, arguments, annotation.requiresOp(), getCommand(clazz), modId));
                hasCommands = true;
                Constants.LOG.info("Registered command {} from {} for {}", commandPath, clazz.getName(), modId);
            } catch (RuntimeException e) {
                Constants.LOG.error("Failed to register command {} from {}", commandPath, clazz.getName(), e);
            }
        }

        if (hasCommands) {
            dispatcher.register(root);
        }
    }

    private static Set<Class<?>> collectCommandClasses(String packageName) {
        Set<Class<?>> commandClasses = new LinkedHashSet<>();
        commandClasses.addAll(new Reflections(packageName).getTypesAnnotatedWith(Command.class));

        if (!DIRIGO_COMMAND_PACKAGE.equals(packageName)) {
            commandClasses.addAll(new Reflections(DIRIGO_COMMAND_PACKAGE).getTypesAnnotatedWith(Command.class));
        }

        return commandClasses;
    }

    private static boolean shouldRegisterCommand(Command command, String modId) {
        return !command.requiresConfig() || Config.getRegistered(modId).isPresent();
    }

    private static boolean isOpPlayer(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildCommandBranch(
            String[] pathSegments,
            CommandArgument[] arguments,
            boolean requiresOp,
            com.mojang.brigadier.Command<CommandSourceStack> command,
            String modId
    ) {
        int totalSegments = pathSegments.length + arguments.length;
        List<ArgumentBuilder<CommandSourceStack, ?>> branches = buildCommandBranches(
                0,
                pathSegments,
                arguments,
                totalSegments,
                requiresOp,
                command,
                modId
        );

        if (branches.size() != 1) {
            throw new IllegalStateException("Command branch must have exactly one root node");
        }

        return branches.get(0);
    }

    private static List<ArgumentBuilder<CommandSourceStack, ?>> buildCommandBranches(
            int index,
            String[] pathSegments,
            CommandArgument[] arguments,
            int totalSegments,
            boolean requiresOp,
            com.mojang.brigadier.Command<CommandSourceStack> command,
            String modId
    ) {
        List<ArgumentBuilder<CommandSourceStack, ?>> branches = createBuilders(index, pathSegments, arguments, modId);

        for (ArgumentBuilder<CommandSourceStack, ?> branch : branches) {
            if (isExecutableNode(index, pathSegments.length, totalSegments, arguments)) {
                makeExecutable(branch, command, requiresOp);
            }

            if (index + 1 < totalSegments) {
                for (ArgumentBuilder<CommandSourceStack, ?> child : buildCommandBranches(
                        index + 1,
                        pathSegments,
                        arguments,
                        totalSegments,
                        requiresOp,
                        command,
                        modId
                )) {
                    branch.then(child);
                }
            }
        }

        return branches;
    }

    private static void makeExecutable(
            ArgumentBuilder<CommandSourceStack, ?> builder,
            com.mojang.brigadier.Command<CommandSourceStack> command,
            boolean requiresOp
    ) {
        if (requiresOp) {
            builder.requires(CommandRegistrar::isOpPlayer);
        }
        builder.executes(command);
    }

    private static List<ArgumentBuilder<CommandSourceStack, ?>> createBuilders(
            int index,
            String[] pathSegments,
            CommandArgument[] arguments,
            String modId
    ) {
        if (index < pathSegments.length) {
            String pathSegment = pathSegments[index];
            if (pathSegment.equals(modId)) {
                Constants.LOG.warn("Command path segment '{}' matches mod id; command will include it twice", pathSegment);
            }
            return List.of(Commands.literal(pathSegment));
        }

        CommandArgument argument = arguments[index - pathSegments.length];
        if (argument.type() == CommandArgumentType.CONFIG_SETTING) {
            return configSettingBuilders(modId);
        }

        return List.of(Commands.argument(argument.name(), getArgumentType(argument)));
    }

    private static List<ArgumentBuilder<CommandSourceStack, ?>> configSettingBuilders(String modId) {
        Config<?> config = Config.getRegistered(modId)
                .orElseThrow(() -> new IllegalStateException("No config registered for " + modId));
        List<ArgumentBuilder<CommandSourceStack, ?>> builders = new ArrayList<>();

        for (String settingName : configSettingNames(config)) {
            builders.add(Commands.literal(settingName));
        }

        return builders;
    }

    private static List<String> configSettingNames(Config<?> config) {
        Set<String> names = new LinkedHashSet<>();
        Arrays.stream(config.getDataClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ConfigSetting.class))
                .sorted(Comparator.comparing(CommandRegistrar::settingName))
                .forEach(field -> names.add(settingName(field)));

        return new ArrayList<>(names);
    }

    private static String settingName(Field field) {
        ConfigSetting setting = field.getAnnotation(ConfigSetting.class);
        if (setting == null || setting.value().isBlank()) {
            return field.getName();
        }

        return setting.value();
    }

    private static boolean isExecutableNode(
            int index,
            int pathSegmentCount,
            int totalSegmentCount,
            CommandArgument[] arguments
    ) {
        if (index == totalSegmentCount - 1) {
            return true;
        }

        int nextIndex = index + 1;
        return nextIndex >= pathSegmentCount && arguments[nextIndex - pathSegmentCount].optional();
    }

    private static String[] parsePath(String commandPath, Class<?> clazz) {
        if (commandPath == null || commandPath.isBlank()) {
            Constants.LOG.error("Failed to register command {} because its @Command value is blank", clazz.getName());
            return new String[0];
        }

        String[] pathSegments = commandPath.trim().split("\\.");
        for (String pathSegment : pathSegments) {
            if (pathSegment.isBlank()) {
                Constants.LOG.error("Failed to register command {} because path '{}' has an empty segment", clazz.getName(), commandPath);
                return new String[0];
            }
        }
        return pathSegments;
    }

    private static boolean isValidArgumentLayout(CommandArgument[] arguments, Class<?> clazz) {
        boolean foundOptionalArgument = false;

        for (int index = 0; index < arguments.length; index++) {
            CommandArgument argument = arguments[index];

            if (argument.name() == null || argument.name().isBlank()) {
                Constants.LOG.error("Failed to register command {} because an argument name is blank", clazz.getName());
                return false;
            }
            if (foundOptionalArgument && !argument.optional()) {
                Constants.LOG.error("Failed to register command {} because required argument '{}' follows an optional argument", clazz.getName(), argument.name());
                return false;
            }
            if (argument.type() == CommandArgumentType.GREEDY_STRING && index != arguments.length - 1) {
                Constants.LOG.error("Failed to register command {} because greedy string argument '{}' is not the final argument", clazz.getName(), argument.name());
                return false;
            }
            if (argument.type() == CommandArgumentType.CONFIG_SETTING && argument.optional()) {
                Constants.LOG.error("Failed to register command {} because config setting argument '{}' cannot be optional", clazz.getName(), argument.name());
                return false;
            }

            foundOptionalArgument = foundOptionalArgument || argument.optional();
        }

        return true;
    }

    private static ArgumentType<?> getArgumentType(CommandArgument argument) {
        return switch (argument.type()) {
            case CONFIG_SETTING -> throw new IllegalArgumentException("CONFIG_SETTING is expanded into literal nodes");
            case WORD -> StringArgumentType.word();
            case STRING -> StringArgumentType.string();
            case GREEDY_STRING -> StringArgumentType.greedyString();
            case BOOLEAN -> BoolArgumentType.bool();
            case INTEGER -> IntegerArgumentType.integer();
            case LONG -> LongArgumentType.longArg();
            case FLOAT -> FloatArgumentType.floatArg();
            case DOUBLE -> DoubleArgumentType.doubleArg();
        };
    }

    private static com.mojang.brigadier.Command<CommandSourceStack> getCommand(Class<?> clazz) {
        try {
            Method method = clazz.getMethod("run", CommandContext.class);
            if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != int.class) {
                throw new IllegalArgumentException(
                        clazz.getName() + " must declare public static int run(CommandContext<CommandSourceStack> context)"
                );
            }

            return context -> {
                try {
                    return (int) method.invoke(null, context);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof CommandSyntaxException commandSyntaxException) {
                        throw commandSyntaxException;
                    }
                    if (cause instanceof RuntimeException runtimeException) {
                        throw runtimeException;
                    }
                    if (cause instanceof Error error) {
                        throw error;
                    }
                    throw new RuntimeException("Failed to run command " + clazz.getName(), cause);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to access command " + clazz.getName(), e);
                }
            };

        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                    clazz.getName() + " must declare public static int run(CommandContext<CommandSourceStack> context)",
                    e
            );
        }
    }
}
