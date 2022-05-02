package tk.sciwhiz12.concord.command.discord;

import com.mojang.brigadier.Command;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import tk.sciwhiz12.concord.ConcordConfig;

import java.util.*;
import java.util.function.Consumer;

/**
 * Dispatches slash commands to the appropriate classes.
 * Serves as a wrapper around many individual ListenerAdapters.
 *
 * Register your commands to this dispatcher with <code>CommandDispatcher.register(...)</code>.
 * If the command is not already, it will be automatically upserted to all valid guilds.
 * You may set <code>.testGuild(...)</code> if you wish to only upsert to a single guild, but this is global.
 *
 * When the command is invoked, the parameter to the register call will be invoked, and passed down the SlashCommandEvent.
 *
 * Information such as parameters, help info, description and others will be automatically taken from the command parameter.
 *
 * @author Curle
 */
public class CommandDispatcher extends ListenerAdapter {

    // The list of valid commands, to be upserted and listened for.
    private List<SlashCommand> commands;

    // The Map that powers the command listener
    private Map<String, SlashCommand> commandsByName;

    // Whether this Dispatcher should only listen on a single guild, in a testing configuration.
    private boolean testMode;

    // The Guild to upsert commands to, if testMode is enabled
    @Nullable
    private Guild testGuild;

    /**
     * Create a new Command Dispatcher, pre-programmed with its' list of commands.
     * This should be the preferred way of creating a dispatcher.
     * @param commands the list of commands to listen for.
     */
    public CommandDispatcher(SlashCommand... commands) {
        this.commands = Arrays.stream(commands).toList();

        this.commandsByName = new HashMap<>();
        for (SlashCommand command : this.commands) {
            this.commandsByName.put(command.getName(), command);
        }

        this.testMode = false;
        this.testGuild = null;
    }

    /**
     * Create an empty CommandDispatcher.
     * Only useful if you want to primarily add commands via lambda using addSingle.
     * Please try not to use this too much.
     */
    public CommandDispatcher() {
        this.commands = new ArrayList<>();
        this.commandsByName = new HashMap<>();
        this.testMode = false;
        this.testGuild = null;
    }

    /**
     * A condensed way of adding a command, that doesn't require creating a new class.
     * Provide the necessary information along with a Consumer<SlashCommandEvent> and the rest will be handled for you.
     * @param commandName the name of the command to add
     * @param commandDescription description of the command to add
     * @param help extra information to be shown in the help command
     * @param consumer the action to perform when the command is invoked
     * @return the modified CommandDispatcher
     */
    public CommandDispatcher registerSingle(String commandName, String commandDescription, String help, Consumer<SlashCommandEvent> consumer) {
        var command = new SlashCommand()  {
            {
                setName(commandName);
                setDescription(commandDescription);
                setHelpString(help);
            }

            @Override
            public void execute(SlashCommandEvent event) {
                consumer.accept(event);
            }

            @Override
            public CommandCreateAction setup(CommandCreateAction action) {
                return action;
            }
        };

        this.commands.add(command);
        this.commandsByName.put(commandName, command);

        return this;
    }

    /**
     * A short way to add a single command to the dispatcher.
     * This is not the recommended way to do this - use the variadic constructor instead.
     * @param command the command to add
     * @return the new Dispatcher, for chaining.
     */
    public CommandDispatcher registerSingle(SlashCommand command) {
        this.commands.add(command);
        this.commandsByName.put(command.getName(), command);
        return this;
    }

    /**
     * Set this Dispatcher to a test mode, which will only upsert commands to the specified guild.
     * This effectively allows for immediate usage of specified commands, rather than the 1 hour delay in regular mode.
     * However, it is limited to a single guild in this mode, so do not use it for production usage.
     * @param guild the guild to upsert commands to.
     * @return The updated CommandDispatcher, for chaining
     */
    public CommandDispatcher testGuild(Guild guild) {
        this.testMode = true;
        this.testGuild = guild;

        return this;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        if (!ConcordConfig.GUILD_ID.get().isEmpty())
            this.testGuild(event.getJDA().getGuildById(ConcordConfig.GUILD_ID.get()));

        for (SlashCommand command : this.commands) {
            // If in test mode, upsert to the configured Test Guild, otherwise to the whole JDA instance.
            var action = this.testMode ?
                    this.testGuild.upsertCommand(command.getName(), command.getDescription()) :
                    event.getJDA().upsertCommand(command.getName(), command.getDescription());
            // Let the command set up extra information
            command.setup(action);
            // Queue the upsert
            action.queue();
        }
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        SlashCommand command = this.commandsByName.get(event.getName());

        // Sanity check
        if (command == null) throw new IllegalStateException("Attempted to invoke command " + event.getName() + " but it does not exist");

        // Dispatch to the registered command
        command.execute(event);
    }
}
