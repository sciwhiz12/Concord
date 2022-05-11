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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;

import javax.annotation.Nullable;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;

import net.minecraft.server.MinecraftServer;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.minecraftforge.common.MinecraftForge;
import tk.sciwhiz12.concord.msg.MessageListener;
import tk.sciwhiz12.concord.msg.Messaging;
import tk.sciwhiz12.concord.msg.PlayerListener;
import tk.sciwhiz12.concord.msg.StatusListener;
import tk.sciwhiz12.concord.util.Messages;

public class ChatBot extends ListenerAdapter {
    private static final Marker BOT = MarkerFactory.getMarker("BOT");
    public static final EnumSet<Permission> REQUIRED_PERMISSIONS =
            EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);
    public static final String MINECRAFT_ICON_URL = "https://www.minecraft.net/etc.clientlibs/minecraft/clientlibs/main/resources/favicon-96x96.png";
    public static final String HEAD_URL = "https://crafatar.com/renders/head/%s?default=MHF_Steve&size=128";
    
    private final JDA discord;
    private final MinecraftServer server;
    private final MessageListener msgListener;
    private final PlayerListener playerListener;
    private final StatusListener statusListener;

    private JDAWebhookClient webhook;

    ChatBot(JDA discord, MinecraftServer server) {
        this.discord = discord;
        this.server = server;
        discord.addEventListener(this);
        msgListener = new MessageListener(this);
        playerListener = new PlayerListener(this);
        statusListener = new StatusListener(this);

        // Prevent any mentions not explicitly specified
        MessageAction.setDefaultMentions(Collections.emptySet());
    }

    public JDA getDiscord() {
        return discord;
    }

    public MinecraftServer getServer() {
        return server;
    }

    @Nullable
    public JDAWebhookClient getWebhook() {
        return webhook;
    }
    
    public void sendMessage(CharSequence content, @Nullable GameProfile sender, Collection<Message.MentionType> allowedMentions) {
        sendMessage(content, sender, true, allowedMentions);
    }

    /**
     * Sends a message in the channel of the bot. That message can either be sent by the bot, or by a webhook, depending on configuration.
     * @param content the content of the message to send
     * @param sender the sender of the message. Maybe be null
     * @param addSender if the code {@code sender} is not null, if {@code true} and if a webhook isn't configured, the bot will send the message using its account, but prefixed with {@code <senderName>}
     * @param allowedMentions the mentions that should be allowed to be sent by the message
     */
    public void sendMessage(CharSequence content, @Nullable GameProfile sender, boolean addSender, Collection<Message.MentionType> allowedMentions) {
        if (webhook == null) {
            final var webhookUrl = ConcordConfig.WEBHOOK_URL.get();
            final var textChannel = discord.getTextChannelById(ConcordConfig.CHAT_CHANNEL_ID.get());
            if (!webhookUrl.isBlank()) {
                if (webhookUrl.toLowerCase(Locale.ROOT).equals(ConcordConfig.GENERATE_WEBHOOK_KEY)) {
                    if (textChannel != null) {
                        try {
                            textChannel.createWebhook("Minecraft")
                                // TODO the line below seems to throw an exception: java.net.SocketException: A
                                // connection attempt failed because the connected party did not properly
                                // respond after a period of time, or established connection failed because
                                // connected host has failed to respond:

                                // .setAvatar(Icon.from(new URL(MINECRAFT_ICON_URL).openStream(), IconType.PNG))
                                .queue(web -> {
                                    webhook = WebhookClientBuilder.fromJDA(web).setHttpClient(discord.getHttpClient())
                                        .buildJDA();

                                    ConcordConfig.WEBHOOK_URL.set(web.getUrl());

                                    webhook.send(buildWebhookMessage(content, sender, allowedMentions).build());
                                });
                        } catch (Exception e) {
                            Concord.LOGGER.error(BOT, "Exception trying to setup webhook in channel with ID {}: ",
                                textChannel.getId(), e);
                        }
                    }
                } else {
                    webhook = new WebhookClientBuilder(webhookUrl).setHttpClient(discord.getHttpClient()).buildJDA();

                    webhook.send(buildWebhookMessage(content, sender, allowedMentions).build());
                }
            } else
                sendMessageInChannel(textChannel, addSender, content, sender, allowedMentions);
        } else {
            webhook.send(buildWebhookMessage(content, sender, allowedMentions).build());
        }
    }
    
    private static WebhookMessageBuilder buildWebhookMessage(CharSequence content, @Nullable GameProfile sender, Collection<Message.MentionType> allowedMentions) {
        final var mentions = AllowedMentions.none();
        if (allowedMentions.contains(MentionType.EVERYONE)) {
            mentions.withParseEveryone(true);
        }
        if (allowedMentions.contains(MentionType.USER)) {
            mentions.withParseUsers(true);
        } 
        if (allowedMentions.contains(MentionType.ROLE)) {
            mentions.withParseRoles(true);
        }
        
        final var builder = new WebhookMessageBuilder();
        if (sender != null) {
            if (ConcordConfig.SEND_AS_PLAYER.get()) {
                builder.setAvatarUrl(HEAD_URL.formatted(sender.getId()))
                    .setUsername(sender.getName());
            } else {
                content = "<" + sender.getName() + ">";
            }
        }
        
        builder.setContent(content.toString())
            .setAllowedMentions(mentions);
        return builder;
    }

    private static void sendMessageInChannel(@Nullable TextChannel channel, boolean addSender, CharSequence content, @Nullable GameProfile sender, Collection<Message.MentionType> allowedMentions) {
        if (channel != null) {
            if (sender != null) {
                content = "<" + sender.getName() + ">";
            }
            channel.sendMessage(new MessageBuilder().append(content).setAllowedMentions(allowedMentions).build()).queue();
        }
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

        Concord.LOGGER.info(BOT, "Discord bot is ready!");

        Messaging.sendToChannel(this, Messages.BOT_START.component().getString(), null);
    }

    void shutdown() {
        Concord.LOGGER.info(BOT, "Shutting down Discord bot...");
        MinecraftForge.EVENT_BUS.unregister(msgListener);
        MinecraftForge.EVENT_BUS.unregister(playerListener);
        MinecraftForge.EVENT_BUS.unregister(statusListener);
        discord.shutdown();
        if (webhook != null) {
            webhook.close();
        }
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
}
