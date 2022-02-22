package tk.sciwhiz12.concord.network;

import java.util.List;
import java.util.Map;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.compat.emojiful.EmojifulCompat;
import tk.sciwhiz12.concord.network.RegisterEmotePacket.EmoteData;

public final class RemoveEmotePacket {

	private final Map<String, List<EmoteData>> emotes;

	public RemoveEmotePacket(Map<String, List<EmoteData>> emotes) {
		this.emotes = emotes;
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeMap(emotes, FriendlyByteBuf::writeUtf,
				(buf, list) -> buf.writeCollection(list, (buf2, d) -> d.write(buf2)));
	}

	public void handle(NetworkEvent.Context context) {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			if (Concord.emojifulLoaded()) {
				emotes.forEach((guildName, emts) -> {
					emts.forEach(
							data -> EmojifulCompat.removeDiscordEmoji(guildName, data.emoteId(), data.emoteName()));
					Concord.LOGGER.info("Removed {} Emojiful emojis from guild \"{}\"", emts.size(), guildName);
				});
				EmojifulCompat.indexEmojis();
			}
		});
	}

	public static RemoveEmotePacket decode(FriendlyByteBuf buffer) {
		return new RemoveEmotePacket(buffer.readMap(FriendlyByteBuf::readUtf, buf -> buf.readList(EmoteData::new)));
	}
}
