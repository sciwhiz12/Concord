package tk.sciwhiz12.concord;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.network.ConnectionType;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.event.EventNetworkChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.WeakHashMap;
import javax.annotation.Nullable;

/**
 * Tracks if a client has this mod installed.
 *
 * @author SciWhiz12
 */
public class ModPresenceTracker {
    private static final Logger LOGGER = LogManager.getLogger();

    static final WeakHashMap<UUID, Boolean> CONCORD_CLIENTS_LIST = new WeakHashMap<>();

    public static final ResourceLocation CHANNEL_NAME = new ResourceLocation(Concord.MODID, "exists");
    public static EventNetworkChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.ChannelBuilder
            .named(CHANNEL_NAME)
            .networkProtocolVersion(() -> "yes")
            .clientAcceptedVersions(version -> true)
            .serverAcceptedVersions(version -> true)
            .eventNetworkChannel();
        CHANNEL.addListener(ModPresenceTracker::onChannelEvent);
        MinecraftForge.EVENT_BUS.addListener(ModPresenceTracker::onPlayerLoggedOut);
    }

    static void onChannelEvent(NetworkEvent.ChannelRegistrationChangeEvent event) {
        if (event.getRegistrationChangeType() != NetworkEvent.RegistrationChangeType.REGISTER) return;
        ServerPlayerEntity client = event.getSource().get().getSender();
        if (client != null && event.getSource().get().getDirection().getReceptionSide().isServer()) {
            LOGGER.debug("Received channel registration change event for our channel; mod is present on client");
            CONCORD_CLIENTS_LIST.put(client.getGameProfile().getId(), true);
        }
    }

    static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        CONCORD_CLIENTS_LIST.remove(event.getPlayer().getGameProfile().getId());
    }

    public static boolean isModPresent(@Nullable ServerPlayerEntity client) {
        return client != null && !isVanillaClient(client)
            && CONCORD_CLIENTS_LIST.getOrDefault(client.getGameProfile().getId(), false);
    }

    // Copied from net.minecraftforge.server.command.TextComponentHelper#isVanillaClient
    private static boolean isVanillaClient(ServerPlayerEntity client) {
        return NetworkHooks.getConnectionType(() -> client.connection.netManager) == ConnectionType.VANILLA;
    }
}
