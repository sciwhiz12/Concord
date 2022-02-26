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

import java.util.function.Function;
import java.util.function.Predicate;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;

import net.minecraft.server.level.ServerPlayer;

import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.MiscUtil;
import tk.sciwhiz12.concord.ChatBot;
import tk.sciwhiz12.concord.ConcordConfig;
import tk.sciwhiz12.concord.command.discord.checks.ChecksSet;
import tk.sciwhiz12.concord.command.discord.checks.SlashCommandContext;

public abstract class ConcordSlashCommand extends SlashCommand {

    protected final ChatBot bot;
    protected ChecksSet checks = ChecksSet.DEFAULT;
    
    protected final Function<? super OptionMapping, ServerPlayer> playerResolver;
    
    protected ConcordSlashCommand(final ChatBot bot) {
        this.bot = bot;
        guildOnly = true;
        
        playerResolver = mapping -> {
            final var playerName = mapping.getAsString();
            return bot.getServer()
                    .getPlayerList()
                    .getPlayers()
                    .stream()
                    .filter(p -> p.getName().getString().equals(playerName))
                    .findAny()
                    .orElse(null);
        };
    }

    @Override
    protected final void execute(SlashCommandEvent event) {
        if (!event.isFromGuild()
                || event.getGuild().getIdLong() != MiscUtil.parseSnowflake(ConcordConfig.GUILD_ID.get())
                || event.getChannel().getIdLong() != MiscUtil.parseSnowflake(ConcordConfig.CHANNEL_ID.get())) {
            event.deferReply(true).setContent("This command cannot be used in this channel.").queue();
            return;
        }
        final var ctx = new SlashCommandContext(event, bot);
        if (!checks.test(ctx)) {
            return;
        }
        execute0(event);
    }
    
    protected abstract void execute0(SlashCommandEvent event);

    public static Predicate<SlashCommandContext> validPlayerChecker(String optionName) {
        return ctx -> {
            final var playerName = ctx.event().getOption(optionName, OptionMapping::getAsString);
            final var playerFound = ctx.bot().getServer()
                .getPlayerList()
                .getPlayers()
                .stream()
                .anyMatch(p -> p.getName().getString().equals(playerName));
            if (!playerFound) {
                ctx.deferReply(true).setContent("Unknown player **%s**!".formatted(playerName)).queue();
            }
            return playerFound;
        };
    }
    
}