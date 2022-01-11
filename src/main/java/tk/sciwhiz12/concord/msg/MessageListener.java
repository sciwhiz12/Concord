package tk.sciwhiz12.concord.msg;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import tk.sciwhiz12.concord.ChatBot;
import tk.sciwhiz12.concord.ConcordConfig;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageListener extends ListenerAdapter {
    // Using a concurrent queue because messages is received on a different thread from the main server thread.
    private final Queue<MessageEntry> queuedMessages = new ConcurrentLinkedQueue<>();
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

        if (event.getGuild().getIdLong() == MiscUtil.parseSnowflake(ConcordConfig.GUILD_ID.get()) &&
            event.getChannel().getIdLong() == MiscUtil.parseSnowflake(ConcordConfig.CHANNEL_ID.get())) {

            queuedMessages.add(new MessageEntry(event));
        }
    }

    @SubscribeEvent
    void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MessageEntry entry;
        while ((entry = queuedMessages.poll()) != null) { // TODO: rate-limiting
            Messaging.sendToAllPlayers(bot.getServer(), entry.member, entry.message.getContentDisplay());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    void onServerChat(ServerChatEvent event) {
        Messaging.sendToChannel(bot.getDiscord(), event.getComponent().getString());
    }

    static record MessageEntry(Member member, Message message) {
        MessageEntry(GuildMessageReceivedEvent event) {
            this(event.getMember(), event.getMessage());
        }
    }
}
