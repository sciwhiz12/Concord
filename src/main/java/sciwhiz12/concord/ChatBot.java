package sciwhiz12.concord;

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
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import sciwhiz12.concord.msg.MessageListener;

import java.util.EnumSet;

import static sciwhiz12.concord.Concord.LOGGER;

public class ChatBot {
    private static final Marker BOT = MarkerManager.getMarker("BOT");
    public static final EnumSet<Permission> REQUIRED_PERMISSIONS =
        EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);

    private final JDA discord;
    private final MessageListener message;

    ChatBot(JDA discord) {
        this.discord = discord;
        discord.setEventManager(new AnnotatedEventManager());
        discord.addEventListener(this);
        message = new MessageListener(this);
        discord.addEventListener(message);
        MinecraftForge.EVENT_BUS.register(message);
    }

    public JDA getDiscord() {
        return discord;
    }

    @SubscribeEvent
    void onReady(ReadyEvent event) {
        discord.getPresence().setPresence(OnlineStatus.ONLINE, Activity.playing("some Minecraft"));

        boolean satisfied = true;
        LOGGER.debug(BOT, "Checking guild and channel existence, and satisfaction of required permissions...");
        // Checking if specified guild and channel IDs are correct
        final Guild guild = discord.getGuildById(ConcordConfig.GUILD_ID);
        if (guild == null) {
            LOGGER.warn(BOT, "This bot is not connected to a guild with ID {}, as specified in the config.",
                ConcordConfig.GUILD_ID);
            LOGGER.warn(BOT, "This indicates either the bot was not invited to the guild, or a wrongly-typed guild ID.");
            satisfied = false;

        } else {
            final GuildChannel channel = guild.getGuildChannelById(ConcordConfig.CHANNEL_ID);
            if (channel == null) {
                LOGGER.error(BOT, "There is no channel with ID {} within the guild, as specified in the config.",
                    ConcordConfig.CHANNEL_ID);
                satisfied = false;

            } else if (channel.getType() != ChannelType.TEXT) {
                LOGGER.error(BOT, "The channel with ID {} is not a TEXT channel, it was of type {}.",
                    ConcordConfig.CHANNEL_ID, channel.getType());
                satisfied = false;

            } else { // Guild and channel IDs are correct, now to check permissions
                final Sets.SetView<Permission> missingPermissions = Sets
                    .difference(REQUIRED_PERMISSIONS, guild.getSelfMember().getPermissions(channel));

                if (!missingPermissions.isEmpty()) {
                    LOGGER.error(BOT, "This bot is missing the following required permissions in the channel: {}.",
                        missingPermissions);
                    LOGGER.error(BOT, "As reference, the bot requires the following permissions in the channel: {}.",
                        REQUIRED_PERMISSIONS);
                    satisfied = false;
                }
            }
        } // Required permissions are there. All checks satisfied.
        if (!satisfied) {
            LOGGER.warn(BOT, "Some checks were not satisfied; disabling Discord integration.");
            Concord.disable();
            return;
        }
        LOGGER.debug(BOT, "Guild and channel are correct, and permissions are satisfied.");

        LOGGER.info(BOT, "Discord bot is ready!");
        LOGGER.info(BOT, "Invite URL for bot: {}", discord.getInviteUrl(REQUIRED_PERMISSIONS));
    }

    void shutdown() {
        LOGGER.info(BOT, "Shutting down Discord bot...");
        MinecraftForge.EVENT_BUS.unregister(message);
        discord.shutdown();
    }
}
