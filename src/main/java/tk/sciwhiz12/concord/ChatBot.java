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
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import tk.sciwhiz12.concord.msg.MessageListener;
import tk.sciwhiz12.concord.msg.Messaging;
import tk.sciwhiz12.concord.msg.PlayerListener;
import tk.sciwhiz12.concord.msg.StatusListener;

import java.util.EnumSet;

public class ChatBot extends ListenerAdapter {
    private static final Marker BOT = MarkerManager.getMarker("BOT");
    public static final EnumSet<Permission> REQUIRED_PERMISSIONS =
        EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);

    private final JDA discord;
    private final MessageListener msgListener;
    private final PlayerListener playerListener;
    private final StatusListener statusListener;

    ChatBot(JDA discord) {
        this.discord = discord;
        discord.addEventListener(this);
        msgListener = new MessageListener(this);
        playerListener = new PlayerListener(this);
        statusListener = new StatusListener(this);
    }

    public JDA getDiscord() {
        return discord;
    }

    @Override
    public void onReady(ReadyEvent event) {
        discord.getPresence().setPresence(OnlineStatus.ONLINE, Activity.playing("some Minecraft"));

        boolean satisfied = true;
        Concord.LOGGER.debug(BOT, "Checking guild and channel existence, and satisfaction of required permissions...");
        // Checking if specified guild and channel IDs are correct
        final Guild guild = discord.getGuildById(ConcordConfig.GUILD_ID);
        if (guild == null) {
            Concord.LOGGER.warn(BOT, "This bot is not connected to a guild with ID {}, as specified in the config.",
                ConcordConfig.GUILD_ID);
            Concord.LOGGER.warn(BOT, "This indicates either the bot was not invited to the guild, or a wrongly-typed guild ID.");
            satisfied = false;

        } else {
            final GuildChannel channel = guild.getGuildChannelById(ConcordConfig.CHANNEL_ID);
            if (channel == null) {
                Concord.LOGGER.error(BOT, "There is no channel with ID {} within the guild, as specified in the config.",
                    ConcordConfig.CHANNEL_ID);
                satisfied = false;

            } else if (channel.getType() != ChannelType.TEXT) {
                Concord.LOGGER.error(BOT, "The channel with ID {} is not a TEXT channel, it was of type {}.",
                    ConcordConfig.CHANNEL_ID, channel.getType());
                satisfied = false;

            } else { // Guild and channel IDs are correct, now to check permissions
                final Sets.SetView<Permission> missingPermissions = Sets
                    .difference(REQUIRED_PERMISSIONS, guild.getSelfMember().getPermissions(channel));

                if (!missingPermissions.isEmpty()) {
                    Concord.LOGGER.error(BOT, "This bot is missing the following required permissions in the channel: {}.",
                        missingPermissions);
                    Concord.LOGGER.error(BOT, "As reference, the bot requires the following permissions in the channel: {}.",
                        REQUIRED_PERMISSIONS);
                    satisfied = false;
                }
            }
        } // Required permissions are there. All checks satisfied.
        if (!satisfied) {
            Concord.LOGGER.warn(BOT, "Some checks were not satisfied; disabling Discord integration.");
            Concord.disable();
            return;
        }
        Concord.LOGGER.debug(BOT, "Guild and channel are correct, and permissions are satisfied.");

        Concord.LOGGER.info(BOT, "Discord bot is ready!");
        Concord.LOGGER.info(BOT, "Invite URL for bot: {}", discord.getInviteUrl(REQUIRED_PERMISSIONS));

        Messaging.sendToChannel(discord, new TranslationTextComponent("message.concord.bot.start").getString());
    }

    void shutdown() {
        Concord.LOGGER.info(BOT, "Shutting down Discord bot...");
        MinecraftForge.EVENT_BUS.unregister(msgListener);
        MinecraftForge.EVENT_BUS.unregister(playerListener);
        MinecraftForge.EVENT_BUS.unregister(statusListener);
        discord.shutdown();
    }
}
