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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

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
            str = fastReplace(str, entry.getKey(), entry.getValue());
        }
        return str;
    }

    public String replace(CharSequence toFormat) {
        return replace(toFormat.toString());
    }

    @ParametersAreNonnullByDefault
    public static String fastReplace(String str, String target, String replacement) {
        int targetLength = target.length();
        if (targetLength == 0) {
            return str;
        }
        int idx2 = str.indexOf(target);
        if (idx2 < 0) {
            return str;
        }
        StringBuilder buffer = new StringBuilder(targetLength > replacement.length() ? str.length() : str.length() * 2);
        int idx1 = 0;
        do {
            buffer.append(str, idx1, idx2);
            buffer.append(replacement);
            idx1 = idx2 + targetLength;
            idx2 = str.indexOf(target, idx1);
        } while (idx2 > 0);
        buffer.append(str, idx1, str.length());
        return buffer.toString();
    }

}
