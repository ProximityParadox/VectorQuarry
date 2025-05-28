package com.nicholasblue.quarrymod.item;

import com.nicholasblue.quarrymod.QuarryMod;
import com.nicholasblue.quarrymod.registry.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, QuarryMod.MODID);

    public static final RegistryObject<Item> QUARRY_BLOCK =
            ITEMS.register("quarry_block",
                    () -> new BlockItem(ModBlocks.QUARRY_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> UPGRADE1 =
            ITEMS.register("upgrade1", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> UPGRADE2 =
            ITEMS.register("upgrade2", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> UPGRADE3 =
            ITEMS.register("upgrade3", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DRILL_BIT_BLUE =
            ITEMS.register("drill_bit_blue", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DRILL_BIT_GREEN =
            ITEMS.register("drill_bit_green", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DRILL_BIT_RED =
            ITEMS.register("drill_bit_red", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DRILL_BIT_OBI =
            ITEMS.register("drill_bit_obi", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DRILL_BIT_NETH =
            ITEMS.register("drill_bit_neth", () -> new Item(new Item.Properties()));


    public static void register() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        bus.addListener(ModItems::populateCreativeTabs);
    }

    private static void populateCreativeTabs(BuildCreativeModeTabContentsEvent evt) {
        if (evt.getTabKey() == net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            evt.accept(QUARRY_BLOCK.get());
            evt.accept(UPGRADE1.get());
            evt.accept(UPGRADE2.get());
            evt.accept(UPGRADE3.get());
            evt.accept(DRILL_BIT_BLUE.get());
            evt.accept(DRILL_BIT_GREEN.get());
            evt.accept(DRILL_BIT_RED.get());
            evt.accept(DRILL_BIT_OBI.get());
            evt.accept(DRILL_BIT_NETH.get());
        }
    }
}

