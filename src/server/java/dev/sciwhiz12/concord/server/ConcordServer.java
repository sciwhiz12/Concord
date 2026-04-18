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

package dev.sciwhiz12.concord.server;

import com.google.common.base.Strings;
import dev.sciwhiz12.concord.Concord;
import dev.sciwhiz12.concord.ConcordConfig;
import dev.sciwhiz12.concord.server.command.ConcordCommand;
import dev.sciwhiz12.concord.server.command.ReportCommand;
import dev.sciwhiz12.concord.network.ConcordNetwork;
import dev.sciwhiz12.concord.util.Messages;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.utils.config.ThreadingConfig;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

@Mod(ConcordServer.MODID)
public class ConcordServer extends Concord {
    @Nullable
    public static ChatBot BOT;

    public ConcordServer(ModContainer container, IEventBus modBus) {
        super();
        ConcordNetwork.register(modBus);
        ConcordConfig.register(container);

        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(ConcordCommand::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(ReportCommand::onRegisterCommands);

        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform()
                .unstarted(() -> {
                    if (BOT != null) {
                        BOT.messaging().allowProcessingMessages(false);
                        // Server probably crashed. Send the stop message if configured
                        if (ConcordConfig.SERVER_STOP.get()) {
                            BOT.messaging().sendSystemMessage(Messages.SERVER_STOP.component());
                            BOT.messaging().processMessages(true);
                        }
                        BOT.getDiscord().shutdownNow();
                    }
                }));
    }

    public void onServerStarting(ServerStartingEvent event) {
        if (!event.getServer().isDedicatedServer() && !ConcordConfig.ENABLE_INTEGRATED.get()) {
            LOGGER.info("Discord integration for integrated servers is disabled in server config.");
            return;
        }
        enable(true, event.getServer());
    }

    public void onServerStopping(ServerStoppingEvent event) {
        if (isEnabled()) {
            disable(true);
        }
    }

    @Nullable
    public static ChatBot getBotOrNull() {
        return BOT;
    }

    public static ChatBot getBot() {
        if (BOT == null) {
            throw new IllegalStateException("Tried to retrieve chat bot while disabled");
        }
        return BOT;
    }

    public static boolean isEnabled() {
        return BOT != null;
    }

    public static void disable() {
        disable(false);
    }

    public static void disable(boolean suppressMessage) {
        if (BOT == null || !isEnabled()) return;
        LOGGER.info("Shutting down Discord integration...");
        if (!suppressMessage && ConcordConfig.BOT_STOP.get()) {
            BOT.messaging().sendSystemMessage(Messages.BOT_STOP.component());
        }
        BOT.shutdown();
        BOT = null;
    }

    public static void enable(MinecraftServer server) {
        enable(false, server);
    }

    @ApiStatus.Internal
    public static void enable(boolean causedByServerStart, MinecraftServer server) {
        if (isEnabled()) return;
        final String token = ConcordConfig.TOKEN.get();
        if (Strings.isNullOrEmpty(token)) {
            LOGGER.warn("Bot token is not set in config; Discord integration will not be enabled.");
            return;
        } else if (Strings.isNullOrEmpty(ConcordConfig.GUILD_ID.get())) {
            LOGGER.warn("Guild ID is not set in config; Discord integration will not be enabled.");
            return;
        } else if (Strings.isNullOrEmpty(ConcordConfig.CHAT_CHANNEL_ID.get())) {
            LOGGER.warn("Channel ID is not set in config; Discord integration will not be enabled.");
            return;
        }
        LOGGER.info("Initializing Discord integration.");
        JDABuilder jdaBuilder = JDABuilder.createDefault(token)
                .setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ONLINE)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MEMBERS)
                .enableCache(EnumSet.of(CacheFlag.CLIENT_STATUS, CacheFlag.ACTIVITY))
                .setAutoReconnect(true)
                // JDA sets this to be a non-daemon thread; we need it to be a daemon thread
                .setRateLimitScheduler(ThreadingConfig.newScheduler(2, () -> "JDA", "RateLimit-Scheduler", true))
                .setActivity(Activity.playing("the readying game..."))
                .setStatus(OnlineStatus.DO_NOT_DISTURB);
        try {
            final JDA jda = jdaBuilder.build();
            BOT = new ChatBot(jda, server, causedByServerStart);
        } catch (InvalidTokenException e) {
            LOGGER.error("Error while trying to login to Discord; integration will not be enabled.", e);
        }
    }
}
