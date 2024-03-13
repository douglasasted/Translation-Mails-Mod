package com.asted.tmails;

import com.mojang.logging.LogUtils;
import com.asted.tmails.commands.CommandHandler;
import com.asted.tmails.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TMails.ID)
public class TMails
{
    // Default Config
    public static final String ID = "tmails";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TMails()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        AllItems.register(modEventBus);
        AllCreativeModeTabs.register(modEventBus);
        PacketHandler.register(); // May cause an error for not registering correctly

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, AllConfigs.SPEC, "tmails-client.toml");

        MinecraftForge.EVENT_BUS.register(this);
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            TranslationManager.startService();

            MinecraftForge.EVENT_BUS.addListener(TMails.ClientModEvents::onClientJoin);
            MinecraftForge.EVENT_BUS.addListener(CommandHandler::registerCommands);
        }

        private static void onClientJoin(ClientPlayerNetworkEvent.LoggingIn event)
        {
            if (TranslationManager.isAuthorized())
            {
                // Local player instance
                LocalPlayer player = Minecraft.getInstance().player;

                // Debug, shows player that the token is valid
                player.displayClientMessage(Component.translatable("auth.tmails.sucess"), false);
            }
        }
    }
}