/*
 * Concord - Copyright (c) 2020-2022 SciWhiz12
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
