package tk.sciwhiz12.concord.compat.emojiful;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

import com.hrznstudio.emojiful.ClientProxy;
import com.hrznstudio.emojiful.Emojiful;
import com.hrznstudio.emojiful.api.EmojiCategory;

import tk.sciwhiz12.concord.Concord;

public class EmojifulCompat {
	
	private static MethodHandle indexEmojisMethod;
	private static List<String> registeredGuildCategories = new ArrayList<>();
	
	public static void loadDiscordEmoji(String guildName, long emojiId, String emojiName) {
		final var emojifulEmoji = new EmojiFromDiscord(emojiId, emojiName);
		Emojiful.EMOJI_MAP.computeIfAbsent(guildName, n -> new ArrayList<>()).add(emojifulEmoji);
		Emojiful.EMOJI_LIST.add(emojifulEmoji);
		if (!registeredGuildCategories.contains(guildName)) {
			registeredGuildCategories.add(guildName);
			final var cat = new EmojiCategory(guildName, false);
			ClientProxy.CATEGORIES.add(cat);
		}
	}
	
	public static void removeDiscordEmoji(String guildName, long emojiId, String emojiName) {
		Emojiful.EMOJI_MAP.computeIfAbsent(guildName, n -> new ArrayList<>()).stream()
			.filter(e -> e instanceof EmojiFromDiscord dcEmoji && dcEmoji.isSame(emojiId, emojiName))
			.findAny()
			.ifPresent(emoji -> {
				Emojiful.EMOJI_MAP.computeIfAbsent(guildName, k -> new ArrayList<>()).remove(emoji);
				Emojiful.EMOJI_LIST.remove(emoji);
			});
	}
	
	public static void indexEmojis() {
		try {
			indexEmojisMethod.bindTo(ClientProxy.PROXY).invoke();
		} catch (Throwable e) {
			Concord.LOGGER.error("Exception while trying to re-index Emojiful emojis.", e);
		}
	}
	
	public static void lookupIndexEmojisMethod() throws NoSuchMethodException, IllegalAccessException {
		final var type = MethodType.methodType(void.class);
		indexEmojisMethod = MethodHandles.privateLookupIn(ClientProxy.class, MethodHandles.lookup()).findVirtual(ClientProxy.class, "indexEmojis", type);
	}
}