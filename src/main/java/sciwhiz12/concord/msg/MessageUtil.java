package sciwhiz12.concord.msg;

import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.network.ConnectionType;
import net.minecraftforge.fml.network.NetworkHooks;

public final class MessageUtil {
    private MessageUtil() {} // Prevent instantiation

    /* Copied from net.minecraftforge.server.command.TextComponentHelper, and modified to suit our purpose */

    /**
     * Creates a {@link TextComponent} from the given translation key, depending on the {@code lazyTranslate} parameter.
     *
     * If {@code lazyTranslate} is {@code false}, then the returned value is a {@link StringTextComponent} with the message
     * specified by the translation key being eagerly evaluated now. This text component is safe to send to clients, as it does
     * not use a translation key.
     *
     * If {@code lazyTranslate} is {@code true}, then the returned value is a {@link TranslationTextComponent} with the
     * translation key and given arguments passed into it, and the contents of the text component is lazily evaluated (on first
     * use of the text component).
     *
     * @param lazyTranslate Whether to lazily translate the message
     * @param translation   The translation key
     * @param args          Extra arguments to the message
     *
     * @return a {@link TextComponent} with the specified message
     */
    public static TextComponent createTranslation(boolean lazyTranslate, final String translation, final Object... args) {
        TranslationTextComponent text = new TranslationTextComponent(translation, args);
        if (!lazyTranslate) {
            //            return new StringTextComponent(String.format(LanguageMap.getInstance().func_230503_a_(translation),
            //            args));
            return eagerTranslate(text);
        }
        return text;
    }

    private static TextComponent eagerTranslate(final TranslationTextComponent component) {
        Object[] oldArgs = component.getFormatArgs();
        Object[] newArgs = new Object[oldArgs.length];

        for (int i = 0; i < oldArgs.length; i++) {
            Object obj = oldArgs[i];
            if (obj instanceof TranslationTextComponent) {
                newArgs[i] = eagerTranslate((TranslationTextComponent) obj);
            } else {
                newArgs[i] = oldArgs[i];
            }
        }

        TranslationTextComponent result =
            new TranslationTextComponent(LanguageMap.getInstance().func_230503_a_(component.getKey()), newArgs);
        result.mergeStyle(component.getStyle());
        return result;
    }

    /**
     * Returns whether the given {@link CommandSource} refers to a player using a vanilla client (or using a {@linkplain
     * ConnectionType#VANILLA vanilla connection}).
     *
     * @param sender the command sender to check
     *
     * @return if the command sender is connected with a vanilla client
     */
    public static boolean isVanillaClient(CommandSource sender) {
        if (sender.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayNetHandler channel = ((ServerPlayerEntity) sender.getEntity()).connection;
            return NetworkHooks.getConnectionType(() -> channel.netManager) == ConnectionType.VANILLA;
        }
        return false;
    }
}
