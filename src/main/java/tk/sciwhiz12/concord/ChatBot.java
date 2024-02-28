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

package tk.sciwhiz12.concord;

import com.google.common.collect.Sets;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.WebhookClient;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import tk.sciwhiz12.concord.msg.*;
import tk.sciwhiz12.concord.msg.chat.ChatForwarder;
import tk.sciwhiz12.concord.msg.chat.DefaultChatForwarder;
import tk.sciwhiz12.concord.msg.chat.WebhookChatForwarder;
import tk.sciwhiz12.concord.util.Messages;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.regex.Matcher;

public class ChatBot extends ListenerAdapter {
    private static final Marker BOT = MarkerFactory.getMarker("BOT");
    public static final EnumSet<Permission> REQUIRED_PERMISSIONS =
            EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND);

    private final JDA discord;
    private final MinecraftServer server;
    private final Messaging messaging;
    private final MessageListener msgListener;
    private final PlayerListener playerListener;
    private final StatusListener statusListener;
    private final SentMessageMemory sentMessageMemory;
    private ChatForwarder chatForwarder;

    ChatBot(JDA discord, MinecraftServer server) {
        this.discord = discord;
        this.server = server;
        discord.addEventListener(this);
        msgListener = new MessageListener(this);
        messaging = new Messaging(this);
        playerListener = new PlayerListener(this);
        statusListener = new StatusListener(this);
        sentMessageMemory = new SentMessageMemory(this);
        chatForwarder = new DefaultChatForwarder(this);

        // Prevent any mentions not explicitly specified
        MessageRequest.setDefaultMentions(Collections.emptySet());
    }

    public JDA getDiscord() {
        return discord;
    }

    public MinecraftServer getServer() {
        return server;
    }

    @Override
    public void onReady(ReadyEvent event) {
        discord.getPresence().setPresence(OnlineStatus.ONLINE, Activity.playing("some Minecraft"));

        Concord.LOGGER.debug(BOT, "Checking guild and channel existence, and satisfaction of required permissions...");
        // Required permissions are there. All checks satisfied.
        if (!checkSatisfaction()) {
            Concord.LOGGER.warn(BOT, "Some checks were not satisfied; disabling Discord integration.");
            Concord.disable();
            return;
        }
        Concord.LOGGER.debug(BOT, "Guild and channel are correct, and permissions are satisfied.");

        final String webhookID = ConcordConfig.RELAY_WEBHOOK.get();
        if (webhookID != null && !webhookID.isEmpty()) {

            @Nullable final String avatarUrl;
            final String configuredAvatarUrl = ConcordConfig.WEBHOOK_AVATAR_URL.get();
            if (configuredAvatarUrl != null && !configuredAvatarUrl.isEmpty()) {
                avatarUrl = configuredAvatarUrl;
                Concord.LOGGER.debug("Using configured webhook avatar URL: {}", avatarUrl);
            } else {
                avatarUrl = null; 
            }

            final Matcher urlMatcher = Webhook.WEBHOOK_URL.matcher(webhookID);
            if (urlMatcher.find()) {
                chatForwarder = new WebhookChatForwarder(this, WebhookClient.createClient(discord, webhookID), avatarUrl);

                Concord.LOGGER.info(BOT, "Enabled webhook chat forwarder, using webhook with ID {}", urlMatcher.group("id"));
            } else {
                discord.retrieveWebhookById(webhookID).queue(webhook -> {
                    
                    chatForwarder = new WebhookChatForwarder(this, webhook, avatarUrl);

                    Concord.LOGGER.info(BOT, "Enabled webhook chat forwarder, using webhook with ID {}", webhookID);
                }, error -> new ErrorHandler(err -> Concord.LOGGER.error(BOT, "Failed to enable webhook chat forwarder for an unknown reason!", err))
                        .handle(ErrorResponse.UNKNOWN_WEBHOOK, err ->
                                Concord.LOGGER.error(BOT, "Failed to enable webhook chat forwarder as webhook does not exist!", err))
                        .handle(ErrorResponse.MISSING_PERMISSIONS, err ->
                                Concord.LOGGER.error(BOT, "Failed to enable webhook chat forwarder as bot is missing permissions!", err)));
            }
        }

        Concord.LOGGER.info(BOT, "Discord bot is ready!");

        if (ConcordConfig.BOT_START.get()) {
            Messaging.sendToChannel(discord, Messages.BOT_START.component().getString());
        }
    }

    void shutdown() {
        Concord.LOGGER.info(BOT, "Shutting down Discord bot...");
        NeoForge.EVENT_BUS.unregister(msgListener);
        NeoForge.EVENT_BUS.unregister(playerListener);
        NeoForge.EVENT_BUS.unregister(statusListener);
        discord.shutdown();
    }

    boolean checkSatisfaction() {
        // Checking if specified guild and channel IDs are correct
        final Guild guild = discord.getGuildById(ConcordConfig.GUILD_ID.get());
        if (guild == null) {
            Concord.LOGGER.warn(BOT, "This bot is not connected to a guild with ID {}, as specified in the config.",
                    ConcordConfig.GUILD_ID.get());
            Concord.LOGGER.warn(BOT, "This indicates either the bot was not invited to the guild, or a wrongly-typed guild ID.");
            return false;
        }

        final GuildChannel channel = guild.getGuildChannelById(ConcordConfig.CHAT_CHANNEL_ID.get());
        if (channel == null) {
            Concord.LOGGER.error(BOT, "There is no channel with ID {} within the guild, as specified in the config.",
                    ConcordConfig.CHAT_CHANNEL_ID.get());
            return false;
        }

        if (channel.getType() != ChannelType.TEXT) {
            Concord.LOGGER.error(BOT, "The channel with ID {} is not a TEXT channel, it was of type {}.",
                    ConcordConfig.CHAT_CHANNEL_ID.get(), channel.getType());
            return false;
        }

        // Guild and channel IDs are correct, now to check permissions
        final Sets.SetView<Permission> missingPermissions = Sets
                .difference(REQUIRED_PERMISSIONS, guild.getSelfMember().getPermissions(channel));

        if (!missingPermissions.isEmpty()) {
            Concord.LOGGER.error(BOT, "This bot is missing the following required permissions in the channel: {}.",
                    missingPermissions);
            Concord.LOGGER.error(BOT, "As reference, the bot requires the following permissions in the channel: {}.",
                    REQUIRED_PERMISSIONS);
            return false;
        }

        // Required permissions are there. All checks satisfied.
        return true;
    }

    public Messaging messaging() {
        return messaging;
    }

    public SentMessageMemory getSentMessageMemory() {
        return sentMessageMemory;
    }

    public ChatForwarder getChatForwarder() {
        return chatForwarder;
    }
}
