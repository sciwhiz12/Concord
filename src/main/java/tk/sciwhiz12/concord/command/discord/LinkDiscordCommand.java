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

import java.util.List;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import tk.sciwhiz12.concord.ChatBot;
import tk.sciwhiz12.concord.util.RestActionWrapper;

public final class LinkDiscordCommand extends ConcordSlashCommand {

    public LinkDiscordCommand(ChatBot bot) {
        super(bot);
        name = "link";
        help = "Links your Discord account with your Minecraft account.";
        options = List.of(new OptionData(OptionType.INTEGER, "code", "The code to use for linking."));
    }

    @Override
    protected void execute0(SlashCommandEvent event) {
        final var code = event.getOption("code", OptionMapping::getAsInt);
        RestActionWrapper.of(event.deferReply(true))
            .flatMapIf(bot.getLinkedUsers().codeExists(code), hook -> {
                bot.getLinkedUsers().linkByCode(code, event.getUser().getIdLong());
                return hook.editOriginal("Successfully linked accounts!");
            }, hook -> hook.editOriginal("The code you provided is invalid, or has been used already!"))
            .queue();
    }

}