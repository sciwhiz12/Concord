package tk.sciwhiz12.concord.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.util.TranslationUtil;

import static net.minecraft.ChatFormatting.GREEN;
import static net.minecraft.ChatFormatting.RED;
import static net.minecraft.commands.Commands.literal;

public class ConcordCommand {
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            literal("concord")
                .then(literal("reload")
                    .requires(source -> source.hasPermission(3))
                    .executes(ConcordCommand::reload)
                )
                .then(literal("enable")
                    .requires(source -> source.hasPermission(3))
                    .executes(ConcordCommand::enable)
                )
                .then(literal("disable")
                    .requires(source -> source.hasPermission(3))
                    .executes(ConcordCommand::disable)
                )
                .then(literal("status")
                    .executes(ConcordCommand::status)
                )
        );
    }

    private static BaseComponent createMessage(CommandSourceStack source, String translation, Object... args) {
        return TranslationUtil.createTranslation((ServerPlayer) source.getEntity(), translation, args);
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ctx.getSource().sendSuccess(createMessage(source, "command.concord.reload"), true);
        if (Concord.isEnabled()) {
            Concord.disable();
        }
        Concord.enable(source.getServer());
        return 1;
    }

    private static int enable(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (Concord.isEnabled()) {
            ctx.getSource().sendFailure(createMessage(source, "command.concord.enable.already_enabled"));
            return 1;
        }
        ctx.getSource().sendSuccess(createMessage(source, "command.concord.enable"), true);
        Concord.enable(source.getServer());
        return 1;
    }

    private static int disable(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!Concord.isEnabled()) {
            ctx.getSource().sendFailure(createMessage(source, "command.concord.disable.already_disabled"));
            return 1;
        }
        ctx.getSource().sendSuccess(createMessage(source, "command.concord.disable"), true);
        Concord.disable();
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Component result;
        if (Concord.isEnabled()) {
            result = createMessage(source, "command.concord.status.enabled").withStyle(GREEN);
        } else {
            result = createMessage(source, "command.concord.status.disabled").withStyle(RED);
        }
        ctx.getSource().sendSuccess(createMessage(source, "command.concord.status", result), false);
        return 1;
    }
}
