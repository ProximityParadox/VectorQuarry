package com.nicholasblue.quarrymod.registry;

import com.nicholasblue.quarrymod.QuarryMod;
import com.nicholasblue.quarrymod.menu.QuarryMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.IContainerFactory;



public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, QuarryMod.MODID);

    public static final RegistryObject<MenuType<QuarryMenu>> QUARRY_MENU =
            MENUS.register("quarry_menu",
                    () -> IForgeMenuType.create(                        /* Forge helper */
                            (windowId, inv, data) ->                    /* 3-arg factory   */
                                    new QuarryMenu(windowId, inv, data) /* return menu     */
                    ));
    public static void register() {
        MENUS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}