package tk.sciwhiz12.concord.compat.emojiful;

import com.hrznstudio.emojiful.api.Emoji;

public final class EmojiFromDiscord extends Emoji {

	private final String url;
	private final long emojiId;

	public EmojiFromDiscord(long emojiId, String name) {
		this.emojiId = emojiId;
		this.url = "https://cdn.discordapp.com/emojis/%s.png?size=80&quality=lossless".formatted(emojiId);
		this.name = name;
		this.strings.add(":%s:".formatted(name));
	}
	
	public boolean isSame(long emojiId, String name) {
		return this.emojiId == emojiId && this.name.equals(name);
	}

	@Override
	public String getUrl() {
		return url;
	}

}