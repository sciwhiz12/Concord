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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.WebhookClient;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;

public class WebhookChatForwarder implements ChatForwarder {
    private final ChatBot bot;
    private final WebhookClient<Message> client;
    @Nullable
    private final String avatarUrl;

    public WebhookChatForwarder(ChatBot bot, WebhookClient<Message> client, @Nullable String avatarUrl) {
        this.bot = bot;
        this.client = client;
        this.avatarUrl = avatarUrl;
    }

    @Override
    public void forward(ServerPlayer player, Component message) {
        WebhookMessageCreateAction<Message> action = client.sendMessage(message.getString())
                .setTTS(false)
                .setUsername(player.getDisplayName().getString())
                .setAllowedMentions(getAllowedMentions());

        if (avatarUrl != null) {
            final String playerUUID = player.getStringUUID();
            final String playerAvatarUrl = avatarUrl
                    .replace("{uuid}", playerUUID.replace("-", ""))
                    .replace("{uuid-dash}", playerUUID)
                    .replace("{username}", player.getGameProfile().getName());

            action = action.setAvatarUrl(playerAvatarUrl);
        }

        action.queue(sentMessage ->
                bot.getSentMessageMemory().rememberMessage(sentMessage.getIdLong(), player.getGameProfile(), message));
    }

    private Set<Message.MentionType> getAllowedMentions() {
        if (ConcordConfig.ALLOW_MENTIONS.get()) {
            final Set<Message.MentionType> mentions = EnumSet.noneOf(Message.MentionType.class);
            if (ConcordConfig.ALLOW_PUBLIC_MENTIONS.get()) {
                mentions.add(Message.MentionType.EVERYONE);
                mentions.add(Message.MentionType.HERE);
            }
            if (ConcordConfig.ALLOW_USER_MENTIONS.get()) {
                mentions.add(Message.MentionType.USER);
            }
            if (ConcordConfig.ALLOW_ROLE_MENTIONS.get()) {
                mentions.add(Message.MentionType.ROLE);
            }
            return mentions;
        }
        return Set.of();
    }
}
