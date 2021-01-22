package tk.sciwhiz12.concord.msg;

import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import tk.sciwhiz12.concord.ChatBot;
import tk.sciwhiz12.concord.ConcordConfig;

public class StatusListener {
    private final ChatBot bot;

    public StatusListener(ChatBot bot) {
        this.bot = bot;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    void onServerStarted(FMLServerStartedEvent event) {
        if (!ConcordConfig.SERVER_START) return;

        Messaging.sendToChannel(bot.getDiscord(),
            new TranslationTextComponent("message.concord.server.start").getString());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    void onServerStopping(FMLServerStoppingEvent event) {
        if (!ConcordConfig.SERVER_STOP) return;

        Messaging.sendToChannel(bot.getDiscord(),
            new TranslationTextComponent("message.concord.server.stop").getString());
    }
}
