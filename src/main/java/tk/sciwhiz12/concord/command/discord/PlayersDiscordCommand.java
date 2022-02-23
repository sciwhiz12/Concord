package tk.sciwhiz12.concord.command.discord;

import java.time.Instant;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import tk.sciwhiz12.concord.ChatBot;
import tk.sciwhiz12.concord.util.Utils;

public final class PlayersDiscordCommand extends ConcordSlashCommand {

    public PlayersDiscordCommand(ChatBot bot) {
        super(bot);
        this.name = "players";
        this.help = "Shows the current players on the Minecraft server.";
    }

    @Override
    protected void execute0(SlashCommandEvent event) {
        final var embed = new EmbedBuilder()
                .setColor(Utils.generateRandomColour())
                .setTitle("Players online on Minecraft server")
                .setTimestamp(Instant.now());
        
        bot.getServer().getPlayerList().getPlayers().forEach(player -> {
            embed.appendDescription(player.getName().getString());
            if (bot.getServer().getPlayerList().isOp(player.getGameProfile())) {
                embed.appendDescription(" - **OP**");
            }
            embed.appendDescription(System.lineSeparator());
        });
        
        event.deferReply().addEmbeds(embed.build()).queue();
    }

}
