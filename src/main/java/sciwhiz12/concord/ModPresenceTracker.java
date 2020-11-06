package sciwhiz12.concord;

import io.netty.util.AttributeKey;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.FMLMCRegisterPacketHandler;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.event.EventNetworkChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.WeakHashMap;
import javax.annotation.Nullable;

/**
 * Tracks if a client has this mod installed.
 *
 * @author SciWhiz12
 */
public class ModPresenceTracker {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final WeakHashMap<ServerPlayerEntity, Boolean> CLIENTS_LIST = new WeakHashMap<>();

    public static final AttributeKey<FMLMCRegisterPacketHandler.ChannelList> FML_MC_REGISTRY = AttributeKey
        .valueOf("minecraft:netregistry");

    public static final ResourceLocation CHANNEL_NAME = new ResourceLocation(Concord.MODID, "exists");
    public static EventNetworkChannel CHANNEL;

    public static void registerChannel() {
        CHANNEL = NetworkRegistry.ChannelBuilder
            .named(CHANNEL_NAME)
            .networkProtocolVersion(() -> "yes")
            .clientAcceptedVersions(version -> true)
            .serverAcceptedVersions(version -> true)
            .eventNetworkChannel();
        CHANNEL.addListener(ModPresenceTracker::onChannelEvent);
    }

    static void onChannelEvent(NetworkEvent.ChannelRegistrationChangeEvent event) {
        LOGGER.debug("Received channel registration change event; mod is present on client");
        CLIENTS_LIST.put(event.getSource().get().getSender(), true);
    }

    public static boolean isModPresent(@Nullable ServerPlayerEntity client) {
        return client != null && CLIENTS_LIST.getOrDefault(client, false);
    }
}
