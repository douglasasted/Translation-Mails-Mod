package com.asted.tmails.item;

import com.asted.tmails.network.PacketHandler;
import com.asted.tmails.network.packet.COpenMailPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class WrittenMailItem extends Item
{
    public WrittenMailItem(Properties pProperties) { super(pProperties); }

    /**
     * Allows items to add custom lines of information to the mouseover description.
     */
    public void appendHoverText(ItemStack mail, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        if (!mail.hasTag())
            return;

        CompoundTag mailTags = mail.getTag();
        String authorName = mailTags.getString("author");

        // Add the name of the author, unless it doesn't have a name
        if (!StringUtil.isNullOrEmpty(authorName))
            tooltip.add(Component.translatable("book.byAuthor", authorName).withStyle(ChatFormatting.GRAY));
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack mail = player.getItemInHand(hand);

        // Increase player statistic
        player.awardStat(Stats.ITEM_USED.get(this));

        if (player instanceof ServerPlayer)
        {
            // Is on server

            // Removed filter parts

            PacketHandler.sendToPlayer(new COpenMailPacket(hand), (ServerPlayer) player);
        }

        return InteractionResultHolder.sidedSuccess(mail, level.isClientSide());
    }
}
