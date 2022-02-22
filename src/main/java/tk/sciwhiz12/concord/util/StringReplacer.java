package tk.sciwhiz12.concord.util;

import java.util.HashMap;
import java.util.Map;

public final class StringReplacer {

	private final Map<String, String> regex = new HashMap<>();

	public void add(String oldChar, String replacement) {
		this.regex.put(oldChar, replacement);
	}

	public void remove(String oldChar) {
		regex.remove(oldChar);
	}

	public String replace(String toFormat) {
		var str = toFormat;
		for (final var entry : regex.entrySet()) {
			str = str.replace(entry.getKey(), entry.getValue());
		}
		return str;
	}

	public String replace(CharSequence toFormat) {
		return replace(toFormat.toString());
	}

}