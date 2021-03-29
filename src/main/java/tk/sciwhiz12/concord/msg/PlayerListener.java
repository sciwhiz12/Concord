package tk.sciwhiz12.concord.msg;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import tk.sciwhiz12.concord.ChatBot;
import tk.sciwhiz12.concord.ConcordConfig;

public class PlayerListener {
    private final ChatBot bot;

    public PlayerListener(ChatBot bot) {
        this.bot = bot;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().getEntityWorld().isRemote()) return;
        if (!ConcordConfig.PLAYER_JOIN.get()) return;

        TranslationTextComponent text = new TranslationTextComponent("message.concord.player.join",
            event.getPlayer().getDisplayName());

        Messaging.sendToChannel(bot.getDiscord(), text.getString());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().getEntityWorld().isRemote()) return;
        if (!ConcordConfig.PLAYER_LEAVE.get()) return;

        TranslationTextComponent text = new TranslationTextComponent("message.concord.player.leave",
            event.getPlayer().getDisplayName());

        Messaging.sendToChannel(bot.getDiscord(), text.getString());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().getEntityWorld().isRemote()) return;
        if (!ConcordConfig.PLAYER_DEATH.get()) return;

        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();

            Messaging.sendToChannel(bot.getDiscord(), player.getCombatTracker().getDeathMessage().getString());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    void onAdvancement(AdvancementEvent event) {
        if (event.getEntity().getEntityWorld().isRemote()) return;

        final DisplayInfo info = event.getAdvancement().getDisplay();
        if (info != null && info.shouldAnnounceToChat()) {
            switch (info.getFrame()) {
                case TASK: {
                    if (!ConcordConfig.PLAYER_ADV_TASK.get()) return;
                    break;
                }
                case CHALLENGE: {
                    if (!ConcordConfig.PLAYER_ADV_CHALLENGE.get()) return;
                    break;
                }
                case GOAL: {
                    if (!ConcordConfig.PLAYER_ADV_GOAL.get()) return;
                    break;
                }
            }
            TranslationTextComponent text = new TranslationTextComponent(
                "message.concord.player.advancement." + info.getFrame().getName(),
                event.getPlayer().getDisplayName(),
                info.getTitle(),
                info.getDescription());

            Messaging.sendToChannel(bot.getDiscord(), text.getString());
        }
    }
}
