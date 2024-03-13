package com.asted.tmails.gui;

import com.google.common.collect.Lists;
import com.asted.tmails.TMails;
import com.asted.tmails.network.PacketHandler;
import com.asted.tmails.network.packet.SEditMailPacket;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class MailEditScreen extends Screen
{
    // Default configs
    private static final ResourceLocation TEXTURE = new ResourceLocation(TMails.ID, "textures/gui/writable_mail.png");


    // Item Info
    private final Player owner;
    private final ItemStack book;
    private final InteractionHand hand;


    // Values
    private int currentPage;
    private int frameTick;

    private final List<String> pages = Lists.newArrayList();

    // Determines if the contents of the book have been modified since it was opened
    private boolean isModified;

    @Nullable private DisplayCache displayCache = DisplayCache.EMPTY;
    private Component pageMsg = CommonComponents.EMPTY;

    /** In milliseconds */
    private long lastClickTime;
    private int lastIndex = -1;


    // Widgets
    private Button stampButton;
    private Button doneButton;
    private Button forwardButton;
    private Button backButton;


    private final TextFieldHelper pageEdit = new TextFieldHelper(this::getCurrentPageText, this::setCurrentPageText, this::getClipboard, this::setClipboard,
        (text) ->
        {
            // Checks if the text doesn't exceed the max length or max word wrap height
            return text.length() < 1024 && this.font.wordWrapHeight(text, 114) <= 128;
        });


    // Constructor
    public MailEditScreen(Player _owner, ItemStack _mail, InteractionHand _hand)
    {
        super(GameNarrator.NO_TITLE);

        owner = _owner;
        book = _mail;
        hand = _hand;

        // Loading all pages
        CompoundTag pageTags = _mail.getTag();
        if (pageTags != null)
            loadPages(pageTags, pages::add);

        // If there's still no pages after page loading, then create first page
        if (this.pages.isEmpty())
            this.pages.add("");
    }


    public void tick()
    {
        super.tick();
        ++frameTick;
    }

    /**
     * Called when a keyboard key is pressed within the GUI element.
     * <p>
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     * @param keyCode the key code of the pressed key.
     * @param scanCode the scan code of the pressed key.
     * @param modifiers the keyboard modifiers.
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (super.keyPressed(keyCode, scanCode, modifiers))
            return true;

        boolean flag = this.bookKeyPressed(keyCode, scanCode, modifiers);
        if (flag)
        {
            clearDisplayCache();
            return true;
        }
        else
            return false;
    }

    /**
     * Called when a character is typed within the GUI element.
     * <p>
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     * @param codePoint the code point of the typed character.
     * @param modifiers the keyboard modifiers.
     */
    public boolean charTyped(char codePoint, int modifiers)
    {
        if (super.charTyped(codePoint, modifiers))
            return true;

        if (SharedConstants.isAllowedChatCharacter(codePoint))
        {
            pageEdit.insertText(Character.toString(codePoint));
            clearDisplayCache();
            return true;
        }

        return false;
    }


    // Widgets & Rendering

    @Override
    protected void init()
    {
        // Clear current text
        clearDisplayCache();


        // Close button widget
        Button _closeButton = Button.builder(
            Component.translatable("gui.tmails.done_button"),
            this::handleCloseButton)
            .bounds(this.width / 2 + 2, 196, 98, 20).build();

        // Stamp button widget
        Button _stampButton = Button.builder(
            Component.translatable("gui.tmails.stamp_button"),
            this::handleStampButton)
            .bounds(this.width / 2 - 100, 196, 98, 20).build();

        // Page buttons widgets
        int i = (this.width - 185) / 2;
        Button _forwardButton = new PageButton(i + 116, 159, true, (p) -> pageForward(), true);
        Button _backButton = new PageButton(i + 43, 159, false, (p) -> pageBack(), true);

        // Adding widgets
        doneButton = addRenderableWidget(_closeButton);
        stampButton = addRenderableWidget(_stampButton);
        forwardButton = addRenderableWidget(_forwardButton);
        backButton = addRenderableWidget(_backButton);


        updateButtonVisibility();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        this.setFocused(null);

        // Transparent black background
        renderBackground(graphics);

        // Layout
        int i = (this.width - 135) / 2 + 2;
        graphics.blit(TEXTURE, i - 3, 6, 0, 0, 135, 192);

        // Render the display cache on screen book
        int j1 = font.width(pageMsg);

        graphics.drawString(font, pageMsg, i - j1 + 65, 22, 0, false);
        DisplayCache _displayCache = getDisplayCache(); // Get current display cache

        // Render each line
        for (LineInfo lineIfo : _displayCache.lines)
            graphics.drawString(font, lineIfo.asComponent, lineIfo.x, lineIfo.y, -16777216, false);

        // Other display cache renders
        renderHighlight(graphics, _displayCache.selection);
        renderCursor(graphics, _displayCache.cursor, _displayCache.cursorAtEnd);


        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderCursor(GuiGraphics graphics, Pos2i cursor, boolean cursorAtEnd)
    {
        if (this.frameTick / 6 % 2 == 0)
        {
            cursor = convertLocalToScreen(cursor);

            if (!cursorAtEnd)
                graphics.fill(cursor.x, cursor.y - 1, cursor.x + 1, cursor.y + 9, -16777216);
            else
                graphics.drawString(this.font, "_", cursor.x, cursor.y, 0, false);
        }
    }

    private void renderHighlight(GuiGraphics graphics, Rect2i[] selection)
    {
        for(Rect2i rect2i : selection)
        {
            int i = rect2i.getX();
            int j = rect2i.getY();
            int k = i + rect2i.getWidth();
            int l = j + rect2i.getHeight();

            graphics.fill(RenderType.guiTextHighlight(), i, j, k, l, -16776961);
        }
    }


    // General Functions

    /**
     * Forward button is only visible if there's more than one page<p>
     * Back button is only visible if the current page is not the first page
     */
    private void updateButtonVisibility()
    {
        backButton.visible = currentPage > 0;
    }

    private int getNumPages()
    {
        return pages.size();
    }

    private void appendPageToBook()
    {
        // Only add a new page in case we are at the page limit
        if (getNumPages() >= 100)
            return;

        // Add new page
        pages.add("");
        isModified = true;
    }

    private void loadPages(CompoundTag tag, Consumer<String> consumer)
    {
        ListTag pagesListTag = tag.getList("pages", 8).copy();
        IntFunction<String> intfunction = pagesListTag::getString;

        for(int i = 0; i < pagesListTag.size(); ++i)
            consumer.accept(intfunction.apply(i));
    }

    private void saveChanges(boolean publish)
    {
        // If is not modified don't continue
        if (!isModified)
            return;

        eraseEmptyTrailingPages();
        updateLocalCopy(publish);

        int i = hand == InteractionHand.MAIN_HAND ? owner.getInventory().selected : 40;

        PacketHandler.sendToServer(new SEditMailPacket(i, pages, publish));
    }

    private void updateLocalCopy(boolean publish)
    {
        ListTag pagesListTag = new ListTag();

        // Sending pages as a list tag
        pages.stream().map(StringTag::valueOf).forEach(pagesListTag::add);

        // If the list of pages is empty
        if (!pages.isEmpty())
            book.addTagElement("pages", pagesListTag);

        // If the book is published also put the author
        if (publish)
            book.addTagElement("author", StringTag.valueOf(owner.getGameProfile().getName()));
    }

    // If a page is empty remove it
    private void eraseEmptyTrailingPages()
    {
        // List iterator of the pages
        ListIterator<String> pagesIterator = pages.listIterator(pages.size());

        while (pagesIterator.hasPrevious() && pagesIterator.previous().isEmpty())
            pagesIterator.remove();
    }

    private void changeLine(int pYChange)
    {
        int i = this.pageEdit.getCursorPos();
        int j = this.getDisplayCache().changeLine(i, pYChange);

        this.pageEdit.setCursorPos(j, Screen.hasShiftDown());
    }

    // Select a specific word in the page
    private void selectWord(int index)
    {
        String currentPage = getCurrentPageText();

        pageEdit.setSelectionRange(StringSplitter.getWordPosition(currentPage, -1, index, false), StringSplitter.getWordPosition(currentPage, 1, index, false));
    }


    // Key pressing functions

    /**
     * Handles keypresses, clipboard functions, and page turning
     */
    private boolean bookKeyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (Screen.isSelectAll(keyCode))
        {
            this.pageEdit.selectAll();
            return true;
        }
        else if (Screen.isCopy(keyCode))
        {
            this.pageEdit.copy();
            return true;
        }
        else if (Screen.isPaste(keyCode))
        {
            this.pageEdit.paste();
            return true;
        }
        else if (Screen.isCut(keyCode))
        {
            this.pageEdit.cut();
            return true;
        }
        else
        {
            TextFieldHelper.CursorStep cursorStep = Screen.hasControlDown() ? TextFieldHelper.CursorStep.WORD : TextFieldHelper.CursorStep.CHARACTER;

            switch (keyCode)
            {
                case 257:
                case 335:
                    this.pageEdit.insertText("\n");
                    return true;
                case 259:
                    this.pageEdit.removeFromCursor(-1, cursorStep);
                    return true;
                case 261:
                    this.pageEdit.removeFromCursor(1, cursorStep);
                    return true;
                case 262:
                    this.pageEdit.moveBy(1, Screen.hasShiftDown(), cursorStep);
                    return true;
                case 263:
                    this.pageEdit.moveBy(-1, Screen.hasShiftDown(), cursorStep);
                    return true;
                case 264:
                    this.keyDown();
                    return true;
                case 265:
                    this.keyUp();
                    return true;
                case 266:
                    this.backButton.onPress();
                    return true;
                case 267:
                    this.forwardButton.onPress();
                    return true;
                case 268:
                    this.keyHome();
                    return true;
                case 269:
                    this.keyEnd();
                    return true;
                default:
                    return false;
            }
        }
    }

    private void keyDown()
    {
        changeLine(1);
    }

    private void keyUp()
    {
        changeLine(-1);
    }

    private void keyHome()
    {
        if (Screen.hasControlDown())
            this.pageEdit.setCursorToStart(Screen.hasShiftDown());
        else
        {
            int i = this.pageEdit.getCursorPos();
            int j = this.getDisplayCache().findLineStart(i);
            this.pageEdit.setCursorPos(j, Screen.hasShiftDown());
        }
    }

    private void keyEnd()
    {
        if (Screen.hasControlDown())
            pageEdit.setCursorToEnd(Screen.hasShiftDown());
        else
        {
            DisplayCache displayCache = getDisplayCache();

            int i = this.pageEdit.getCursorPos();
            int j = displayCache.findLineEnd(i);

            pageEdit.setCursorPos(j, Screen.hasShiftDown());
        }
    }

    // Text field helper functions

    /**
     * Returns the contents of the current page as a string (or an empty string if the currPage isn't a valid page index)
     */
    private String getCurrentPageText()
    {
        return currentPage >= 0 && currentPage < pages.size() ? pages.get(currentPage) : "";
    }

    private void setCurrentPageText(String pageText)
    {
        if (currentPage >= 0 && currentPage < pages.size())
        {
            pages.set(currentPage, pageText);

            isModified = true;
            clearDisplayCache();
        }
    }

    private String getClipboard()
    {
        return minecraft != null ? TextFieldHelper.getClipboardContents(minecraft) : "";
    }

    private void setClipboard(String text)
    {
        if (minecraft != null)
            TextFieldHelper.setClipboardContents(minecraft, text);
    }


    // Button Functions

    private void handleCloseButton(Button button)
    {
        this.minecraft.setScreen((Screen)null);
        saveChanges(false);
    }

    private void handleStampButton(Button button)
    {
        // If the stamp is being called, the book has been modified
        isModified = true;

        // Save book changes
        saveChanges(true);

        // Close current screen
        minecraft.setScreen(null);
    }

    private void pageForward()
    {
        // Go to the next, in case we are not on the last page
        if (this.currentPage < getNumPages() - 1)
            ++currentPage;
        // Create a new page at the end of the book
        else
        {
            appendPageToBook();

            // Go to next page, unless we are at the pages limit
            if (this.currentPage < getNumPages() - 1)
                ++currentPage;
        }

        updateButtonVisibility();
        clearDisplayCacheAfterPageChange();
    }

    private void pageBack()
    {
        // Put the mail one page back, in case there's a page to go back
        if (this.currentPage > 0)
            currentPage--;

        updateButtonVisibility();
        clearDisplayCacheAfterPageChange();
    }


    // Mouse functions

    /**
     * Called when a mouse button is clicked within the GUI element.
     * <p>
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     * @param x the X coordinate of the mouse.
     * @param y the Y coordinate of the mouse.
     * @param button the button that was clicked.
     */
    public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button))
            return true;

        if (button == 0)
        {
            long i = Util.getMillis();
            DisplayCache displayCache = this.getDisplayCache();
            int j = displayCache.getIndexAtPosition(font, this.convertScreenToLocal(new Pos2i((int)x, (int)y)));

            if (j >= 0)
            {
                if (j == this.lastIndex && i - this.lastClickTime < 250L)
                {
                    if (!this.pageEdit.isSelecting())
                        selectWord(j);
                    else
                        pageEdit.selectAll();
                }
                else
                    this.pageEdit.setCursorPos(j, Screen.hasShiftDown());

                this.clearDisplayCache();
            }

            lastIndex = j;
            lastClickTime = i;
        }

        return true;
    }


    /**
     * Called when the mouse is dragged within the GUI element.
     * <p>
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     * @param x the X coordinate of the mouse.
     * @param y the Y coordinate of the mouse.
     * @param button the button that is being dragged.
     * @param dragX the X distance of the drag.
     * @param dragY the Y distance of the drag.
     */
    public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
        if (super.mouseDragged(x, y, button, dragX, dragY))
            return true;
        else
        {
            if (button == 0)
            {
                DisplayCache displayCache = getDisplayCache();
                int i = displayCache.getIndexAtPosition(this.font, this.convertScreenToLocal(new Pos2i((int)x, (int)y)));

                pageEdit.setCursorPos(i, true);

                clearDisplayCache();
            }

            return true;
        }
    }

    // Display cache functions

    private void clearDisplayCache()
    {
        displayCache = null;
    }

    private void clearDisplayCacheAfterPageChange()
    {
        pageEdit.setCursorToEnd();
        clearDisplayCache();
    }

    private DisplayCache getDisplayCache()
    {
        if (displayCache == null)
        {
            displayCache = rebuildDisplayCache();
            pageMsg = Component.translatable("book.pageIndicator", this.currentPage + 1, this.getNumPages());
        }

        return displayCache;
    }

    private DisplayCache rebuildDisplayCache()
    {
        String pageText = getCurrentPageText();

        // In case the page text is empty, there's also no display cache
        if (pageText.isEmpty())
            return DisplayCache.EMPTY;


        int i = pageEdit.getCursorPos();
        int j = pageEdit.getSelectionPos();

        MutableBoolean cursorAtEnd = new MutableBoolean();

        IntList posList = new IntArrayList();
        List<LineInfo> lineList = Lists.newArrayList();

        StringSplitter stringSplitter = font.getSplitter();
        MutableInt pageLineSize = new MutableInt();

        // Split the full page text into lines
        stringSplitter.splitLines(pageText, 114, Style.EMPTY, true, (style, leftBorder, rightBorder) ->
        {
            int currentLine = pageLineSize.getAndIncrement();
            String line = pageText.substring(leftBorder, rightBorder);

            // Does this lines ends in a break?
            cursorAtEnd.setValue(line.endsWith("\n"));
            // Removes \n from the end of the string
            String text = StringUtils.stripEnd(line, " \n");
            //Convert local position to screen position
            Pos2i screenPos = convertLocalToScreen(new Pos2i(0, currentLine * 9));
            // Add current line position list of positions
            posList.add(leftBorder);
            // Add new line to line lists
            lineList.add(new LineInfo(style, text, screenPos.x, screenPos.y));
        });


        int[] posArray = posList.toIntArray();
        boolean isCursorAtEnd = i == pageText.length(); // Is the cursor positioned at the end of the page?

        Pos2i cursorPosition;

        // Set the cursor position
        if (isCursorAtEnd && cursorAtEnd.isTrue())
            // Go to following page
            cursorPosition = new Pos2i(0, lineList.size() * 9);
        else
        {
            // Cursor position in page
            int k = findLineFromPos(posArray, i);
            int l = font.width(pageText.substring(posArray[k], i));

            cursorPosition = new Pos2i(l, k * 9);
        }


        List<Rect2i> selection = Lists.newArrayList();

        // Create line selection
        if (i != j)
        {
            int l2 = Math.min(i, j);
            int i1 = Math.max(i, j);
            int j1 = findLineFromPos(posArray, l2);
            int k1 = findLineFromPos(posArray, i1);

            if (j1 == k1)
            {
                int l1 = j1 * 9;
                int i2 = posArray[j1];
                selection.add(this.createPartialLineSelection(pageText, stringSplitter, l2, i1, l1, i2));
            }
            else
            {
                int i3 = j1 + 1 > posArray.length ? pageText.length() : posArray[j1 + 1];
                selection.add(this.createPartialLineSelection(pageText, stringSplitter, l2, i3, j1 * 9, posArray[j1]));

                for(int j3 = j1 + 1; j3 < k1; ++j3)
                {
                    int j2 = j3 * 9;
                    String s1 = pageText.substring(posArray[j3], posArray[j3 + 1]);
                    int k2 = (int)stringSplitter.stringWidth(s1);

                    selection.add(this.createSelection(new Pos2i(0, j2), new Pos2i(k2, j2 + 9)));
                }

                selection.add(this.createPartialLineSelection(pageText, stringSplitter, posArray[k1], i1, k1 * 9, posArray[k1]));
            }
        }


        // Return the new rebuild display cache
        return new DisplayCache(pageText, cursorPosition, isCursorAtEnd, posArray, lineList.toArray(new LineInfo[0]), selection.toArray(new Rect2i[0]));
    }


    // Utility functions for display cache

    private Pos2i convertScreenToLocal(Pos2i screenPos)
    {
        return new Pos2i(screenPos.x - (this.width - 192) / 2 - 36, screenPos.y - 32);
    }

    private Pos2i convertLocalToScreen(Pos2i localScreenPos)
    {
        return new Pos2i(localScreenPos.x + (this.width - 192) / 2 + 36, localScreenPos.y + 32);
    }

    static int findLineFromPos(int[] pLineStarts, int pFind)
    {
        int i = Arrays.binarySearch(pLineStarts, pFind);
        return i < 0 ? -(i + 2) : i;
    }

    private Rect2i createPartialLineSelection(String input, StringSplitter splitter, int startPos, int endPos, int y, int lineStart)
    {
        String s = input.substring(lineStart, startPos);
        String s1 = input.substring(lineStart, endPos);

        Pos2i corner1 = new Pos2i((int)splitter.stringWidth(s), y);
        Pos2i corner2 = new Pos2i((int)splitter.stringWidth(s1), y + 9);

        return createSelection(corner1, corner2);
    }

    private Rect2i createSelection(Pos2i corner1, Pos2i corner2)
    {
        Pos2i pos2i = convertLocalToScreen(corner1);
        Pos2i pos2i1 = convertLocalToScreen(corner2);

        int i = Math.min(pos2i.x, pos2i1.x);
        int j = Math.max(pos2i.x, pos2i1.x);
        int k = Math.min(pos2i.y, pos2i1.y);
        int l = Math.max(pos2i.y, pos2i1.y);

        return new Rect2i(i, k, j - i, l - k);
    }


    @OnlyIn(Dist.CLIENT)
    static class DisplayCache
    {
        // Empty display cache, used for new display caches
        static final DisplayCache EMPTY = new DisplayCache("", new Pos2i(0, 0), true, new int[]{0}, new LineInfo[]{new LineInfo(Style.EMPTY, "", 0, 0)}, new Rect2i[0]);

        private final String fullText;
        final Pos2i cursor;
        final boolean cursorAtEnd;
        final int[] lineStarts;
        final LineInfo[] lines;
        final Rect2i[] selection;

        public DisplayCache(String fullText, Pos2i cursor, boolean cursorAtEnd, int[] lineStarts, LineInfo[] lines, Rect2i[] selection)
        {
            this.fullText = fullText;
            this.cursor = cursor;
            this.cursorAtEnd = cursorAtEnd;
            this.lineStarts = lineStarts;
            this.lines = lines;
            this.selection = selection;
        }

        public int getIndexAtPosition(Font font, Pos2i cursorPosition)
        {
            int i = cursorPosition.y / 9;

            if (i < 0)
                return 0;
            else if (i >= this.lines.length)
                return this.fullText.length();
            else
            {
                LineInfo lineInfo = this.lines[i];
                return this.lineStarts[i] + font.getSplitter().plainIndexAtWidth(lineInfo.contents, cursorPosition.x, lineInfo.style);
            }
        }

        public int changeLine(int xChange, int yChange)
        {
            int i = findLineFromPos(this.lineStarts, xChange);
            int j = i + yChange;
            int k;

            if (0 <= j && j < this.lineStarts.length)
            {
                int l = xChange - this.lineStarts[i];
                int i1 = this.lines[j].contents.length();
                k = this.lineStarts[j] + Math.min(l, i1);
            }
            else
                k = xChange;

            return k;
        }

        public int findLineStart(int line)
        {
            int i = findLineFromPos(this.lineStarts, line);
            return this.lineStarts[i];
        }

        public int findLineEnd(int line)
        {
            int i = findLineFromPos(this.lineStarts, line);
            return this.lineStarts[i] + this.lines[i].contents.length();
        }
    }

    // Information for a single line of the book
    @OnlyIn(Dist.CLIENT)
    private static class LineInfo
    {
        final Style style;
        final String contents;
        final Component asComponent;
        final int x;
        final int y;

        // Constructor
        public LineInfo(Style style, String contents, int x, int y)
        {
            this.style = style;
            this.contents = contents;
            this.x = x;
            this.y = y;
            this.asComponent = Component.literal(contents).setStyle(style);
        }
    }

    // Position as two integers
    @OnlyIn(Dist.CLIENT)
    static class Pos2i
    {
        public final int x;
        public final int y;

        // Constructor
        Pos2i(int x, int y)
        {
            this.x = x;
            this.y = y;
        }
    }
}