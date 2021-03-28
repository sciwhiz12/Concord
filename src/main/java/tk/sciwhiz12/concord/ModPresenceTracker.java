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
    public static final ResourceLocation CHANNEL_NAME = new ResourceLocation(Concord.MODID, "exists");
    public static EventNetworkChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.ChannelBuilder
            .named(CHANNEL_NAME)
            .networkProtocolVersion(() -> "yes")
            .clientAcceptedVersions(version -> true)
            .serverAcceptedVersions(version -> true)
            .eventNetworkChannel();
    }

    public static boolean isModPresent(@Nullable ServerPlayerEntity client) {
        return client != null && CHANNEL.isRemotePresent(client.connection.getNetworkManager());
    }
}
