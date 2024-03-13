package com.asted.tmails.network;

import com.asted.tmails.TMails;
import com.asted.tmails.network.packet.COpenMailPacket;
import com.asted.tmails.network.packet.SEditMailPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler
{
    private static final SimpleChannel INSTANCE = NetworkRegistry.ChannelBuilder.named(
            new ResourceLocation(TMails.ID, "main"))
            .serverAcceptedVersions((status) -> true)
            .clientAcceptedVersions((status) -> true)
            .networkProtocolVersion(() -> "1.0")
            .simpleChannel();

    public static void register()
    {
        INSTANCE.messageBuilder(SEditMailPacket.class, NetworkDirection.PLAY_TO_SERVER.ordinal())
                .encoder(SEditMailPacket::write)
                .decoder(SEditMailPacket::new)
                .consumerMainThread(SEditMailPacket::handle)
                .add();

        INSTANCE.messageBuilder(COpenMailPacket.class, NetworkDirection.PLAY_TO_CLIENT.ordinal())
                .encoder(COpenMailPacket::write)
                .decoder(COpenMailPacket::new)
                .consumerMainThread(COpenMailPacket::handle)
                .add();
    }

    public static void sendToServer(Object msg)
    {
        INSTANCE.send(PacketDistributor.SERVER.noArg(), msg);
    }

    public static void sendToPlayer(Object msg, ServerPlayer player)
    {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToAllClients(Object msg)
    {
        INSTANCE.send(PacketDistributor.ALL.noArg(), msg);
    }
}
