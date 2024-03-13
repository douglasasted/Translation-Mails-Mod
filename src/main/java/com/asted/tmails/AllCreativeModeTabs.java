package com.asted.tmails;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class AllCreativeModeTabs
{
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TMails.ID);

    public static final RegistryObject<CreativeModeTab> TMAILS_CREATIVE_TAB = TABS.register("tmails_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("TMails"))
                    .icon(AllItems.WRITABLE_MAIL.get()::getDefaultInstance)
                    .displayItems((displayParameters, output) -> {
                        output.accept(AllItems.WRITABLE_MAIL.get());
                    })
                    .build());

    public static void register(IEventBus eventBus)
    {
        TABS.register(eventBus);
    }
}
