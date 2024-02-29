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

package dev.sciwhiz12.concord.mixin;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.sciwhiz12.concord.Concord;
import dev.sciwhiz12.concord.ConcordConfig;
import dev.sciwhiz12.concord.util.Messages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.commands.SayCommand;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SayCommand.class)
public abstract class SayCommandMixin {
    @Unique
    private static final Logger concord$LOGGER = LogUtils.getLogger();

    @Inject(method = "*(Lcom/mojang/brigadier/context/CommandContext;Lnet/minecraft/network/chat/PlayerChatMessage;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/network/chat/ChatType$Bound;)V"))
    private static void concord$injectCommandHook(CommandContext<CommandSourceStack> ctx, PlayerChatMessage message, CallbackInfo ci) {
        if (!ConcordConfig.EMOTE_COMMAND_HOOK.get()) return;

        try {
            if (Concord.isEnabled() && ConcordConfig.COMMAND_SAY.get()) {
                Concord.getBot().messaging().sendToDiscord(
                        Messages.SAY_COMMAND.component(ctx.getSource().getDisplayName(), message.decoratedContent()));
            }
        } catch (Exception e) {
            concord$LOGGER.warn("Exception from command hook; ignoring to continue command execution", e);
        }
    }
}
