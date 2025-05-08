package com.nicholasblue.quarrymod.registry;

import com.nicholasblue.quarrymod.QuarryMod;
import com.nicholasblue.quarrymod.block.QuarryBlock;
import com.nicholasblue.quarrymod.blockentity.QuarryBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;


public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, QuarryMod.MODID);

    public static final RegistryObject<BlockEntityType<QuarryBlockEntity>> QUARRY_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("quarry_block_entity",
                    () -> BlockEntityType.Builder.of(QuarryBlockEntity::new, ModBlocks.QUARRY_BLOCK.get()).build(null)
            );
    public static void register() {
        BLOCK_ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());    }
}