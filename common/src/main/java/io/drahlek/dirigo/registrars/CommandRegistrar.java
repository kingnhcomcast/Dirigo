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
import io.drahlek.dirigo.config.Config;
import io.drahlek.dirigo.config.ConfigFieldUtil;
import io.drahlek.dirigo.permissions.PermissionHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommandRegistrar {
    private static final String DIRIGO_COMMAND_PACKAGE = "io.drahlek.dirigo.commands";

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, String modId, String packageName) {
        if (modId == null || modId.isBlank()) {
            Constants.LOG.error("Failed to register commands because modId is blank");
            return;
        }

        Set<Class<?>> commandClasses = collectCommandClasses(packageName);
        List<ResolvedCommand> resolvedCommands = resolveCommands(commandClasses, modId);
        Set<String> opOnlyPrefixes = computeOpOnlyPrefixes(resolvedCommands);
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(modId);
        boolean hasCommands = false;

        for (ResolvedCommand resolved : resolvedCommands) {
            try {
                root.then(buildCommandBranch(
                        resolved.pathSegments(),
                        resolved.arguments(),
                        resolved.annotation().requiresOp(),
                        getCommand(resolved.clazz()),
                        modId,
                        opOnlyPrefixes
                ));
                hasCommands = true;
                Constants.LOG.info("Registered command {} from {} for {}", resolved.annotation().value(), resolved.clazz().getName(), modId);
            } catch (RuntimeException e) {
                Constants.LOG.error("Failed to register command {} from {}", resolved.annotation().value(), resolved.clazz().getName(), e);
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

    private static List<ResolvedCommand> resolveCommands(Set<Class<?>> commandClasses, String modId) {
        List<ResolvedCommand> resolvedCommands = new ArrayList<>();

        for (Class<?> clazz : commandClasses) {
            Command annotation = clazz.getAnnotation(Command.class);
            if (annotation == null || !shouldRegisterCommand(annotation, modId)) {
                continue;
            }

            String[] pathSegments = parsePath(annotation.value(), clazz);
            if (pathSegments.length == 0) {
                continue;
            }

            CommandArgument[] arguments = annotation.arguments();
            if (!isValidArgumentLayout(arguments, clazz)) {
                continue;
            }

            resolvedCommands.add(new ResolvedCommand(clazz, annotation, pathSegments, arguments));
        }

        return resolvedCommands;
    }

    private static Set<String> computeOpOnlyPrefixes(List<ResolvedCommand> commands) {
        Map<String, PrefixAccess> prefixAccess = new HashMap<>();

        for (ResolvedCommand command : commands) {
            String[] pathSegments = command.pathSegments();
            for (int index = 0; index < pathSegments.length; index++) {
                String prefix = String.join(".", Arrays.copyOfRange(pathSegments, 0, index + 1));
                PrefixAccess access = prefixAccess.computeIfAbsent(prefix, ignored -> new PrefixAccess());
                if (command.annotation().requiresOp()) {
                    access.hasOp = true;
                } else {
                    access.hasNonOp = true;
                }
            }
        }

        Set<String> opOnlyPrefixes = new LinkedHashSet<>();
        for (Map.Entry<String, PrefixAccess> entry : prefixAccess.entrySet()) {
            PrefixAccess access = entry.getValue();
            if (access.hasOp && !access.hasNonOp) {
                opOnlyPrefixes.add(entry.getKey());
            }
        }

        return opOnlyPrefixes;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildCommandBranch(
            String[] pathSegments,
            CommandArgument[] arguments,
            boolean requiresOp,
            com.mojang.brigadier.Command<CommandSourceStack> command,
            String modId,
            Set<String> opOnlyPrefixes
    ) {
        int totalSegments = pathSegments.length + arguments.length;
        List<ArgumentBuilder<CommandSourceStack, ?>> branches = buildCommandBranches(
                0,
                pathSegments,
                arguments,
                totalSegments,
                requiresOp,
                command,
                modId,
                opOnlyPrefixes
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
            String modId,
            Set<String> opOnlyPrefixes
    ) {
        List<ArgumentBuilder<CommandSourceStack, ?>> branches = createBuilders(index, pathSegments, arguments, modId, opOnlyPrefixes);

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
                        modId,
                        opOnlyPrefixes
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
            builder.requires(PermissionHelper::canUseOpCommands);
        }
        builder.executes(command);
    }

    private static List<ArgumentBuilder<CommandSourceStack, ?>> createBuilders(
            int index,
            String[] pathSegments,
            CommandArgument[] arguments,
            String modId,
            Set<String> opOnlyPrefixes
    ) {
        if (index < pathSegments.length) {
            String pathSegment = pathSegments[index];
            if (pathSegment.equals(modId)) {
                Constants.LOG.warn("Command path segment '{}' matches mod id; command will include it twice", pathSegment);
            }
            LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal(pathSegment);
            String prefix = String.join(".", Arrays.copyOfRange(pathSegments, 0, index + 1));
            if (opOnlyPrefixes.contains(prefix)) {
                literal.requires(PermissionHelper::canUseOpCommands);
            }
            return List.of(literal);
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
        ConfigFieldUtil.configSettingFields(config)
                .forEach(field -> names.add(ConfigFieldUtil.settingName(field)));

        return new ArrayList<>(names);
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

    private record ResolvedCommand(
            Class<?> clazz,
            Command annotation,
            String[] pathSegments,
            CommandArgument[] arguments
    ) {
    }

    private static final class PrefixAccess {
        private boolean hasOp;
        private boolean hasNonOp;
    }
}
