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

package tk.sciwhiz12.concord.util.conversion;

import java.io.InputStreamReader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.util.StringReplacer;

public class UnicodeConversion {

    public static final Gson GSON = new GsonBuilder().setLenient().disableHtmlEscaping().create();
    private static final StringReplacer REPLACER = new StringReplacer();

    public static void load() {
        try (final var reader = new InputStreamReader(Concord.class.getResourceAsStream("/unicode_conversion.json"))) {
            final var json = GSON.fromJson(reader, JsonObject.class);
            json.keySet().stream().filter(s -> !s.startsWith("_"))
                .forEach(code -> REPLACER.add(code, ":%s:".formatted(json.get(code).getAsString())));
        } catch (Exception e) {
            Concord.LOGGER.error(
                "Exception while trying to read unicode conversion JSON. Emojiful compatibility will not properly work.",
                e);
        }
    }

    public static String replace(String text) {
        return REPLACER.replace(text);
    }
}