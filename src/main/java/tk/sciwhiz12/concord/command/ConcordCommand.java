package tk.sciwhiz12.concord.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.util.TranslationUtil;

import static net.minecraft.command.Commands.literal;
import static net.minecraft.util.text.TextFormatting.GREEN;
import static net.minecraft.util.text.TextFormatting.RED;

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

    private static TextComponent createMessage(CommandSource source, String translation, Object... args) {
        return TranslationUtil.createTranslation((ServerPlayerEntity) source.getEntity(), translation, args);
    }

    private static int reload(CommandContext<CommandSource> ctx) {
        CommandSource source = ctx.getSource();
        ctx.getSource().sendSuccess(createMessage(source, "command.concord.reload"), true);
        if (Concord.isEnabled()) {
            Concord.disable();
        }
        Concord.enable(source.getServer());
        return 1;
    }

    private static int enable(CommandContext<CommandSource> ctx) {
        CommandSource source = ctx.getSource();
        if (Concord.isEnabled()) {
            ctx.getSource().sendFailure(createMessage(source, "command.concord.enable.already_enabled"));
            return 1;
        }
        ctx.getSource().sendSuccess(createMessage(source, "command.concord.enable"), true);
        Concord.enable(source.getServer());
        return 1;
    }

    private static int disable(CommandContext<CommandSource> ctx) {
        CommandSource source = ctx.getSource();
        if (!Concord.isEnabled()) {
            ctx.getSource().sendFailure(createMessage(source, "command.concord.disable.already_disabled"));
            return 1;
        }
        ctx.getSource().sendSuccess(createMessage(source, "command.concord.disable"), true);
        Concord.disable();
        return 1;
    }

    private static int status(CommandContext<CommandSource> ctx) {
        CommandSource source = ctx.getSource();
        ITextComponent result;
        if (Concord.isEnabled()) {
            result = createMessage(source, "command.concord.status.enabled").withStyle(GREEN);
        } else {
            result = createMessage(source, "command.concord.status.disabled").withStyle(RED);
        }
        ctx.getSource().sendSuccess(createMessage(source, "command.concord.status", result), true);
        return 1;
    }
}
