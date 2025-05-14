package com.nicholasblue.quarrymod.util;

import com.nicholasblue.quarrymod.QuarryMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

// This class will hold all your mod's custom TagKeys
public class ModTags {

    // Define block tags within a nested class for organization
    public static class Blocks {
        private static final ResourceLocation NO_BREAK_ID = new ResourceLocation(QuarryMod.MODID, "no_break");
        private static final ResourceLocation NO_SILK_TOUCH_ID = new ResourceLocation(QuarryMod.MODID, "no_silk_touch");

        // Define the public static final TagKey objects

        public static final TagKey<Block> NO_BREAK;
        public static final TagKey<Block> NO_SILK_TOUCH;

        // Use a static initializer block to handle potential exceptions
        static {
            try {

                NO_BREAK = ForgeRegistries.BLOCKS.tags().createTagKey(NO_BREAK_ID);
                NO_SILK_TOUCH = ForgeRegistries.BLOCKS.tags().createTagKey(NO_SILK_TOUCH_ID);

                // Optional: A successful initialization message to System.out or a logger
                System.out.println(QuarryMod.MODID + "Successfully initialized custom block tag keys.");

            } catch (Throwable e) { // Catch Throwable for early initialization safety
                String errorMessage = "[FATAL ERROR - " + QuarryMod.MODID + " ]: Failed to initialize custom block tag keys. This indicates a core issue with game or mod loading. Cannot recover.";

                // *** Use System.err for guaranteed output during early critical errors ***
                System.err.println(errorMessage);
                e.printStackTrace(System.err); // Print stack trace to System.err

                // Re-throw the exception to correctly fail class loading
                throw new RuntimeException(errorMessage, e);
            }
        }
    }

    private ModTags() {}
}