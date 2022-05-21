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

import com.google.common.collect.Maps;
import net.minecraft.network.chat.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class IntelligentTranslator<C> {
    private final Map<C, TranslatableComponent> cache = new HashMap<>();
    private final Function<C, TranslatableComponent> componentCreator;
    private final KeyTranslator<C> keyFunction;

    public IntelligentTranslator(Function<C, TranslatableComponent> componentCreator, KeyTranslator<C> keyFunction) {
        this.componentCreator = componentCreator;
        this.keyFunction = keyFunction;
    }

    public TranslatableComponent resolve(C context) {
        return cache.computeIfAbsent(context, this::doResolve);
    }

    private TranslatableComponent doResolve(C context) {
        final TranslatableComponent component = componentCreator.apply(context);
        final Set<String> componentKeys = collectKeys(component);

        // Resolve all the keys once ahead of time
        final Map<String, String> newKeys = new HashMap<>(Maps.asMap(componentKeys, key -> keyFunction.apply(key, context)));

        // If the original and transformed keys are the same, delete
        newKeys.entrySet().removeIf(entry -> entry.getKey().equals(entry.getValue()));

        // If there are no changes between the original keys and the new keys (as applied by the function with context),
        // Return the base component directly
        if (newKeys.isEmpty()) {
            return component;
        }

        return (TranslatableComponent) checkComponent(component, key -> newKeys.getOrDefault(key, key));
    }

    // This returns a new component
    private MutableComponent checkComponent(Component original, UnaryOperator<String> resolver) {
        MutableComponent result;
        if (original instanceof TranslatableComponent translatable) {
            result = translateEagerly(translatable, resolver);
        } else {
            result = original.plainCopy();
        }
        result.setStyle(checkHover(original.getStyle(), resolver));
        checkSiblings(original, resolver).forEach(result::append);
        return result;
    }

    // Does not modify the original component
    private List<Component> checkSiblings(Component original, UnaryOperator<String> resolver) {
        final ArrayList<Component> result = new ArrayList<>();
        for (Component sibling : original.getSiblings()) {
            // The new siblings are copies (see checkComponent)
            if (sibling instanceof TranslatableComponent translatable) {
                result.add(checkComponent(translatable, resolver));
            } else {
                result.add(checkComponent(sibling, resolver));
            }
        }
        return result;
    }

    // The returned (newly created) component does not have the original's style or siblings
    private TranslatableComponent translateEagerly(TranslatableComponent component, UnaryOperator<String> resolver) {
        Object[] oldArgs = component.getArgs();
        Object[] newArgs = new Object[oldArgs.length];

        for (int i = 0; i < oldArgs.length; i++) {
            Object obj = oldArgs[i];
            if (obj instanceof Component componentArg) {
                obj = checkComponent(componentArg, resolver);
            }
            newArgs[i] = obj;
        }

        return new TranslatableComponent(resolver.apply(component.getKey()), newArgs);
    }

    // Returns a new style
    private Style checkHover(Style style, UnaryOperator<String> resolver) {
        @Nullable HoverEvent hover = style.getHoverEvent();
        if (hover != null && hover.getAction() == HoverEvent.Action.SHOW_TEXT) {
            @Nullable Component hoverComponent = hover.getValue(HoverEvent.Action.SHOW_TEXT);
            if (hoverComponent != null) {
                return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        checkComponent(hoverComponent, resolver)));
            }
        }
        return style;
    }

    private Set<String> collectKeys(TranslatableComponent component) {
        final HashSet<String> keys = new HashSet<>();
        final Deque<Component> components = new ArrayDeque<>();
        components.add(component);

        @Nullable Component current;
        while ((current = components.poll()) != null) {
            if (current instanceof TranslatableComponent translatable) {
                keys.add(translatable.getKey());

                // Add components in the TranslatableComponent's args for checking
                for (Object arg : translatable.getArgs()) {
                    if (arg instanceof Component componentArg) {
                        components.add(componentArg);
                    }
                }
            }

            // Add all component's siblings for checking
            components.addAll(current.getSiblings());

            @Nullable HoverEvent hover = current.getStyle().getHoverEvent();
            if (hover != null) {
                @Nullable Component hoverComponent = hover.getValue(HoverEvent.Action.SHOW_TEXT);
                if (hoverComponent != null) {
                    // Add component in hover for checking
                    components.add(hoverComponent);
                }
            }
        }

        return keys;
    }

    @FunctionalInterface
    public interface KeyTranslator<C> extends BiFunction<String, C, String> {
        @Override
        String apply(String originalKey, C context);
    }
}
