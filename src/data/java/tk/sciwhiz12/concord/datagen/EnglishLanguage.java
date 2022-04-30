/*
 * Concord - Copyright (c) 2020-2022 SciWhiz12
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

package tk.sciwhiz12.concord.datagen;

import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;
import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.util.Messages;
import tk.sciwhiz12.concord.util.Translations;

public class EnglishLanguage extends LanguageProvider {
    public EnglishLanguage(DataGenerator gen) {
        super(gen, Concord.MODID, LanguageManager.DEFAULT_LANGUAGE_CODE);
    }

    @Override
    protected void addTranslations() {
        for (Messages message : Messages.values()) {
            add(message.key(), message.englishText());
        }

        for (Translations translation : Translations.values()) {
            add(translation.key(), translation.englishText());
        }
    }
}
