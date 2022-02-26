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

import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.ConcordConfig;

public enum Checks implements Predicate<SlashCommandContext> { 
    INTEGRATION_ENABLED(ctx -> {
        if (!Concord.isEnabled()) {
            ctx.deferReply(true).setContent("This command required Discord Integration to be active, but it isn't.").queue();
            return false;
        }
        return true;
    }),
    COMMAND_ENABLED(ctx -> {
        final var cmdName = ctx.event().getName();
        if (ConcordConfig.DISCORD_COMMANDS_ENABLED.containsKey(cmdName) && !ConcordConfig.DISCORD_COMMANDS_ENABLED.get(cmdName).get()) {
            ctx.deferReply(true).setContent("This command is disabled!").queue();
            return false;
        } 
        return true;
    }),
    ACCOUNTS_LINKED(ctx -> {
        if (!ctx.bot().isUserLinked(ctx.user())) {
             ctx.deferReply(true).setContent("This command requires your Minecraft account to be linked with your Discord Account!").queue();
            return false;
        }
        return true;
    }),
    SERVER_OP(ctx -> {
        final var isOp = ctx.bot().getLinkedAccount(ctx.user()).map(profile -> ctx.minecraftServer().getPlayerList().isOp(profile)).orElse(false);
        if (Boolean.FALSE.equals(isOp)) {
            ctx.deferReply(true).setContent("This command requires your Minecraft and Discord accounts linked, and requires OP permission levels on the Minecraft server.").queue();
        }
        return isOp;
    });
    
    private final Predicate<SlashCommandContext> predicate;

    private Checks(Predicate<SlashCommandContext> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(SlashCommandContext ctx) {
        return predicate.test(ctx);
    }

}