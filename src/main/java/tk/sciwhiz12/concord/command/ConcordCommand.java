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
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sun.jdi.connect.Connector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.ConcordConfig;
import tk.sciwhiz12.concord.util.Translations;

import java.awt.*;
import java.time.Instant;
import java.util.Collections;

import static net.minecraft.ChatFormatting.GREEN;
import static net.minecraft.ChatFormatting.RED;
import static net.minecraft.commands.Commands.literal;

public class ConcordCommand {
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // Concord bot control commands.
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
                        .then(literal("support")
                                .executes((ctx) -> ConcordCommand.support(ctx, Translations.COMMAND_SUPPORT_NO_REASON.resolvedComponent(ctx.getSource())))
                                .then(Commands.argument("reason", MessageArgument.message())
                                        .executes((ctx) -> ConcordCommand.support(ctx, MessageArgument.getMessage(ctx, "reason")))
                        ))
        );
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ctx.getSource().sendSuccess(Translations.COMMAND_ENABLING.resolvedComponent(source), true);
        if (Concord.isEnabled()) {
            Concord.disable();
        }
        Concord.enable(source.getServer());
        return Command.SINGLE_SUCCESS;
    }

    private static int enable(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (Concord.isEnabled()) {
            ctx.getSource().sendFailure(Translations.COMMAND_ALREADY_ENABLED.resolvedComponent(source));
            return Command.SINGLE_SUCCESS;
        }
        ctx.getSource().sendSuccess(Translations.COMMAND_ENABLING.resolvedComponent(source), true);
        Concord.enable(source.getServer());
        return Command.SINGLE_SUCCESS;
    }

    private static int disable(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!Concord.isEnabled()) {
            ctx.getSource().sendFailure(Translations.COMMAND_ALREADY_DISABLED.resolvedComponent(source));
            return Command.SINGLE_SUCCESS;
        }
        ctx.getSource().sendSuccess(Translations.COMMAND_DISABLING.resolvedComponent(source), true);
        Concord.disable();
        return Command.SINGLE_SUCCESS;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Component result;
        if (Concord.isEnabled()) {
            result = Translations.COMMAND_STATUS_ENABLED.resolvedComponent(source).withStyle(GREEN);
        } else {
            result = Translations.COMMAND_STATUS_DISABLED.resolvedComponent(source).withStyle(RED);
        }
        ctx.getSource().sendSuccess(Translations.COMMAND_STATUS_PREFIX.resolvedComponent(source, result), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int support(CommandContext<CommandSourceStack> ctx, Component reason) {
        CommandSourceStack source = ctx.getSource();

        if (Concord.isEnabled() && !ConcordConfig.MODERATOR_ROLE_ID.get().isEmpty()) {
            // We need to ping the moderators, and we can't do it from an embed, and we can't ping anyone unless it's explicitly told to, so..
            // Send the message with the mention
            Concord.BOT.getReportChannel().sendMessage("<@&" + ConcordConfig.MODERATOR_ROLE_ID.get() + ">")
                    .setAllowedMentions(
                            Collections.singleton(Message.MentionType.ROLE)
                    )
                    .mentionRoles(ConcordConfig.MODERATOR_ROLE_ID.get())
                    // And set context too.
                    .setEmbeds(new EmbedBuilder()
                            .setTitle("Support Request")
                            .setDescription("A user has requested the support of a Server Administrator!")
                            .addField("User", source.getTextName(), false)
                            .addField("Reason", reason.getString(), false)
                            .setTimestamp(Instant.now())
                            .setColor(Color.ORANGE)
                            .build()).queue();

            ctx.getSource().sendSuccess(Translations.COMMAND_SUPPORT_SUCCESS.resolvedComponent(source), false);
        } else {
            ctx.getSource().sendSuccess(Translations.COMMAND_SUPPORT_DISABLED.resolvedComponent(source), false);
        }

        return Command.SINGLE_SUCCESS;
    }
}
