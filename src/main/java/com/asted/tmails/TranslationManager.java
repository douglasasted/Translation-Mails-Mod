package com.asted.tmails;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class TranslationManager
{
    // OpenAi Config
    private static final String token = "";
    private static OpenAiService service;

    // Translation values
    private static final String Prompt = "Translate every text/characters inside the brackets to %s: \"%s\" If it's already on the language just send back the same text. Your answer must only contain the raw translation text without extra characters. In case of errors or other problems continue sending just the translation of the text.";

    // Other
    private static final ExecutorService executor;

    static
    {
        executor = Executors.newFixedThreadPool(4);
    }

    // Start OpenAi Java service using an API key
    public static void startService()
    {
        // Start OpenAi service if token is not empty
        if (!token.isEmpty())
            service = new OpenAiService(token);
    }

    // Get if player has authorization from OpenAi service
    public static boolean isAuthorized()
    {
        if (service == null)
        {
            // Local player instance
            LocalPlayer player = Minecraft.getInstance().player;

            // If there's no API Key token selected send a warning to the player
            if (player != null)
                player.displayClientMessage(Component.translatable("auth.tmails.no_token"), false);

            // Player is not authorized
            return false;
        }

        // Player is authorized
        return true;
    }

    public static class TranslationTask implements Callable<String>
    {
        private String translatedText;

        public TranslationTask(String language, String text)
        {
            System.out.println("Sending prompt: \"" + Prompt.formatted(language, text) + "\"");

            // The request sent to ChatGPT are required to be a list of messages
            List<ChatMessage> messageList = new ArrayList<>();
            // Adding the message
            messageList.add(new ChatMessage("user", Prompt.formatted(language, text)));
            // ChatGPT request for a reply
            ChatCompletionRequest req = ChatCompletionRequest.builder().messages(messageList).model("gpt-3.5-turbo").build();

            try
            {
                // Get reply from OpenAi service
                ChatCompletionResult reply = service.createChatCompletion(req); // All replies
                ChatMessage replyMessage = reply.getChoices().get(0).getMessage(); // First choice from the reply

                translatedText = replyMessage.getContent().replaceAll("^\\s+|\\s+$", "");
            }
            catch (RuntimeException e) // An error occurred on connection with OpenAi
            {
                TMails.LOGGER.error("Error occurred in communication with OpenAI", e);

                // Debug errors

                // The prompt exceeded the quota
                //if(e.getMessage().toLowerCase().contains("exceeded your current quota"))
                //    player.displayClientMessage(Component.translatable("translation.tmails.quota"), false);
                //    // The prompt exceeded the maximum context length
                //else if (e.getMessage().toLowerCase().contains("maximum context length"))
                //    player.displayClientMessage(Component.translatable("translation.tmails.context").setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(e.getMessage())))), false);
                //    // Other errors
                //else
                //    player.displayClientMessage(Component.translatable("translation.tmails.error").setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(e.getMessage())))), false);
            }
        }

        @Override
        public String call() throws Exception
        {
            return translatedText;
        }
    }
}
