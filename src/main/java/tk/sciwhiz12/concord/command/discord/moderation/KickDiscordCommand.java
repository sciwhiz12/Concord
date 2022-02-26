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

package tk.sciwhiz12.concord.command.discord.moderation;

import java.time.Instant;
import java.util.List;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import net.minecraft.network.chat.TextComponent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import tk.sciwhiz12.concord.ChatBot;
import tk.sciwhiz12.concord.command.discord.ConcordSlashCommand;
import tk.sciwhiz12.concord.command.discord.checks.Checks;
import tk.sciwhiz12.concord.command.discord.checks.ChecksSet;

public final class KickDiscordCommand extends ConcordSlashCommand {

    public KickDiscordCommand(ChatBot bot) {
        super(bot);
        name = "kick";
        help = "Kicks an user from the Minecraft server.";
        checks = ChecksSet.DEFAULT
            .toBuilder()
            .and(Checks.ACCOUNTS_LINKED)
            .and(Checks.SERVER_OP)
            .and(validPlayerChecker("player"))
            .build();
        options = List.of(
                new OptionData(OptionType.STRING, "player", "The player to kick.").setRequired(true).setAutoComplete(true),
                new OptionData(OptionType.STRING, "reason", "The reason to kick.").setRequired(true)
            );
    }
    
    @Override
    protected void execute0(SlashCommandEvent event) {
        final var player = event.getOption("player", playerResolver);
        final var reason = event.getOption("reason", OptionMapping::getAsString);
        player.connection.disconnect(new TextComponent(reason));
        final var embed = new EmbedBuilder()
            .setColor(0xff0000)
            .setTitle("Player Kicked")
            .setDescription("%s kicked %s from the Minecraft server!".formatted(event.getUser().getAsMention(), player.getDisplayName().getString()))
            .addField("Reason", reason, false)
            .setTimestamp(Instant.now());
        event.deferReply().addEmbeds(embed.build()).queue();
    }
    
    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        bot.suggestPlayers(event, 5, e -> e.getFocusedOption().getName().equals("player"));
    }

}
