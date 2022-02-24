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

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;

import net.dv8tion.jda.api.utils.MiscUtil;
import tk.sciwhiz12.concord.ChatBot;
import tk.sciwhiz12.concord.ConcordConfig;

public abstract class ConcordSlashCommand extends SlashCommand {

    protected final ChatBot bot;

    protected ConcordSlashCommand(final ChatBot bot) {
        this.bot = bot;
        guildOnly = true;
    }

    @Override
    protected final void execute(SlashCommandEvent event) {
        if (!event.isFromGuild()
                || event.getGuild().getIdLong() != MiscUtil.parseSnowflake(ConcordConfig.GUILD_ID.get())
                || event.getChannel().getIdLong() != MiscUtil.parseSnowflake(ConcordConfig.CHANNEL_ID.get())) {
            event.deferReply(true).setContent("This command cannot be used in this channel.").queue();
            return;
        }
        if (ConcordConfig.DISCORD_COMMANDS_ENABLED.containsKey(name) && !ConcordConfig.DISCORD_COMMANDS_ENABLED.get(name).get()) {
            event.deferReply(true).setContent("This command is disabled!").queue();
            return;
        }
        execute0(event);
    }

    protected abstract void execute0(SlashCommandEvent event);

}