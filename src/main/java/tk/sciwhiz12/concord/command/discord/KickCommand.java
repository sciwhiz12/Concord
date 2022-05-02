package tk.sciwhiz12.concord.command.discord;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.TextComponent;
import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.ConcordConfig;

import java.util.List;

/**
 * This command takes the form:
 *  /kick <user> [reason]
 *
 * It removes a user from the server, optionally with the specified reason.
 *
 * @author Curle
 */
public class KickCommand extends SlashCommand {
    private static final OptionData USER_OPTION = new OptionData(OptionType.STRING, "user", "The username of the Minecraft user to kick from the server", true);
    private static final OptionData REASON_OPTION = new OptionData(OptionType.STRING, "reason", "Why the user is being kicked from the server.", false);

    // Static instance.
    public static KickCommand INSTANCE = new KickCommand();

    public KickCommand() {
        setName("kick");
        setDescription("Kick a user from your Minecraft server");
        setHelpString("Remove a user from the server, optionally with a reason. The reason will be shown to the user in the disconnection screen.");
    }

    @Override
    public void execute(SlashCommandEvent event) {
        var user = event.getOption(USER_OPTION.getName()).getAsString();
        var server = Concord.BOT.getServer();

        // Short-circuit for integrated servers.
        if (!ConcordConfig.ENABLE_INTEGRATED.get() && server instanceof IntegratedServer) {
            event.reply("Sorry, but this command is disabled on Integrated Servers. Check the enable_integrated option in the Concord Config.").setEphemeral(true).queue();
            return;
        }

        var reasonMapping = event.getOption(REASON_OPTION.getName());

        // The Reason Option is optional, so default to "Reason Not Specified" if it isn't.
        var reason = "";
        if (reasonMapping == null)
            reason = "Reason Not Specified";
        else
            reason = reasonMapping.getAsString();

        // Check whether the user is online
        if (List.of(server.getPlayerNames()).contains(user)) {
            var player = server.getPlayerList().getPlayerByName(user);
            // If they are, kick them with the message.
            player.connection.disconnect(new TextComponent(reason));

            // Reply to the user.
            event.reply("User " + user + " kicked successfully.").queue();
            return;
        }

        // Reply with a failure message.
        event.reply("The user " + user + " is not connected to the server.").queue();
    }

    @Override
    public CommandCreateAction setup(CommandCreateAction action) {
        return action.addOptions(USER_OPTION, REASON_OPTION);
    }
}
