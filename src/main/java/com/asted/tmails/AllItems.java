package com.asted.tmails;

import com.asted.tmails.item.WritableMailItem;
import com.asted.tmails.item.WrittenMailItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class AllItems
{
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TMails.ID);

    public static final RegistryObject<Item> WRITABLE_MAIL = ITEMS.register("writable_mail",
            () -> new WritableMailItem((new Item.Properties()).stacksTo(1)));

    public static final RegistryObject<Item> COMMON_MAIL = ITEMS.register("common_mail",
            () -> new WrittenMailItem((new Item.Properties()).stacksTo(16)));

    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
