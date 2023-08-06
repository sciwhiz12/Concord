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

package tk.sciwhiz12.concord.msg.chat;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import tk.sciwhiz12.concord.ConcordConfig;

public class WebhookChatForwarder implements ChatForwarder {
    private final WebhookClient client;

    public WebhookChatForwarder(WebhookClient client) {
        this.client = client;
    }

    public WebhookChatForwarder(WebhookClientBuilder builder) {
        this(builder.setDaemon(true).build());
    }

    @Override
    public void forward(ServerPlayer player, Component message) {
        final WebhookMessageBuilder builder = new WebhookMessageBuilder()
                .append(message.getString())
                .setTTS(false)
                .setUsername(player.getDisplayName().getString())
                .setAllowedMentions(getAllowedMentions());

        client.send(builder.build());
    }

    private AllowedMentions getAllowedMentions() {
        if (ConcordConfig.ALLOW_MENTIONS.get()) {
            return AllowedMentions.none()
                    .withParseEveryone(ConcordConfig.ALLOW_PUBLIC_MENTIONS.get())
                    .withParseUsers(ConcordConfig.ALLOW_USER_MENTIONS.get())
                    .withParseRoles(ConcordConfig.ALLOW_ROLE_MENTIONS.get());
        }
        return AllowedMentions.none();
    }
}
