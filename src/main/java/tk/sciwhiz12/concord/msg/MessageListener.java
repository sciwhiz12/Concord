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

package tk.sciwhiz12.concord.msg;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReference;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import tk.sciwhiz12.concord.ChatBot;
import tk.sciwhiz12.concord.ConcordConfig;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageListener extends ListenerAdapter {
    // Using a concurrent queue because messages is received on a different thread from the main server thread.
    private final Queue<MessageEntry> queuedMessages = new ConcurrentLinkedQueue<>();
    private final ChatBot bot;

    public MessageListener(ChatBot bot) {
        this.bot = bot;
        bot.getDiscord().addEventListener(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getAuthor().getIdLong() == bot.getDiscord().getSelfUser().getIdLong()) return;
        if (event.isWebhookMessage() || event.getAuthor().isBot()) return; // TODO: maybe make this a config option

        if (event.getGuild().getIdLong() == MiscUtil.parseSnowflake(ConcordConfig.GUILD_ID.get()) &&
                event.getChannel().getIdLong() == MiscUtil.parseSnowflake(ConcordConfig.CHAT_CHANNEL_ID.get())) {

            final MessageEntry entry = new MessageEntry(event);
            final MessageReference reference = entry.message.getMessageReference();
            if (reference != null) {
                reference.resolve().queue();
            }
            queuedMessages.add(entry);
        }
    }

    @SubscribeEvent
    void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MessageEntry entry;
        while ((entry = queuedMessages.poll()) != null) { // TODO: rate-limiting
            Messaging.sendToAllPlayers(bot.getServer(), entry.member, entry.message);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    void onServerChat(ServerChatEvent event) {
        Messaging.sendToChannel(bot.getDiscord(), event.getComponent().getString());
    }

    static record MessageEntry(Member member, Message message) {
        MessageEntry(GuildMessageReceivedEvent event) {
            // Currently, only events with non-null members ever get here
            this(Objects.requireNonNull(event.getMember()), event.getMessage());
        }
    }
}
