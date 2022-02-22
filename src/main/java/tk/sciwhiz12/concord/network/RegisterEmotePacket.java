package tk.sciwhiz12.concord.network;

import java.util.List;
import java.util.Map;

import net.minecraft.network.FriendlyByteBuf;

import net.dv8tion.jda.api.entities.Emote;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.compat.emojiful.EmojifulCompat;

public final class RegisterEmotePacket {

	private final Map<String, List<EmoteData>> emotes;
	
	public RegisterEmotePacket(Map<String, List<EmoteData>> emotes) {
		this.emotes = emotes;
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeMap(emotes, FriendlyByteBuf::writeUtf, (buf, list) -> buf.writeCollection(list, (buf2, d) -> d.write(buf2)));
	}
	
	public void handle(NetworkEvent.Context context) {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			if (Concord.emojifulLoaded()) {
				emotes.forEach((guildName, emts) -> {
					emts.forEach(data -> 
					EmojifulCompat.loadDiscordEmoji(guildName, data.emoteId(), data.emoteName()));
					Concord.LOGGER.info("Registered {} Emojiful emojis from guild \"{}\"", emts.size(), guildName);
				});
				EmojifulCompat.indexEmojis();
			}
		});
	}
	
	public static RegisterEmotePacket decode(FriendlyByteBuf buffer) {
		return new RegisterEmotePacket(buffer.readMap(FriendlyByteBuf::readUtf, buf -> buf.readList(EmoteData::new)));
	}
	
	public record EmoteData(long emoteId, String emoteName) {
		public void write(FriendlyByteBuf buffer) {
			buffer.writeLong(emoteId);
			buffer.writeUtf(emoteName);
		}
		
		public EmoteData(FriendlyByteBuf buffer) {
			this(buffer.readLong(), buffer.readUtf());
		}
		
		public EmoteData(Emote emote) {
			this(emote.getIdLong(), emote.getName());
		}
	}
}