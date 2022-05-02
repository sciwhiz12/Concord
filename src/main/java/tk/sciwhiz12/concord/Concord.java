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

package tk.sciwhiz12.concord;

import com.google.common.base.Strings;
import com.mojang.logging.LogUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;
import org.slf4j.Logger;
import tk.sciwhiz12.concord.command.ConcordCommand;
import tk.sciwhiz12.concord.command.EmoteCommandHook;
import tk.sciwhiz12.concord.command.ReportCommand;
import tk.sciwhiz12.concord.command.ConcordDiscordCommand;
import tk.sciwhiz12.concord.command.SayCommandHook;
import tk.sciwhiz12.concord.msg.Messaging;
import tk.sciwhiz12.concord.util.Messages;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.util.EnumSet;

@Mod(Concord.MODID)
public class Concord {
    public static final String MODID = "concord";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Nullable
    public static ChatBot BOT;

    public Concord() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (ver, remote) -> true));
        ConcordNetwork.register();

        ConcordConfig.register();

        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(ConcordCommand::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(ReportCommand::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(SayCommandHook::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(EmoteCommandHook::onRegisterCommands);
    }


    public void onServerStarting(ServerStartingEvent event) {
        if (!event.getServer().isDedicatedServer() && !ConcordConfig.ENABLE_INTEGRATED.get()) {
            LOGGER.info("Discord integration for integrated servers is disabled in server config.");
            return;
        }
        enable(event.getServer());
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
        if (!suppressMessage) {
            Messaging.sendToChannel(BOT.getDiscord(), Messages.BOT_STOP.component().getString());
        }
        BOT.shutdown();
        BOT = null;
    }

    public static void enable(MinecraftServer server) {
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
                .enableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MEMBERS)
                .enableCache(EnumSet.of(CacheFlag.CLIENT_STATUS, CacheFlag.ACTIVITY))
                .setAutoReconnect(true)
                .setActivity(Activity.playing("the readying game..."))
                .setStatus(OnlineStatus.DO_NOT_DISTURB);
        try {
            final JDA jda = jdaBuilder.build();
            BOT = new ChatBot(jda, server);
            ConcordDiscordCommand.postInit();
        } catch (LoginException e) {
            LOGGER.error("Error while trying to login to Discord; integration will not be enabled.", e);
        }
    }
}
