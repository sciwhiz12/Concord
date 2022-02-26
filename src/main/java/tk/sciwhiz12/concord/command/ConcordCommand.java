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

package tk.sciwhiz12.concord.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

import net.dv8tion.jda.api.requests.RestAction;
import net.minecraftforge.event.RegisterCommandsEvent;
import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.ConcordConfig;
import tk.sciwhiz12.concord.util.TranslationUtil;

import static net.minecraft.ChatFormatting.GREEN;
import static net.minecraft.ChatFormatting.RED;
import static net.minecraft.commands.Commands.literal;

import java.util.List;
import java.util.stream.Collectors;

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
                .then(literal("clear_discord_commands")
                    .requires(source -> source.hasPermission(Commands.LEVEL_ADMINS))
                    .executes(ConcordCommand::clearDiscordCommands)
                )
                .then(literal("link")
                    .executes(ConcordCommand::generateLinkCode)
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
    
    private static int clearDiscordCommands(CommandContext<CommandSourceStack> ctx) {
        final var source = ctx.getSource();
        if (!Concord.isEnabled()) {
            source.sendFailure(createMessage(source, "command.concord.integraton_disabled"));
            return Command.SINGLE_SUCCESS;
        }
        final var guild = Concord.BOT.getDiscord().getGuildById(ConcordConfig.GUILD_ID.get());
        guild.retrieveCommands()
            .flatMap(cmds -> RestAction.accumulate(cmds.stream()
                    .map(cmd -> guild.deleteCommandById(cmd.getId())).toList(), 
                    Collectors.toList()))
            .onErrorMap(t -> {
                source.sendFailure(createMessage(source, "command.concord.clear_discord.exception"));
                Concord.LOGGER.error("Exception while trying to clear Discord commands.", t);
                return List.of();
            })
            .queue($ -> source.sendSuccess(createMessage(source, "command.concord.clear_discord.success", $.size()), true));
        return Command.SINGLE_SUCCESS;
    }
    
    private static int generateLinkCode(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final var source = ctx.getSource();
        if (!Concord.isEnabled()) {
            source.sendFailure(createMessage(source, "command.concord.integraton_disabled"));
            return Command.SINGLE_SUCCESS;
        }
        final var code = Concord.BOT.getLinkedUsers().generateLinkCode(source.getPlayerOrException().getUUID());
        source.sendSuccess(createMessage(source, "command.concord.link", 
                new TextComponent(String.valueOf(code)).withStyle(s -> s.withColor(ChatFormatting.GOLD).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.valueOf(code)))),
                new TextComponent("/link").withStyle(s -> s.withColor(ChatFormatting.GREEN))), false);
        return Command.SINGLE_SUCCESS;
    }
}
