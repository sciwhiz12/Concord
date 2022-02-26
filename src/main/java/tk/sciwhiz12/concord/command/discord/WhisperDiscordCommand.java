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

package tk.sciwhiz12.concord.command.discord;

import java.util.List;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.jagrosh.jdautilities.command.SlashCommandEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundChatPacket;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import tk.sciwhiz12.concord.ChatBot;
import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.ConcordConfig;
import tk.sciwhiz12.concord.command.discord.checks.ChecksSet;
import tk.sciwhiz12.concord.msg.Messaging;
import tk.sciwhiz12.concord.network.ConcordNetwork;
import tk.sciwhiz12.concord.util.TranslationUtil;
import tk.sciwhiz12.concord.util.UnicodeConversion;

public final class WhisperDiscordCommand extends ConcordSlashCommand {

    public WhisperDiscordCommand(ChatBot bot) {
        super(bot);
        name = "whisper";
        help = "Whipsers a message to a player from the Minecraft server.";
        options = List.of(
            new OptionData(OptionType.STRING, "player", "The player to whisper to.").setRequired(true).setAutoComplete(true),
            new OptionData(OptionType.STRING, "message", "The message to whisper.").setRequired(true)
        );
        checks = ChecksSet.DEFAULT.toBuilder()
            .and(validPlayerChecker("player"))
            .build();
    }

    @Override
    protected void execute0(SlashCommandEvent event) {
        final var player = event.getOption("player", playerResolver);
        var text = event.getOption("message", OptionMapping::getAsString);
        if (Concord.emojifulLoaded()) {
            text = UnicodeConversion.replace(text);
        }
        
        final ConcordConfig.CrownVisibility crownVisibility = ConcordConfig.HIDE_CROWN.get();
        final var textComponent = new TextComponent(text).withStyle(s -> s.withColor(ChatFormatting.WHITE));
        
        class ComponentCreator {
            TranslatableComponent createComponent(boolean withIcons) {
                final var user = Messaging.createUserComponent(withIcons, crownVisibility, event.getMember(), null);
                return (TranslatableComponent) new TranslatableComponent("chat.concord.whisper", user.withStyle(s -> s.withItalic(false)), 
                        textComponent.withStyle(s -> s.withItalic(false))).withStyle(s -> s.withColor(ChatFormatting.GRAY).withItalic(true));
            }
        }
        final var creator = new ComponentCreator();
        
        final var withoutIcons = creator.createComponent(false);
        Supplier<TranslatableComponent> withIcons = Suppliers.memoize(() -> creator.createComponent(true));

        final boolean lazyTranslate = ConcordConfig.LAZY_TRANSLATIONS.get();
        final boolean useIcons = ConcordConfig.USE_CUSTOM_FONT.get();

        MutableComponent sendingText;
        if ((lazyTranslate || useIcons) && ConcordNetwork.isModPresent(player)) {
            TranslatableComponent translate = useIcons ? withIcons.get() : withoutIcons;
            sendingText = lazyTranslate ? translate : TranslationUtil.eagerTranslate(translate);
        } else {
            sendingText = TranslationUtil.eagerTranslate(withoutIcons);
        }
        player.connection.send(new ClientboundChatPacket(sendingText, ChatType.SYSTEM, Util.NIL_UUID));
        event.deferReply(true).setContent("Successfully sent message!").queue();
    }
    
    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        bot.suggestPlayers(event, 10, e -> e.getFocusedOption().getName().equals("player"));
    }

}