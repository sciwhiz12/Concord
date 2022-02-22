package tk.sciwhiz12.concord;

import static tk.sciwhiz12.concord.Concord.LOGGER;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import com.google.common.collect.Sets;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.emote.EmoteAddedEvent;
import net.dv8tion.jda.api.events.emote.EmoteRemovedEvent;
import net.dv8tion.jda.api.events.emote.update.EmoteUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.minecraftforge.common.MinecraftForge;
import tk.sciwhiz12.concord.msg.MessageListener;
import tk.sciwhiz12.concord.msg.Messaging;
import tk.sciwhiz12.concord.msg.PlayerListener;
import tk.sciwhiz12.concord.msg.StatusListener;
import tk.sciwhiz12.concord.network.ConcordNetwork;
import tk.sciwhiz12.concord.network.RegisterEmotePacket;
import tk.sciwhiz12.concord.network.RemoveEmotePacket;
import tk.sciwhiz12.concord.network.RegisterEmotePacket.EmoteData;

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

		LOGGER.debug(BOT, "Checking guild and channel existence, and satisfaction of required permissions...");
        // Required permissions are there. All checks satisfied.
        if (!checkSatisfaction()) {
			LOGGER.warn(BOT, "Some checks were not satisfied; disabling Discord integration.");
            Concord.disable();
            return;
        }
		LOGGER.debug(BOT, "Guild and channel are correct, and permissions are satisfied.");

		LOGGER.info(BOT, "Discord bot is ready!");

        Messaging.sendToChannel(discord, new TranslatableComponent("message.concord.bot.start").getString());
    }

	@Override
	public void onGuildReady(GuildReadyEvent event) {
		event.getGuild().getEmotes().forEach(Messaging::addEmojiReplacement);
	}

	@Override
	public void onEmoteAdded(EmoteAddedEvent event) {
		Messaging.addEmojiReplacement(event.getEmote());
		
		ConcordNetwork.sendToAllInServer(ConcordNetwork.EMOJIFUL_CHANNEL, server, 
				new RegisterEmotePacket(Map.of(event.getGuild().getName(), 
						List.of(new EmoteData(event.getEmote())))));
	}

	@Override
	public void onEmoteRemoved(EmoteRemovedEvent event) {
		Messaging.removeEmojiReplacement(event.getEmote());
		
		ConcordNetwork.sendToAllInServer(ConcordNetwork.EMOJIFUL_CHANNEL, server, 
				new RemoveEmotePacket(Map.of(event.getGuild().getName(), 
						List.of(new EmoteData(event.getEmote())))));
	}

	@Override
	public void onEmoteUpdateName(EmoteUpdateNameEvent event) {
		Messaging.removeEmojiReplacement(event.getOldName());
		Messaging.addEmojiReplacement(event.getEmote());
		
		ConcordNetwork.sendToAllInServer(ConcordNetwork.EMOJIFUL_CHANNEL, server, 
				new RemoveEmotePacket(Map.of(event.getGuild().getName(), 
						List.of(new EmoteData(event.getEmote())))));
		
		ConcordNetwork.sendToAllInServer(ConcordNetwork.EMOJIFUL_CHANNEL, server, 
				new RegisterEmotePacket(Map.of(event.getGuild().getName(), 
						List.of(new EmoteData(event.getEmote())))));
	}

    void shutdown() {
		LOGGER.info(BOT, "Shutting down Discord bot...");
        MinecraftForge.EVENT_BUS.unregister(msgListener);
        MinecraftForge.EVENT_BUS.unregister(playerListener);
        MinecraftForge.EVENT_BUS.unregister(statusListener);
        discord.shutdown();
    }

    boolean checkSatisfaction() {
        // Checking if specified guild and channel IDs are correct
        final Guild guild = discord.getGuildById(ConcordConfig.GUILD_ID.get());
        if (guild == null) {
			LOGGER.warn(BOT, "This bot is not connected to a guild with ID {}, as specified in the config.",
                ConcordConfig.GUILD_ID.get());
			LOGGER.warn(BOT,
					"This indicates either the bot was not invited to the guild, or a wrongly-typed guild ID.");
            return false;
        }

        final GuildChannel channel = guild.getGuildChannelById(ConcordConfig.CHANNEL_ID.get());
        if (channel == null) {
			LOGGER.error(BOT, "There is no channel with ID {} within the guild, as specified in the config.",
                ConcordConfig.CHANNEL_ID.get());
            return false;
        }

        if (channel.getType() != ChannelType.TEXT) {
			LOGGER.error(BOT, "The channel with ID {} is not a TEXT channel, it was of type {}.",
                ConcordConfig.CHANNEL_ID.get(), channel.getType());
            return false;
        }

        // Guild and channel IDs are correct, now to check permissions
        final Sets.SetView<Permission> missingPermissions = Sets
            .difference(REQUIRED_PERMISSIONS, guild.getSelfMember().getPermissions(channel));

        if (!missingPermissions.isEmpty()) {
			LOGGER.error(BOT, "This bot is missing the following required permissions in the channel: {}.",
                missingPermissions);
			LOGGER.error(BOT, "As reference, the bot requires the following permissions in the channel: {}.",
                REQUIRED_PERMISSIONS);
            return false;
        }

        // Required permissions are there. All checks satisfied.
        return true;
    }
}
