package com.asted.tmails.item;

import com.asted.tmails.gui.MailEditScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class WritableMailItem extends Item
{
    public WritableMailItem(Properties pProperties) { super(pProperties); }

    // Item interaction when pressed right click
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack itemStack = player.getItemInHand(hand);

        // Increase player statistic
        player.awardStat(Stats.ITEM_USED.get(this));

        if (level.isClientSide)
        {
            // May need "DistExecutor.unsafeRunWhenOn();"
            Minecraft.getInstance().setScreen(new MailEditScreen(player, itemStack, hand));
        }

        // Interaction result, causes the hand animation
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    /**
     * Returns {@code true} if the book's NBT Tag List "pages" is valid.
     */
    public static boolean makeSureTagIsValid(@Nullable CompoundTag pageTag)
    {
        if (pageTag == null || !pageTag.contains("pages", 9))
            return false;

        // Get pages list
        ListTag pagesTagList = pageTag.getList("pages", 8);

        for(int i = 0; i < pagesTagList.size(); ++i)
        {
            String page = pagesTagList.getString(i);

            if (page.length() > 32767)
                return false;
        }

        return true;
    }
}
