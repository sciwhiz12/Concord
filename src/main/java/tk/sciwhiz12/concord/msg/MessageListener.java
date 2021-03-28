package tk.sciwhiz12.concord.msg;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import tk.sciwhiz12.concord.ChatBot;
import tk.sciwhiz12.concord.ConcordConfig;

public class MessageListener extends ListenerAdapter {
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

        if (event.getGuild().getIdLong() == MiscUtil.parseSnowflake(ConcordConfig.GUILD_ID) &&
            event.getChannel().getIdLong() == MiscUtil.parseSnowflake(ConcordConfig.CHANNEL_ID)) {

            Messaging.sendToAllPlayers(ServerLifecycleHooks.getCurrentServer(), event.getMember(),
                event.getMessage().getContentDisplay());
            // TODO: remove dependency on ServerLifecycleHooks
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    void onServerChat(ServerChatEvent event) {
        Messaging.sendToChannel(bot.getDiscord(), event.getComponent().getString());
    }
}
