package tk.sciwhiz12.concord;

import tk.sciwhiz12.concord.compat.emojiful.EmojifulCompat;

public class ConcordClient {

	static void setup() {
		if (Concord.emojifulLoaded()) {
			try {
				EmojifulCompat.lookupIndexEmojisMethod();
			} catch (NoSuchMethodException | IllegalAccessException e) {
				Concord.LOGGER.error("Exception while trying to setup Emojiful compatibility. A RuntimeException will be now thrown because otherwise things will go badly aftwerwards.", e);
				throw new RuntimeException(e);
			}
		}
	}
	
}