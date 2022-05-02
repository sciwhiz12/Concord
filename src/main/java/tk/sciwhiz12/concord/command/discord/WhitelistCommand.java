package tk.sciwhiz12.concord.command.discord;

import com.mojang.authlib.GameProfile;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import tk.sciwhiz12.concord.Concord;

import java.util.Optional;


/**
 * This command takes the form:
 *  /whitelist <add|remove> <user>
 *
 * Depending on the second term, it will add or remove the specified user from the server whitelist.
 * This command is disabled on integrated servers, even if enable_integrated is specified.
 *
 * @author Curle
 */
public class WhitelistCommand extends SlashCommand {
    private static final OptionData USER_OPTION = new OptionData(OptionType.STRING, "user", "The user to change", true);
    private static final SubcommandData ADD_SUBCOMMAND = new SubcommandData("add", "Add a user to the whitelist").addOptions(USER_OPTION);
    private static final SubcommandData REMOVE_SUBCOMMAND = new SubcommandData("remove", "Remove a user from the whitelist").addOptions(USER_OPTION);

    public static WhitelistCommand INSTANCE = new WhitelistCommand();

    public WhitelistCommand() {
        setName("whitelist");
        setDescription("Add or remove a player from the server's whitelist.");
        setHelpString("Contains two subcommands; add and remove. Each takes a user argument and will add or remove the player from the whitelist respectively.");
    }

    @Override
    public void execute(SlashCommandEvent event) {
        var server = Concord.BOT.getServer();

        // Short circuit for singleplayer worlds
        if (FMLLoader.getDist() == Dist.CLIENT) {
            event.reply("Sorry, but this command is disabled on Integrated Servers").setEphemeral(true).queue();
            return;
        }

        // Figure out which subcommand we're running
        var subcommand = event.getSubcommandName();
        switch (subcommand) {
            case "add":
                var player = event.getOption(USER_OPTION.getName()).getAsString();
                var whitelist = server.getPlayerList().getWhiteList();
                Optional<GameProfile> optional = server.getProfileCache().get(player);
                var profile = optional.orElseThrow();

                if (!whitelist.isWhiteListed(profile)) {
                    UserWhiteListEntry userwhitelistentry = new UserWhiteListEntry(profile);
                    whitelist.add(userwhitelistentry);

                    event.reply("User " + player + " successfully added to the whitelist.").setEphemeral(true).queue();
                    return;
                }

                event.reply("User " + player + " is already whitelisted.").setEphemeral(true).queue();
                return;
            case "remove":
                player = event.getOption(USER_OPTION.getName()).getAsString();
                whitelist = server.getPlayerList().getWhiteList();
                optional = server.getProfileCache().get(player);
                profile = optional.orElseThrow();

                if (whitelist.isWhiteListed(profile)) {
                    whitelist.remove(profile);

                    event.reply("User " + player + " successfully removed from the whitelist.").setEphemeral(true).queue();
                    return;
                }

                event.reply("User " + player + " is not whitelisted.").setEphemeral(true).queue();
                return;
        }

        // No recognized subcommand. Fall through to a safe default.
        event.reply("Unrecognized subcommand.").setEphemeral(true).queue();

    }

    @Override
    public CommandCreateAction setup(CommandCreateAction action) {
        return action.addSubcommands(ADD_SUBCOMMAND).addSubcommands(REMOVE_SUBCOMMAND);
    }
}
