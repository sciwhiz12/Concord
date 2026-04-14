/*
 * Concord - Copyright (c) 2020 SciWhiz12
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

package dev.sciwhiz12.concord.msg;

import dev.sciwhiz12.concord.ChatBot;
import dev.sciwhiz12.concord.ConcordConfig;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReference;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Objects;

public class MessageListener extends ListenerAdapter {
    private final ChatBot bot;

    public MessageListener(ChatBot bot) {
        this.bot = bot;
        bot.getDiscord().addEventListener(this);
        NeoForge.EVENT_BUS.register(this);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;

        if (event.getAuthor().getIdLong() == bot.getDiscord().getSelfUser().getIdLong()) return;
        if (event.isWebhookMessage() || event.getAuthor().isBot()) return; // TODO: maybe make this a config option

        if (event.getGuild().getIdLong() == MiscUtil.parseSnowflake(ConcordConfig.GUILD_ID.get()) &&
                event.getChannel().getIdLong() == MiscUtil.parseSnowflake(ConcordConfig.CHAT_CHANNEL_ID.get())) {

            // Currently, only events with non-null members ever get here
            final Member member = Objects.requireNonNull(event.getMember());
            final Message message = event.getMessage();
            final MessageReference reference = message.getMessageReference();
            if (reference != null) {
                reference.resolve().queue();
            }
            bot.messaging().sendToMinecraft(member, message);
        }
    }

    @SubscribeEvent
    void onServerTickPost(ServerTickEvent.Post event) {
        bot.messaging().processMessages();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    void onSubmittedServerChat(ServerChatEvent event) {
        bot.getChatForwarder().forward(event.getPlayer(), event.getMessage());
    }
}
