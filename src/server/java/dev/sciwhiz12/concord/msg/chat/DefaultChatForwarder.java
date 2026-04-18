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

package dev.sciwhiz12.concord.msg.chat;

import dev.sciwhiz12.concord.ChatBot;
import dev.sciwhiz12.concord.ConcordConfig;
import dev.sciwhiz12.concord.ConcordServer;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

import static dev.sciwhiz12.concord.Concord.LOGGER;

public class DefaultChatForwarder implements ChatForwarder {
    private final ChatBot bot;

    public DefaultChatForwarder(ChatBot bot) {
        this.bot = bot;
    }

    @Override
    public CompletableFuture<Message> forwardPlayerMessage(ServerPlayer player, Component message) {
        return forwardSystemMessage(Component.translatable("chat.type.text", player.getDisplayName(), message).getString());
    }

    @Override
    public CompletableFuture<Message> forwardSystemMessage(String message) {
        final TextChannel channel = bot.getDiscord().getTextChannelById(ConcordConfig.CHAT_CHANNEL_ID.get());
        if (channel != null) {
            return channel.sendMessage(message)
                    .setAllowedMentions(bot.messaging().getAllowedMentions())
                    .submit();
        } else {
            LOGGER.error("Failed to retrieve chat channel from JDA channel cache; was the channel deleted?");
            ConcordServer.disable(true);
            return CompletableFuture.failedFuture(new RuntimeException("Failed to retrieve chat channel from JDA channel cache"));
        }
    }
}
