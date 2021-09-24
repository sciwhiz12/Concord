package tk.sciwhiz12.concord.command;

import com.mojang.brigadier.Command;
import net.minecraft.Util;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.ConcordConfig;
import tk.sciwhiz12.concord.msg.Messaging;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static tk.sciwhiz12.concord.Concord.BOT;

public class SayCommandHook {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        if (!ConcordConfig.SAY_COMMAND_HOOK.get()) return;

        LOGGER.debug("Hooking into /say command");
        event.getDispatcher().register(literal("say")
            .requires((ctx) -> ctx.hasPermission(2))
            .then(argument("message", MessageArgument.message())
                .executes((ctx) -> {
                    Component message = MessageArgument.getMessage(ctx, "message");
                    TranslatableComponent text = new TranslatableComponent("chat.type.announcement", ctx.getSource().getDisplayName(), message);
                    Entity entity = ctx.getSource().getEntity();
                    if (entity != null) {
                        try {
                            if (Concord.isEnabled() && ConcordConfig.COMMAND_SAY.get()) {
                                Messaging.sendToChannel(BOT.getDiscord(), text.getString());
                            }
                        } catch (Exception e) {
                            LOGGER.warn("Exception from command hook; ignoring to continue command execution", e);
                        }
                        ctx.getSource().getServer().getPlayerList().broadcastMessage(text, ChatType.CHAT, entity.getUUID());
                    } else {
                        try {
                            if (Concord.isEnabled() && ConcordConfig.COMMAND_SAY.get()) {
                                Messaging.sendToChannel(BOT.getDiscord(), new TranslatableComponent("message.concord.command.say", ctx.getSource().getDisplayName(), message).getString());
                            }
                        } catch (Exception e) {
                            LOGGER.warn("Exception from command hook; ignoring to continue command execution", e);
                        }
                        ctx.getSource().getServer().getPlayerList().broadcastMessage(text, ChatType.SYSTEM, Util.NIL_UUID);
                    }

                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }
}
