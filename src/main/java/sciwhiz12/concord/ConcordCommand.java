package sciwhiz12.concord;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.minecraft.command.Commands.literal;
import static net.minecraft.util.text.TextFormatting.GREEN;
import static net.minecraft.util.text.TextFormatting.RED;
import static sciwhiz12.concord.Concord.MODID;
import static sciwhiz12.concord.msg.MessageUtil.createTranslation;
import static sciwhiz12.concord.msg.MessageUtil.isVanillaClient;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ConcordCommand {
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            literal("concord")
                .then(literal("reload")
                    .requires(source -> source.hasPermissionLevel(3))
                    .executes(ConcordCommand::reload)
                )
                .then(literal("enable")
                    .requires(source -> source.hasPermissionLevel(3))
                    .executes(ConcordCommand::enable)
                )
                .then(literal("disable")
                    .requires(source -> source.hasPermissionLevel(3))
                    .executes(ConcordCommand::disable)
                )
                .then(literal("status")
                    .executes(ConcordCommand::status)
                )
        );
    }

    public static int reload(CommandContext<CommandSource> ctx) {
        CommandSource source = ctx.getSource();
        ctx.getSource().sendFeedback(createTranslation(!isVanillaClient(source), "command.concord.reload"), true);
        if (Concord.isEnabled()) {
            Concord.disable();
        }
        Concord.enable();
        return 1;
    }

    public static int enable(CommandContext<CommandSource> ctx) {
        CommandSource source = ctx.getSource();
        if (Concord.isEnabled()) {
            ctx.getSource().sendErrorMessage(
                createTranslation(!isVanillaClient(source), "command.concord.enable.already_enabled"));
            return 1;
        }
        ctx.getSource().sendFeedback(createTranslation(!isVanillaClient(source), "command.concord.enable"), true);
        Concord.enable();
        return 1;
    }

    public static int disable(CommandContext<CommandSource> ctx) {
        CommandSource source = ctx.getSource();
        if (!Concord.isEnabled()) {
            ctx.getSource().sendErrorMessage(
                createTranslation(!isVanillaClient(source), "command.concord.disable.already_disabled"));
            return 1;
        }
        ctx.getSource().sendFeedback(createTranslation(!isVanillaClient(source), "command.concord.disable"), true);
        Concord.disable();
        return 1;
    }

    public static int status(CommandContext<CommandSource> ctx) {
        CommandSource source = ctx.getSource();
        ITextComponent result;
        if (Concord.isEnabled()) {
            result = createTranslation(!isVanillaClient(source), "command.concord.status.enabled").mergeStyle(GREEN);
        } else {
            result = createTranslation(!isVanillaClient(source), "command.concord.status.disabled").mergeStyle(RED);
        }
        ctx.getSource().sendFeedback(createTranslation(!isVanillaClient(source), "command.concord.status", result), true);
        return 1;
    }
}
