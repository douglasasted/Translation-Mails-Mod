package com.asted.tmails.network.packet;

import com.asted.tmails.AllItems;
import com.asted.tmails.gui.MailViewScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class COpenMailPacket
{
    private final InteractionHand hand;

    public COpenMailPacket(InteractionHand _hand)
    {
        hand = _hand;
    }

    // Reader/Decoder
    public COpenMailPacket(FriendlyByteBuf buffer)
    {
        hand = buffer.readEnum(InteractionHand.class);
    }

    // Writer/Encoder
    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeEnum(hand);
    }

    public boolean handle(Supplier<NetworkEvent.Context> handler)
    {
        NetworkEvent.Context context = handler.get();
        LocalPlayer player = Minecraft.getInstance().player;

        context.enqueueWork(() ->
        {
            // Handle open book
            //PacketUtils.ensureRunningOnSameThread(this, context, Minecraft.getInstance());

            ItemStack item = player.getItemInHand(hand);

            if (item.is(AllItems.COMMON_MAIL.get())) {
                Minecraft.getInstance().setScreen(new MailViewScreen(new MailViewScreen.WrittenMailAccess(item)));
            }
        });

        return true;
    }
}