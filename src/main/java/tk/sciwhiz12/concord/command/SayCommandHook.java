package tk.sciwhiz12.concord.command;

import com.mojang.brigadier.Command;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.ConcordConfig;
import tk.sciwhiz12.concord.msg.Messaging;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;
import static tk.sciwhiz12.concord.Concord.BOT;

@Mod.EventBusSubscriber(modid = Concord.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SayCommandHook {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        if (!ConcordConfig.SAY_COMMAND_HOOK.get()) return;

        LOGGER.debug("Hooking into /say command");
        event.getDispatcher().register(literal("say")
            .requires((ctx) -> ctx.hasPermissionLevel(2))
            .then(argument("message", MessageArgument.message())
                .executes((ctx) -> {
                    ITextComponent message = MessageArgument.getMessage(ctx, "message");
                    TranslationTextComponent text = new TranslationTextComponent("chat.type.announcement", ctx.getSource().getDisplayName(), message);
                    Entity entity = ctx.getSource().getEntity();
                    if (entity != null) {
                        if (Concord.isEnabled() && ConcordConfig.COMMAND_SAY.get()) {
                            Messaging.sendToChannel(BOT.getDiscord(), text.getString());
                        }
                        ctx.getSource().getServer().getPlayerList().func_232641_a_(text, ChatType.CHAT, entity.getUniqueID());
                    } else {
                        if (Concord.isEnabled() && ConcordConfig.COMMAND_SAY.get()) {
                            Messaging.sendToChannel(BOT.getDiscord(), new TranslationTextComponent("message.concord.command.say", ctx.getSource().getDisplayName(), message).getString());
                        }
                        ctx.getSource().getServer().getPlayerList().func_232641_a_(text, ChatType.SYSTEM, Util.DUMMY_UUID);
                    }

                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }
}
