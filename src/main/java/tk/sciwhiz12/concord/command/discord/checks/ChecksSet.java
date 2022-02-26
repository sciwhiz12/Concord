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

package tk.sciwhiz12.concord.command.discord.checks;

import java.util.function.Predicate;

public final class ChecksSet implements Predicate<SlashCommandContext> {
    
    public static final ChecksSet DEFAULT = builder().and(Checks.INTEGRATION_ENABLED, Checks.COMMAND_ENABLED).build();

    private final Predicate<SlashCommandContext> checker;
    private ChecksSet(Predicate<SlashCommandContext> checker) {
        this.checker = checker;
    }
    
    @Override
    public boolean test(SlashCommandContext t) {
        return checker.test(t);
    }
    
    public Builder toBuilder() {
        return new Builder(checker);
    }

    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        private Predicate<SlashCommandContext> predicate;
        
        private Builder(Predicate<SlashCommandContext> predicate) {
            this.predicate = predicate;
        }
        
        private Builder() {
            this(ctx -> true);
        }
        
        public Builder and(Predicate<SlashCommandContext> predicate) {
            this.predicate = this.predicate.and(predicate);
            return this;
        }
        
        public Builder and(Checks... perms) {
            for (final var perm : perms) {
                this.predicate = this.predicate.and(perm);
            }
            return this;
        }
        
        public ChecksSet build() {
            return new ChecksSet(predicate);
        }
    }
}