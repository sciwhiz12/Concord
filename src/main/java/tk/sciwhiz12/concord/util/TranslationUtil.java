package tk.sciwhiz12.concord.util;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import tk.sciwhiz12.concord.ConcordConfig;
import tk.sciwhiz12.concord.ModPresenceTracker;

import javax.annotation.Nullable;
import java.util.ArrayList;

public final class TranslationUtil {
    private TranslationUtil() {
    } // Prevent instantiation

    /* Copied from net.minecraftforge.server.command.TextComponentHelper, and modified to suit our purpose */

    /**
     * Creates a {@link BaseComponent} from the given translation key, depending on the {@code lazyTranslate} parameter.
     * <p>
     * If {@code lazyTranslate} is {@code false}, then the returned value is a {@link TextComponent} with the message
     * specified by the translation key being eagerly evaluated now. This text component is safe to send to clients, as it does
     * not use a translation key.
     * <p>
     * If {@code lazyTranslate} is {@code true}, then the returned value is a {@link TranslatableComponent} with the
     * translation key and given arguments passed into it, and the contents of the text component is lazily evaluated (on first
     * use of the text component).
     *
     * @param lazyTranslate Whether to lazily translate the message
     * @param translation   The translation key
     * @param args          Extra arguments to the message
     * @return a {@link BaseComponent} with the specified message
     */
    public static MutableComponent createTranslation(boolean lazyTranslate, final String translation, final Object... args) {
        TranslatableComponent text = new TranslatableComponent(translation, args);
        return lazyTranslate ? text : eagerTranslate(text);
    }

    public static MutableComponent createTranslation(@Nullable ServerPlayer entity, String translationKey, Object... args) {
        return createTranslation(!ConcordConfig.LAZY_TRANSLATIONS.get() || ModPresenceTracker.isModPresent(entity),
            translationKey, args);
    }

    public static MutableComponent eagerTranslate(final TranslatableComponent component) {
        return checkComponent(component);
    }

    public static MutableComponent checkComponent(Component component) {
        if (component instanceof MutableComponent mutable) {
            return checkComponent(mutable);
        }
        return checkComponent(component.copy());
    }

    // Use the above instead
    private static MutableComponent checkComponent(MutableComponent component) {
        if (component instanceof TranslatableComponent translatable) {
            component = translateEagerly(translatable);
        }
        component.withStyle(TranslationUtil::checkHover);
        checkSiblings(component);
        return component;
    }

    private static void checkSiblings(MutableComponent component) {
        final ArrayList<Component> originalSiblings = new ArrayList<>(component.getSiblings());
        component.getSiblings().clear();
        for (Component sibling : originalSiblings) {
            if (sibling instanceof TranslatableComponent translatable) {
                component.append(eagerTranslate(translatable));
            } else {
                component.append(checkComponent(sibling));
            }
        }
    }

    private static TranslatableComponent translateEagerly(TranslatableComponent component) {
        Object[] oldArgs = component.getArgs();
        Object[] newArgs = new Object[oldArgs.length];

        for (int i = 0; i < oldArgs.length; i++) {
            Object obj = oldArgs[i];
            if (obj instanceof Component componentArg) {
                obj = checkComponent(componentArg);
            }
            newArgs[i] = obj;
        }

        TranslatableComponent result = new TranslatableComponent(Language.getInstance().getOrDefault(component.getKey()), newArgs);
        result.setStyle(component.getStyle());
        component.getSiblings().forEach(result::append);
        return result;
    }

    private static Style checkHover(Style style) {
        HoverEvent hover = style.getHoverEvent();
        if (hover != null && hover.getAction() == HoverEvent.Action.SHOW_TEXT) {
            Component hoverComponent = hover.getValue(HoverEvent.Action.SHOW_TEXT);
            if (hoverComponent != null) {
                return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, checkComponent(hoverComponent)));
            }
        }
        return style;
    }
}
