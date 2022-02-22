package tk.sciwhiz12.concord.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
                    .requires(source -> source.hasPermission(Commands.LEVEL_ADMINS))
                    .executes(ConcordCommand::reload)
                )
                .then(literal("enable")
                    .requires(source -> source.hasPermission(Commands.LEVEL_ADMINS))
                    .executes(ConcordCommand::enable)
                )
                .then(literal("disable")
                    .requires(source -> source.hasPermission(Commands.LEVEL_ADMINS))
                    .executes(ConcordCommand::disable)
                )
                .then(literal("status")
                    .executes(ConcordCommand::status)
                )
        );
    }

    private static MutableComponent createMessage(CommandSourceStack source, String translation, Object... args) {
        return TranslationUtil.createTranslation((ServerPlayer) source.getEntity(), translation, args);
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ctx.getSource().sendSuccess(createMessage(source, "command.concord.reload"), true);
        if (Concord.isEnabled()) {
            Concord.disable();
        }
        Concord.enable(source.getServer());
        return Command.SINGLE_SUCCESS;
    }

    private static int enable(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (Concord.isEnabled()) {
            ctx.getSource().sendFailure(createMessage(source, "command.concord.enable.already_enabled"));
            return Command.SINGLE_SUCCESS;
        }
        ctx.getSource().sendSuccess(createMessage(source, "command.concord.enable"), true);
        Concord.enable(source.getServer());
        return Command.SINGLE_SUCCESS;
    }

    private static int disable(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!Concord.isEnabled()) {
            ctx.getSource().sendFailure(createMessage(source, "command.concord.disable.already_disabled"));
            return Command.SINGLE_SUCCESS;
        }
        ctx.getSource().sendSuccess(createMessage(source, "command.concord.disable"), true);
        Concord.disable();
        return Command.SINGLE_SUCCESS;
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
        return Command.SINGLE_SUCCESS;
    }
}
