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

package tk.sciwhiz12.concord.msg;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import tk.sciwhiz12.concord.ChatBot;
import tk.sciwhiz12.concord.ConcordConfig;
import tk.sciwhiz12.concord.util.Messages;
import tk.sciwhiz12.concord.util.Translation;

public class PlayerListener {
    private final ChatBot bot;

    public PlayerListener(ChatBot bot) {
        this.bot = bot;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().getCommandSenderWorld().isClientSide()) return;
        if (!ConcordConfig.PLAYER_JOIN.get()) return;

        TranslatableComponent text = Messages.PLAYER_JOIN.component(event.getPlayer().getDisplayName());

        Messaging.sendToChannel(bot.getDiscord(), text.getString());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().getCommandSenderWorld().isClientSide()) return;
        if (!ConcordConfig.PLAYER_LEAVE.get()) return;

        TranslatableComponent text = Messages.PLAYER_LEAVE.component(event.getPlayer().getDisplayName());

        Messaging.sendToChannel(bot.getDiscord(), text.getString());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().getCommandSenderWorld().isClientSide()) return;
        if (!ConcordConfig.PLAYER_DEATH.get()) return;

        if (event.getEntity() instanceof ServerPlayer player) {
            Messaging.sendToChannel(bot.getDiscord(), player.getCombatTracker().getDeathMessage().getString());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    void onAdvancement(AdvancementEvent event) {
        Level world = event.getEntity().getCommandSenderWorld();
        if (world.isClientSide()) return;

        if (ConcordConfig.PLAYER_ADV_GAMERULE.get() && !world.getGameRules().getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS))
            return;

        final DisplayInfo info = event.getAdvancement().getDisplay();
        if (info != null && info.shouldAnnounceChat()) {
            boolean enabled = switch (info.getFrame()) {
                case TASK -> ConcordConfig.PLAYER_ADV_TASK.get();
                case CHALLENGE -> ConcordConfig.PLAYER_ADV_CHALLENGE.get();
                case GOAL -> ConcordConfig.PLAYER_ADV_GOAL.get();
            };
            Translation translation = switch (info.getFrame()) {
                case TASK -> Messages.ADVANCEMENT_TASK;
                case CHALLENGE -> Messages.ADVANCEMENT_CHALLENGE;
                case GOAL -> Messages.ADVANCEMENT_GOAL;
            };
            if (!enabled) return;
            TranslatableComponent text = translation.component(
                event.getPlayer().getDisplayName(),
                info.getTitle(),
                info.getDescription());

            Messaging.sendToChannel(bot.getDiscord(), text.getString());
        }
    }
}
