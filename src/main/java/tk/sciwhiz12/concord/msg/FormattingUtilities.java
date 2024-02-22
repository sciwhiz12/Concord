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

package tk.sciwhiz12.concord.msg;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import tk.sciwhiz12.concord.ConcordConfig;

import static net.minecraft.ChatFormatting.WHITE;

// Package-private class for formatting-related helper/utility methods
final class FormattingUtilities {
    private FormattingUtilities() {
    }

    /**
     * Search a user-input string for legacy-style ChatFormatting.
     * ie. the input "&5Sup?" will be sent to Minecraft as "Sup?" with a purple color.
     * It is intentional that this only supports the default vanilla formatting.
     *
     * @param input the text from Discord
     * @return a properly formatted MutableComponent to be echoed into chat.
     * @author Curle
     */
    public static MutableComponent processLegacyFormatting(String input) {
        if (!ConcordConfig.USE_LEGACY_FORMATTING.get()) {
            // Default to white if legacy formatting is disabled.
            return Component.literal(input).withStyle(WHITE);
        } else {
            final String[] parts = input.split("(?=&)");
            MutableComponent currentComponent = Component.literal("");

            for (String part : parts) {
                // Ensure that we only process non-empty strings
                if (part.isEmpty()) continue;

                final boolean partHasFormatter = part.charAt(0) == '&';
                // Short circuit for strings of only "&" to avoid a temporal paradox
                if (partHasFormatter && part.length() == 1) {
                    currentComponent = currentComponent.append(Component.literal(part).withStyle(WHITE));
                    continue;
                }

                // Parse a formatting character after the & trigger
                final ChatFormatting formatting = ChatFormatting.getByCode(part.charAt(1));
                // Ensure that we only process if there's a formatting code
                if (partHasFormatter && formatting != null) {
                    currentComponent = currentComponent.append(Component.literal(part.substring(2)).withStyle(formatting));
                } else {
                    // White by default!
                    currentComponent = currentComponent.append(Component.literal(part).withStyle(WHITE));
                }
            }

            return currentComponent;
        }
    }

    /**
     * Search a user-input string for a custom chat formatting syntax.
     * The custom syntax follows the format of $color or $#hex.
     * ie. "$red chat" will print "chat" in red. "$#0000FF sup" will print "sup" in blue.
     * There is no custom formatting for italic, strikethrough, bold, etc.
     *
     * @param input the text from Discord
     * @return a properly formatted MutableComponent to be echoed into chat.
     * @author Curle
     */
    public static MutableComponent processCustomFormatting(String input) {
        if (!ConcordConfig.USE_CUSTOM_FORMATTING.get()) {
            // Default to white if custom formatting is disabled.
            return processLegacyFormatting(input);
        } else {
            MutableComponent currentComponent = Component.literal("");
            // Regexplanation:
            // (?= \$ #? [\w \d] + )
            // ^   ^  ^^ ^ ^ ^   ^ ^
            // |   |  || | | |   | |
            // |   |  || | | |   | - End group
            // |   |  || | | |   - Match at least one
            // |   |  || | | - Match any digit
            // |   |  || | - Match any word
            // |   |  || - Look for any of the following
            // |   |  |- Match 0 or 1 of the preceding
            // |   |  - Look for a # character
            // |   - Look for a $ character
            // - Include the result in the split strings

            final String[] parts = input.split("(?=\\$#?[\\w\\d]+)");


            for (String part : parts) {
                // Ensure that we only process non-empty strings
                if (part.isEmpty()) continue;

                final boolean partHasFormatter = part.charAt(0) == '$';
                final int firstSpacePosition = part.indexOf(' ');

                // Short circuit for strings of only "$" to avoid a temporal paradox
                if (partHasFormatter && part.length() == 1 && firstSpacePosition == -1) {
                    currentComponent = currentComponent.append(Component.literal(part).withStyle(WHITE));
                    continue;
                }

                // Make sure that formatting at the end of messages, or lone formatting, is dealt with.
                final String formatString = firstSpacePosition == -1 ? part.substring(1) : part.substring(1, firstSpacePosition);
                // Use TextColor's built-in parsing to do the heavy lifting.
                final TextColor color = TextColor.parseColor(formatString).get().left().orElse(TextColor.fromLegacyFormat(WHITE));
                // Assign the TextColor into a Style instance so that we can use it with a TextComponent.
                final Style formatting = Style.EMPTY.withColor(color);

                if (partHasFormatter && color != null) {

                    currentComponent = currentComponent.append(
                            // Cut the string on either the space (if there is one) or the end of the string.
                            Component.literal(part.substring(firstSpacePosition != -1 ? firstSpacePosition + 1 : part.length()))
                                    .withStyle(formatting)
                    );
                } else {
                    // White by default!
                    currentComponent = currentComponent.append(
                            Component.literal(part).withStyle(WHITE)
                    );
                }

            }


            return currentComponent;
        }
    }
}
