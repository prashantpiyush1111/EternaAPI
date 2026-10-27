package me.hapyl.eterna.module.command;

import com.google.common.collect.Maps;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.hapyl.eterna.EternaLogger;
import me.hapyl.eterna.module.util.Compute;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Represents a {@link SimpleCommand} processor, which is used to register commands.
 *
 * <p>Commands are registered through Paper's lifecycle command registrar, allowing
 * Paper to rebuild the command tree when required.</p>
 */
public class CommandProcessor {

    private static final Map<Plugin, List<SimpleCommand>> COMMANDS_BY_PLUGIN = Maps.newHashMap();

    private final Plugin plugin;

    /**
     * Creates a new {@link CommandProcessor} with the given plugin as delegate.
     *
     * @param plugin - The plugin delegate.
     */
    public CommandProcessor(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers the given {@link SimpleCommand}.
     *
     * @param simpleCommand - The command to register.
     */
    public void registerCommand(@NotNull SimpleCommand simpleCommand) {
        this.registerCommands(simpleCommand);
    }

    /**
     * Registers the given {@link SimpleCommand} varargs, in the exact order passed.
     *
     * @param commands - The commands to register.
     * @throws IllegalArgumentException if no commands are passed.
     */
    public void registerCommands(@NotNull SimpleCommand... commands) {
        if (commands.length == 0) {
            throw new IllegalArgumentException("There must be at least one command!");
        }

        final List<SimpleCommand> registeredCommands = List.of(commands);

        for (final SimpleCommand command : registeredCommands) {
            if (command == null) {
                throw new IllegalArgumentException("Commands cannot contain null elements!");
            }

            command.markRegistered();
        }

        COMMANDS_BY_PLUGIN.compute(plugin, (ignored, existing) -> {
            final List<SimpleCommand> updated = existing == null ? Maps.newArrayList() : existing;
            updated.addAll(registeredCommands);
            return updated;
        });

        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final var registrar = event.registrar();

            for (final SimpleCommand command : registeredCommands) {
                registrar.register(
                        command.getName(),
                        command.getDescription(),
                        List.of(command.getAliases()),
                        new SimpleCommandAdapter(command)
                );
            }
        });
    }

    @ApiStatus.Internal
    private static final class SimpleCommandAdapter implements BasicCommand {

        private final SimpleCommand command;

        private SimpleCommandAdapter(@NotNull SimpleCommand command) {
            this.command = command;
        }

        @Override
        public boolean canUse(@NotNull CommandSender sender) {
            return command.canExecute(sender);
        }

        @Override
        public void execute(
                @NotNull io.papermc.paper.command.brigadier.CommandSourceStack source,
                @NotNull String[] args
        ) {
            command.executeSafely(source.getSender(), ArgumentList.create(args));
        }

        @Override
        @NotNull
        public List<String> suggest(
                @NotNull io.papermc.paper.command.brigadier.CommandSourceStack source,
                @NotNull String[] args
        ) {
            return command.tabCompleteSafely(source.getSender(), ArgumentList.create(args));
        }
    }

    /**
     * Gets an immutable view of all the {@link SimpleCommand} registered by the given {@link Plugin}.
     *
     * @param plugin - The plugin.
     * @return an immutable view of all commands registered by the plugin.
     */
    @NotNull
    public static List<SimpleCommand> getCommands(@NotNull Plugin plugin) {
        final List<SimpleCommand> commands = COMMANDS_BY_PLUGIN.get(plugin);

        return commands != null ? List.copyOf(commands) : List.of();
    }
}
