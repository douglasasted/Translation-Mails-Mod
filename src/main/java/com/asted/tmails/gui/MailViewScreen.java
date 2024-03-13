package com.asted.tmails.gui;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.asted.tmails.AllConfigs;
import com.asted.tmails.AllItems;
import com.asted.tmails.TMails;
import com.asted.tmails.TranslationManager;
import com.asted.tmails.item.WritableMailItem;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class MailViewScreen extends Screen
{
    // Default configs
    private static final ResourceLocation MAIL_TEXTURE = new ResourceLocation(TMails.ID, "textures/gui/common_mail.png");
    private static final ResourceLocation BOX_TEXTURE = new ResourceLocation(TMails.ID, "textures/gui/common_mail.png");

    private int currentPage;

    private MailAccess mailAccess;

    // Widgets
    private Button translateButton;
    private Button closeButton;
    private Button forwardButton;
    private Button backButton;

    // Book access in case there's no previous access
    public static final MailAccess EMPTY_ACCESS = new MailAccess()
    {
        public int getPageCount()
        {
            return 0;
        }

        public FormattedText getPageRaw(int index)
        {
            return FormattedText.EMPTY;
        }
    };

    /** Holds a copy of the page text, split into page width lines */
    private List<FormattedCharSequence> cachedPageComponents = Collections.emptyList();
    private List<List<FormattedCharSequence>> cachedTranslatedPageComponents = new ArrayList<>(4);
    private int cachedPage = -1;
    private Component pageMsg = CommonComponents.EMPTY;
    private boolean playTurnSound;


    // Mail & Translation info
    String[] languagesArray = new String[]{"English", "Spanish", "Portuguese", "French"};
    Future[] languagesFuture = new Future[4];


    // Threading
    private static final ExecutorService executor;

    static
    {
        executor = Executors.newFixedThreadPool(8);
    }

    // Constructors

    // Previously with book access constructor
    public MailViewScreen(MailAccess _mailAccess)
    {
        this(_mailAccess, true);
    }

    // No book access constructor
    public MailViewScreen()
    {
        this(EMPTY_ACCESS, false);
    }

    private MailViewScreen(MailAccess _mailAccess, boolean _playTurnSound)
    {
        super(GameNarrator.NO_TITLE);
        mailAccess = _mailAccess;
        playTurnSound = _playTurnSound;

        // Creating translation cached page components
        for (int i = 0; i < languagesArray.length; i++)
            cachedTranslatedPageComponents.add(Collections.emptyList());
    }


    // Rendering

    @Override
    protected void init()
    {
        super.init();

        // Generate Translation button widget
        Button _translateButton = Button.builder(
                        Component.translatable("gui.tmails.translate_button"),
                        this::handleTranslateButton)
                .bounds(this.width / 2 - 50, 175, 100, 16).build();
        closeButton = addRenderableWidget(_translateButton);

        // Close button widget
        Button _closeButton = Button.builder(
            Component.translatable("gui.tmails.close_button"),
            this::handleCloseButton)
            .bounds(this.width / 2 - 50, 196, 100, 16).build();
        closeButton = addRenderableWidget(_closeButton);

        // Page buttons widgets
        int i = (width - 178) / 2;
        Button _forwardButton = new PageButton(i + 114, 146, true, (p) -> this.pageForward(), playTurnSound);
        Button _backButton = new PageButton(i + 41, 146, false, (p) -> this.pageBack(), playTurnSound);

        forwardButton = this.addRenderableWidget(_forwardButton);
        backButton = this.addRenderableWidget(_backButton);

        updateButtonVisibility();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        int center = (width - 110) / 2 + 15;


        // Transparent black background
        renderBackground(graphics);


        // Updating cache
        if (cachedPage != currentPage)
        {
            FormattedText formattedPage = mailAccess.getPage(currentPage);

            // Get the translation for each other translation box
            // Only if auto translate on new page is on
            if (AllConfigs.AUTO_TRANSLATE.get())
                fullTranslate(formattedPage.getString());

            cachedPageComponents = font.split(formattedPage, 114);
            pageMsg = Component.translatable("book.pageIndicator", currentPage + 1, Math.max(getNumPages(), 1));
        }


        // Main pages/books render
        renderMailBoxes(graphics, center);


        // Rendering page contents
        renderPagesText(graphics, center);


        PoseStack ms = graphics.pose();

        ms.pushPose();
        ms.translate(center + 30, 135, 0);
        ms.scale(2.8f, 2.8f, 1);

        ResourceLocation playerHeadTexture = getSkin(((WrittenMailAccess) mailAccess).getUuid());
        graphics.blit(playerHeadTexture, 0, 0, 0, 8, 8, 8, 8, 64, 64);

        ms.popPose();

        // Mouse hover over text
        //Style style = getClickedComponentStyleAt((double) mouseX, (double) mouseY);
        //if (style != null)
        //    graphics.renderComponentHoverEffect(font, style, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
    }


    // Special renders functions

    private void renderMailBoxes(GuiGraphics graphics, int center)
    {
        PoseStack ms = graphics.pose();

        final int boxWidth = 110;

        ms.pushPose();
        ms.translate(center, 0, 0);
        ms.scale(0.74f, 0.74f, 0.74f);

        // Layout
        graphics.blit(MAIL_TEXTURE, 0, 27, 0, 0, boxWidth, 190);

        // Translation boxes
        graphics.blit(BOX_TEXTURE, 115, 27, 120, 0, 125, 190);
        graphics.blit(BOX_TEXTURE, 115 * 2, 27, 120, 0, 125, 190);
        graphics.blit(BOX_TEXTURE, -115, 27, 120, 0, 125, 190);
        graphics.blit(BOX_TEXTURE, -115 * 2, 27, 120, 0, 125, 190);

        ms.popPose();
    }

    private void renderPagesText(GuiGraphics graphics, int center)
    {
        PoseStack ms = graphics.pose();

        ms.pushPose();
        ms.translate(center + 2, 0, 0);
        ms.scale(0.68f, 0.68f, 1);


        int textColor = 0;
        cachedPage = currentPage;
        graphics.drawString(font, pageMsg, 0, 18, 16777215, false); // Drawing "Page x of x" on screen
        int k = Math.min(128 / 9, cachedPageComponents.size());


        // Drawing the page text on screen

        // Original text
        for (int line = 0; line < k; ++line)
        {
            FormattedCharSequence formattedCharPage = cachedPageComponents.get(line);
            graphics.drawString(font, formattedCharPage, 0, 32 + line * 9, textColor, false);
        }

        // Translation boxes
        int indOffset = 125;
        int offset[] = new int[] {-indOffset * 2, -indOffset, indOffset, indOffset * 2};

        for (int i = 0; i < cachedTranslatedPageComponents.size(); i++)
        {
            k = Math.min(128 / 9, cachedTranslatedPageComponents.get(i).size());
            graphics.drawString(font, languagesArray[i], offset[i], 18, 16777215, false); // Drawing language title

            for (int line = 0; line < k; ++line)
            {
                FormattedCharSequence formattedCharPage = cachedTranslatedPageComponents.get(i).get(line);
                graphics.drawString(font, formattedCharPage, offset[i], 32 + line * 9, textColor, false);
            }
        }

        ms.popPose();
    }

    public static ResourceLocation getSkin(UUID uuid)
    {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();

        if (connection == null)
            return DefaultPlayerSkin.getDefaultSkin(uuid);

        PlayerInfo playerInfo = connection.getPlayerInfo(uuid);

        if (playerInfo == null)
            return DefaultPlayerSkin.getDefaultSkin(uuid);

        return playerInfo.getSkinLocation();
    }


    // Translation function

    public void translateSync(String text, int language) throws ExecutionException, InterruptedException
    {
        Future<String> futureAnswer = executor.submit(new TranslationManager.TranslationTask(languagesArray[language], text));

        System.out.println(languagesArray[language]);

        String translation = futureAnswer.get();

        if (translation == null)
        {
            FormattedText formattedError = FormattedText.of(Component.translatable("translation.tmails.general_error").getString());

            cachedTranslatedPageComponents.set(language, font.split(formattedError, 114));

            return;
        }

        FormattedText formattedTranslation = FormattedText.of(translation);

        cachedTranslatedPageComponents.set(language, font.split(formattedTranslation, 114));
    }

    private void translate(String text, int language)
    {
        Future translation = executor.submit(() ->
        {
            try
            {
                translateSync(text, language);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        languagesFuture[language] = translation;
    }

    private void fullTranslate(String text)
    {
        for (int i = 0; i < cachedTranslatedPageComponents.size(); i++)
        {
            if (languagesFuture[i] != null)
                languagesFuture[i].cancel(true);

            translate(text, i);
        }
    }


    // General Functions

    /**
     * Forward button is only visible if there's more than one page<p>
     * Back button is only visible if the current page is not the first page
     */
    private void updateButtonVisibility()
    {
        forwardButton.visible = currentPage < getNumPages() - 1;
        backButton.visible = currentPage > 0;
    }

    private int getNumPages()
    {
        return mailAccess.getPageCount();
    }

    static List<String> loadPages(CompoundTag pagesTag)
    {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        loadPages(pagesTag, builder::add);

        return builder.build();
    }

    private static void loadPages(CompoundTag pageTags, Consumer<String> consumer)
    {
        ListTag pageListTag = pageTags.getList("pages", 8).copy();
        IntFunction<String> pagesIntFunction;

        pagesIntFunction = pageListTag::getString;

        for(int i = 0; i < pageListTag.size(); ++i)
            consumer.accept(pagesIntFunction.apply(i));
    }

    public void setMailAccess(MailAccess _mailAccess)
    {
        mailAccess = _mailAccess;

        currentPage = Mth.clamp(currentPage, 0, mailAccess.getPageCount());
        updateButtonVisibility();

        cachedPage = -1;
    }


    // Button Functions

    private void handleTranslateButton(Button button)
    {
        // Translate all the boxes
        fullTranslate(mailAccess.getPage(currentPage).getString());
    }

    private void handleCloseButton(Button button)
    {
        this.onClose();
    }

    private void pageForward()
    {
        if (this.currentPage < this.getNumPages() - 1)
            ++this.currentPage;

        this.updateButtonVisibility();
    }

    private void pageBack()
    {
        if (this.currentPage > 0)
            --this.currentPage;

        updateButtonVisibility();
    }


    // Book accesses

    public interface MailAccess
    {
        // Returns the size of the book
        int getPageCount();

        FormattedText getPageRaw(int index);

        // Get intended page
        default FormattedText getPage(int page)
        {
            return page >= 0 && page <= getPageCount() ? getPageRaw(page) : FormattedText.EMPTY;
        }

        // Get the mail access from the item
        static MailAccess fromItem(ItemStack item)
        {
            if (item.is(AllItems.COMMON_MAIL.get()))
                return new WrittenMailAccess(item);

            return (MailAccess) (item.is(AllItems.WRITABLE_MAIL.get()) ? new WritableMailAccess(item) : EMPTY_ACCESS);
        }

        static String getOwner()
        {
            return "?";
        }

        static UUID getUuid()
        {
            return null;
        }
    }

    public static class WritableMailAccess implements MailAccess
    {
        private final List<String> pages;

        public WritableMailAccess(ItemStack item)
        {
            pages = readPages(item);
        }

        private static List<String> readPages(ItemStack writableMail)
        {
            CompoundTag pagesTag = writableMail.getTag();
            return (List<String>) (pagesTag != null ? loadPages(pagesTag) : ImmutableList.of());
        }

        public int getPageCount()
        {
            return pages.size();
        }

        @Override
        public FormattedText getPageRaw(int index)
        {
            return FormattedText.of(pages.get(index));
        }
    }

    public static class WrittenMailAccess implements MailAccess
    {
        public final String owner;
        @Getter
        public final UUID uuid;

        private final List<String> pages;

        public WrittenMailAccess(ItemStack mailStack)
        {
            pages = readPages(mailStack);

            owner = mailStack.getTag().getString("author");
            uuid = UUID.fromString(mailStack.getTag().getString("uuid"));
        }

        private static List<String> readPages(ItemStack mailStack)
        {
            CompoundTag mailTags = mailStack.getTag();

            return (mailTags != null && WritableMailItem.makeSureTagIsValid(mailTags) ?
                    MailViewScreen.loadPages(mailTags) :
                    ImmutableList.of(Component.Serializer.toJson(Component.translatable("mail.tmails.invalid").withStyle(ChatFormatting.DARK_RED))));
        }

        public int getPageCount()
        {
            return pages.size();
        }

        public FormattedText getPageRaw(int index)
        {
            String pagesText = pages.get(index);

            try
            {
                FormattedText formattedPages = Component.Serializer.fromJson(pagesText);

                if (formattedPages != null)
                    return formattedPages;
            }
            catch (Exception e) {}

            return FormattedText.of(pagesText);
        }
    }
}
