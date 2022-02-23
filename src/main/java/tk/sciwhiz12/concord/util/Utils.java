package tk.sciwhiz12.concord.util;

import java.util.Random;

public final class Utils {

    public static final Random RANDOM = new Random();
    
    public static int generateRandomColour() {
        return (int) (RANDOM.nextDouble() * 16777215);
    }
    
}