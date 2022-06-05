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

package tk.sciwhiz12.concord.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.ConcordConfig;
import tk.sciwhiz12.concord.msg.Messaging;
import tk.sciwhiz12.concord.util.Messages;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class EmoteCommandHook {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        if (!ConcordConfig.SAY_COMMAND_HOOK.get()) return;

        LOGGER.debug("Hooking into /me command");
        event.getDispatcher().register(literal("me")
                .then(argument("action", greedyString())
                        .executes(EmoteCommandHook::execute)
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        String action = StringArgumentType.getString(ctx, "action");
        Entity entity = ctx.getSource().getEntity();
        MinecraftServer server = ctx.getSource().getServer();
        if (entity != null) {
            if (entity instanceof ServerPlayer player) {
                player.getTextFilter().processStreamMessage(action).thenAcceptAsync((filteredText) -> {
                    String text = filteredText.getFiltered();
                    Component filteredMessage = text.isEmpty() ? null : createMessage(ctx, text);
                    Component rawMessage = createMessage(ctx, filteredText.getRaw());
                    server.getPlayerList().broadcastMessage(rawMessage,
                            (target) -> player.shouldFilterMessageTo(target) ? filteredMessage : rawMessage,
                            ChatType.CHAT, entity.getUUID());

                    sendMessage(ctx, filteredText.getRaw());
                }, server);
                return Command.SINGLE_SUCCESS;
            }

            server.getPlayerList().broadcastMessage(createMessage(ctx, action), ChatType.CHAT, entity.getUUID());
        } else {
            server.getPlayerList().broadcastMessage(createMessage(ctx, action), ChatType.SYSTEM, Util.NIL_UUID);
        }
        sendMessage(ctx, action);

        return Command.SINGLE_SUCCESS;
    }

    private static Component createMessage(CommandContext<CommandSourceStack> ctx, String action) {
        return new TranslatableComponent("chat.type.emote", ctx.getSource().getDisplayName(), action);
    }

    private static void sendMessage(CommandContext<CommandSourceStack> ctx, String message) {
        try {
            if (Concord.isEnabled() && ConcordConfig.COMMAND_EMOTE.get()) {
                Messaging.sendToChannel(Concord.getBot().getDiscord(),
                        Messages.EMOTE_COMMAND.component(ctx.getSource().getDisplayName(), message).getString());
            }
        } catch (Exception e) {
            LOGGER.warn("Exception from command hook; ignoring to continue command execution", e);
        }
    }
}
