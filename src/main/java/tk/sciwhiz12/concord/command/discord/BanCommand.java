package tk.sciwhiz12.concord.command.discord;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.ConcordConfig;

import java.util.Date;
import java.util.List;


/**
 * This command takes the form:
 *  /ban <user> [reason]
 *
 * It removes a user from the server and prevents them from joining again (known as a ban, or kickban),
 *  optionally with the specified reason.
 *
 * @author Curle
 */
public class BanCommand extends SlashCommand {
    private static final OptionData USER_OPTION = new OptionData(OptionType.STRING, "user", "The name of the user to ban from the server", true);
    private static final OptionData REASON_OPTION = new OptionData(OptionType.STRING, "reason", "The reason for the user to be banned.", false);

    public static BanCommand INSTANCE = new BanCommand();

    public BanCommand() {
        setName("ban");
        setDescription("Ban a player from your Minecraft server");
        setHelpString("Remove a player from the server, and prevent them from joining again, optionally with a reason. The reason is for moderation purposes, and is not shown to the user.");
    }

    @Override
    public void execute(SlashCommandEvent event) {
        var roleConfig = ConcordConfig.MODERATOR_ROLE_ID.get();
        if (!roleConfig.isEmpty()) {
            var role = Concord.BOT.getDiscord().getRoleById(roleConfig);
            // If no role, then it's non-empty and invalid; disable the command
            if (role == null) {
                event.reply("Sorry, but this command is disabled by configuration. Check the moderator_role_id option in the config.").setEphemeral(true).queue();
                return;
            } else {
                // If the member doesn't have the moderator role, then deny them the ability to use the command.
                if (!event.getMember().getRoles().contains(role)) {
                    event.reply("Sorry, but you don't have permission to use this command.").setEphemeral(true).queue();
                    return;
                }
                // Fall-through; member has the role, so they can use the command.
            }
            // Fall-through; the role is empty, so all permissions are handled by Discord.
        }

        var user = event.getOption(USER_OPTION.getName()).getAsString();
        var server = Concord.BOT.getServer();


        // Short-circuit for integrated servers.
        if (!ConcordConfig.ENABLE_INTEGRATED.get() && FMLLoader.getDist() == Dist.CLIENT) {
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


        if (List.of(server.getPlayerNames()).contains(user)) {
            var player = server.getPlayerList().getPlayerByName(user);
            var profile = player.getGameProfile();

            // If they're not already banned..
            if (!server.getPlayerList().getBans().isBanned(profile)) {
                // Prevent them from rejoining
                UserBanListEntry userbanlistentry = new UserBanListEntry(profile, (Date) null, "Discord User " + event.getMember().getEffectiveName(), (Date) null, reason);
                server.getPlayerList().getBans().add(userbanlistentry);
                // Kick them
                player.connection.disconnect(Component.translatable("multiplayer.disconnect.banned"));

                event.reply("User " + user + " banned successfully.").queue();
                return;
            }
            event.reply("The user " + user + " is already banned on this server.").setEphemeral(true).queue();
            return;
        }
        event.reply("The user " + user + " is not connected to the server.").setEphemeral(true).queue();
    }

    @Override
    public CommandCreateAction setup(CommandCreateAction action) {
        return action.addOptions(USER_OPTION, REASON_OPTION);
    }
}
