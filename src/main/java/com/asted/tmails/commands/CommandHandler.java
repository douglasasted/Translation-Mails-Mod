package com.asted.tmails.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

@OnlyIn(Dist.CLIENT)
public class CommandHandler
{
    public static void registerCommands (RegisterClientCommandsEvent event)
    {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // Old Debug Ask Command

        //LiteralArgumentBuilder builder = Commands.literal("tmails")
        //        .then(Commands.literal("translate")
        //        .then(Commands.argument("prompt", StringArgumentType.greedyString())
        //        .executes(context ->
        //        {
        //            LocalPlayer player = Minecraft.getInstance().player;
        //
        //            ClientCommandSourceStack source = (ClientCommandSourceStack) context.getSource();
        //            String toTranslateText = StringArgumentType.getString(context, "prompt");
        //
        //            Supplier<Component> successMessage = () -> Component.literal("§7<" + player.getDisplayName().getString() + "> " + toTranslateText);
        //
        //            source.sendSuccess(successMessage, false);
        //
        //            // Ask chat gpt question
        //            TranslationManager.fullTranslate(toTranslateText);
        //
        //            return 0;
        //        })));
        //
        //dispatcher.register(builder);
    }
}
