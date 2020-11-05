package sciwhiz12.concord;

import com.google.common.base.Strings;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumSet;
import javax.security.auth.login.LoginException;

@Mod(Concord.MODID)
public class Concord {
    public static final String MODID = "concord";
    public static final Logger LOGGER = LogManager.getLogger();

    public static ChatBot BOT;

    public Concord() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (ver, remote) -> true));

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConcordConfig.SPEC);

        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    public void onServerStarting(FMLServerStartingEvent event) {
        startChatBot();
    }

    public void onServerStopping(FMLServerStoppingEvent event) {
        if (BOT != null) {
            BOT.shutdown();
            BOT = null;
        }
    }

    public static void startChatBot() {
        final String token = ConcordConfig.TOKEN.get();
        if (Strings.isNullOrEmpty(token)) {
            LOGGER.warn("Client token is not set in config; Discord integration will not be enabled.");
            return;
        } else if (Strings.isNullOrEmpty(ConcordConfig.GUILD_ID.get())) {
            LOGGER.warn("Guild ID is not set in config; Discord integration will not be enabled.");
            return;
        } else if (Strings.isNullOrEmpty(ConcordConfig.CHANNEL_ID.get())) {
            LOGGER.warn("Channel ID is not set in config; Discord integration will not be enabled.");
            return;
        }
        if (BOT != null) {
            LOGGER.debug("Discord integration is currently enabled; disabling for restart.");
            BOT.shutdown();
            BOT = null;
        }
        LOGGER.info("Initializing Discord integration.");
        JDABuilder jdaBuilder = JDABuilder.createDefault(ConcordConfig.TOKEN.get())
                .enableIntents(GatewayIntent.GUILD_PRESENCES)
                .enableCache(EnumSet.of(CacheFlag.CLIENT_STATUS, CacheFlag.ACTIVITY))
                .setAutoReconnect(true)
                .setActivity(Activity.playing("the readying game..."))
                .setStatus(OnlineStatus.DO_NOT_DISTURB);
        try {
            final JDA jda = jdaBuilder.build();
            BOT = new ChatBot(jda);
        } catch (LoginException e) {
            LOGGER.error("Error while trying to login to Discord; integration will not be enabled.", e);
        }
    }
}
