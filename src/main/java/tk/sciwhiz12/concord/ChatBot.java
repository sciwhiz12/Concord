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

package tk.sciwhiz12.concord;

import com.google.common.collect.Sets;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import tk.sciwhiz12.concord.msg.MessageListener;
import tk.sciwhiz12.concord.msg.Messaging;
import tk.sciwhiz12.concord.msg.PlayerListener;
import tk.sciwhiz12.concord.msg.StatusListener;

import java.util.Collections;
import java.util.EnumSet;

public class ChatBot extends ListenerAdapter {
    private static final Marker BOT = MarkerManager.getMarker("BOT");
    public static final EnumSet<Permission> REQUIRED_PERMISSIONS =
        EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);

    private final JDA discord;
    private final MinecraftServer server;
    private final MessageListener msgListener;
    private final PlayerListener playerListener;
    private final StatusListener statusListener;

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

        Messaging.sendToChannel(discord, new TranslatableComponent("message.concord.bot.start").getString());
    }

    void shutdown() {
        Concord.LOGGER.info(BOT, "Shutting down Discord bot...");
        MinecraftForge.EVENT_BUS.unregister(msgListener);
        MinecraftForge.EVENT_BUS.unregister(playerListener);
        MinecraftForge.EVENT_BUS.unregister(statusListener);
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

        final GuildChannel channel = guild.getGuildChannelById(ConcordConfig.CHANNEL_ID.get());
        if (channel == null) {
            Concord.LOGGER.error(BOT, "There is no channel with ID {} within the guild, as specified in the config.",
                ConcordConfig.CHANNEL_ID.get());
            return false;
        }

        if (channel.getType() != ChannelType.TEXT) {
            Concord.LOGGER.error(BOT, "The channel with ID {} is not a TEXT channel, it was of type {}.",
                ConcordConfig.CHANNEL_ID.get(), channel.getType());
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
