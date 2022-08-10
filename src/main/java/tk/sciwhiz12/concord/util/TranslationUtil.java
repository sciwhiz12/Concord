/*
 * Concord - Copyright (c) 2020 SciWhiz12
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package tk.sciwhiz12.concord.util;

import com.google.common.collect.Streams;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import tk.sciwhiz12.concord.ConcordConfig;
import tk.sciwhiz12.concord.ConcordNetwork;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class TranslationUtil {
    static final Map<String, Translation> KEY_TO_INSTANCE = new HashMap<>();

    private TranslationUtil() {
    } // Prevent instantiation

    @Nullable
    public static Translation findTranslation(String key) {
        if (KEY_TO_INSTANCE.isEmpty()) {
            Streams.concat(
                    Arrays.stream(Messages.values()),
                    Arrays.stream(Translations.values())
            ).forEach(t -> KEY_TO_INSTANCE.put(t.key(), t));
        }
        return KEY_TO_INSTANCE.get(key);
    }

    /* Copied from net.minecraftforge.server.command.TextComponentHelper, and modified to suit our purpose */

    /**
     * Creates a {@link MutableComponent} from the given translation key, depending on the {@code lazyTranslate} parameter.
     * <p>
     * If {@code lazyTranslate} is {@code false}, then the returned value is a {@link MutableComponent} with the message
     * specified by the translation key being eagerly evaluated now. This text component is safe to send to clients, as it does
     * not use a translation key.
     * <p>
     * If {@code lazyTranslate} is {@code true}, then the returned value is a {@link MutableComponent} with the
     * translation key and given arguments passed into it, and the contents of the text component is lazily evaluated (on first
     * use of the text component).
     *
     * @param lazyTranslate Whether to lazily translate the message
     * @param translation   The translation key
     * @param args          Extra arguments to the message
     * @return a {@link MutableComponent} with the specified message
     */
    public static MutableComponent createTranslation(boolean lazyTranslate, final String translation, final Object... args) {
        MutableComponent text = Component.translatable(translation, args);
        return lazyTranslate ? text : eagerTranslate(text);
    }

    public static MutableComponent createTranslation(@Nullable ServerPlayer entity, String translationKey, Object... args) {
        return createTranslation(!ConcordConfig.LAZY_TRANSLATIONS.get() || ConcordNetwork.isModPresent(entity),
                translationKey, args);
    }

    public static MutableComponent eagerTranslate(final Component component) {
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
        if (component.getContents() instanceof TranslatableContents translatable) {
            component = translateEagerly(component, translatable);
        }
        component.withStyle(TranslationUtil::checkHover);
        checkSiblings(component);
        return component;
    }

    private static void checkSiblings(MutableComponent component) {
        final ArrayList<Component> originalSiblings = new ArrayList<>(component.getSiblings());
        component.getSiblings().clear();
        for (Component sibling : originalSiblings) {
            if (sibling.getContents() instanceof TranslatableContents) {
                component.append(eagerTranslate(sibling));
            } else {
                component.append(checkComponent(sibling));
            }
        }
    }

    private static MutableComponent translateEagerly(MutableComponent component, TranslatableContents contents) {
        Object[] oldArgs = contents.getArgs();
        Object[] newArgs = new Object[oldArgs.length];

        for (int i = 0; i < oldArgs.length; i++) {
            Object obj = oldArgs[i];
            if (obj instanceof Component componentArg) {
                obj = checkComponent(componentArg);
            }
            newArgs[i] = obj;
        }

        MutableComponent result = Component.translatable(Language.getInstance().getOrDefault(contents.getKey()), newArgs);
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
