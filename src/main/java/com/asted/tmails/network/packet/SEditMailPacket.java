package com.asted.tmails.network.packet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.asted.tmails.AllItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.nbt.ListTag;

import java.util.List;
import java.util.function.Supplier;

public class SEditMailPacket
{
    private final int slot;
    private final List<String> pages;
    private boolean stamp;


    public SEditMailPacket(int _slot, List<String> _pages, boolean _stamp)
    {
        slot = _slot;
        pages = ImmutableList.copyOf(_pages);
        stamp = _stamp;
    }

    // Reader/Decoder
    public SEditMailPacket(FriendlyByteBuf buffer)
    {
        slot = buffer.readVarInt();
        pages = buffer.readCollection(FriendlyByteBuf.limitValue(Lists::newArrayListWithCapacity, 200), (p_182763_) ->
        {
            return p_182763_.readUtf(8192);
        });
        stamp = buffer.readBoolean();
    }

    // Writer/Encoder
    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(slot);
        buffer.writeCollection(pages, (p_182759_, p_182760_) ->
        {
            p_182759_.writeUtf(p_182760_, 8192);
        });
        buffer.writeBoolean(stamp);
    }

    public boolean handle(Supplier<NetworkEvent.Context> handler)
    {
        NetworkEvent.Context context = handler.get();

        context.enqueueWork(() ->
        {
            // Handle edit mail
            if (Inventory.isHotbarSlot(slot) || slot == 40)
            {
                // On the server
                List<String> pagesList = Lists.newArrayList();
                ServerPlayer player = context.getSender();
                ItemStack item = player.getInventory().getItem(slot);

                // Save the mail on the server
                pages.stream().limit(100L).forEach(pagesList::add);
                item.addTagElement("pages", convertListToListTag(pagesList));

                if (stamp)
                {
                    if (item.is(AllItems.WRITABLE_MAIL.get()))
                    {
                        ItemStack mail = new ItemStack(AllItems.COMMON_MAIL.get());
                        CompoundTag mailTag = item.getTag();

                        System.out.println(mailTag);

                        // Copy writable mail pages to written mail
                        if (mailTag != null)
                            mail.setTag(mailTag.copy());

                        mail.addTagElement("author", StringTag.valueOf(player.getName().getString()));
                        mail.addTagElement("uuid", StringTag.valueOf(player.getUUID().toString()));

                        player.getInventory().setItem(slot, mail);
                    }
                }
            }
        });

        return true;
    }

    // There's probably a better way to do this. Will search for it later!
    private ListTag convertListToListTag(List<String> list)
    {
        ListTag listTag = new ListTag();

        int i = 0;

        for(int j = list.size(); i < j; ++i)
        {
            String s = list.get(i);
            listTag.add(StringTag.valueOf(s));
        }

        return listTag;
    }
}
