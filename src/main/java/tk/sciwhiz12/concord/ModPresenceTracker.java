package tk.sciwhiz12.concord;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.event.EventNetworkChannel;

import javax.annotation.Nullable;

/**
 * Tracks if a client has this mod installed.
 *
 * @author SciWhiz12
 */
public class ModPresenceTracker {
    public static final ResourceLocation CHANNEL_NAME = new ResourceLocation(Concord.MODID, "exists");
    public static final EventNetworkChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(CHANNEL_NAME)
        .networkProtocolVersion(() -> "yes")
        .clientAcceptedVersions(version -> true)
        .serverAcceptedVersions(version -> true)
        .eventNetworkChannel();

    public static void register() {
        // Channel is created as part of class initialization
    }

    public static boolean isModPresent(@Nullable ServerPlayer client) {
        return client != null && CHANNEL.isRemotePresent(client.connection.getConnection());
    }
}
