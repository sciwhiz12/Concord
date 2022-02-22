package tk.sciwhiz12.concord.network;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;
import net.minecraftforge.network.simple.SimpleChannel;
import tk.sciwhiz12.concord.Concord;

public class ConcordNetwork {
    public static final ResourceLocation MOD_EXISTS_CHANNEL_NAME = new ResourceLocation(Concord.MODID, "exists");
    public static final EventNetworkChannel MOD_EXISTS_CHANNEL = NetworkRegistry.ChannelBuilder
        .named(MOD_EXISTS_CHANNEL_NAME)
        .networkProtocolVersion(() -> "yes")
        .clientAcceptedVersions(version -> true)
        .serverAcceptedVersions(version -> true)
        .eventNetworkChannel();

	public static final SimpleChannel EMOJIFUL_CHANNEL = NetworkRegistry.ChannelBuilder
		.named(new ResourceLocation(Concord.MODID, "emojiful"))
		.networkProtocolVersion(() -> "sure")
		.clientAcceptedVersions(version -> true)
		.serverAcceptedVersions(version -> true)
		.simpleChannel();

    public static void register() {
        EMOJIFUL_CHANNEL.messageBuilder(RegisterEmotePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
        	.consumer((pkt, context) -> {
        		pkt.handle(context.get());
        		return true;
        	})
        	.decoder(RegisterEmotePacket::decode)
        	.encoder(RegisterEmotePacket::encode)
        	.add();
        
        EMOJIFUL_CHANNEL.messageBuilder(RemoveEmotePacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
	        .consumer((pkt, context) -> {
	        	pkt.handle(context.get());
	        	return true;
	        })
	        .decoder(RemoveEmotePacket::decode)
	        .encoder(RemoveEmotePacket::encode)
	        .add();
    }
    
    public static <MSG> void sendToAllInServer(SimpleChannel channel, MinecraftServer server, MSG msg) {
    	server.getPlayerList().getPlayers().forEach(player -> {
    		if (isModPresent(player)) {
    			channel.sendTo(msg, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    		}
    	});
    }

	/**
	 * Tracks if a client has this mod installed.
	 *
	 * @author SciWhiz12
	 */
    public static boolean isModPresent(@Nullable ServerPlayer client) {
        return client != null && MOD_EXISTS_CHANNEL.isRemotePresent(client.connection.getConnection());
    }
}