package com.nicholasblue.quarrymod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = "quarrymod", bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    // Exposed to the rest of your mod
    public static final Common COMMON;
    public static final ForgeConfigSpec SPEC;

    static {
        Pair<Common, ForgeConfigSpec> specPair =
                new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = specPair.getLeft();
        SPEC   = specPair.getRight();
    }


    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    /** Holds all of your IntValues (no statics in here!). */
    public static class Common {
        public final ForgeConfigSpec.IntValue PER_BLOCK_ENERGY_COST;
        public final ForgeConfigSpec.IntValue DIFFICULTY;
        public final ForgeConfigSpec.IntValue DIFFICULTY1_TOTAL_LASER_LENGTH;
        public final ForgeConfigSpec.IntValue DIFFICULTY2_BASIC_LASER_LENGTH;
        public final ForgeConfigSpec.IntValue DIFFICULTY2_INTERMEDIATE_LASER_LENGTH;
        public final ForgeConfigSpec.IntValue DIFFICULTY2_ADVANCED_LASER_LENGTH;
        public final ForgeConfigSpec.IntValue DIFFICULTY3_BASIC_REPEATER_LENGTH;
        public final ForgeConfigSpec.IntValue DIFFICULTY3_INTERMEDIATE_REPEATER_LENGTH;
        public final ForgeConfigSpec.IntValue DIFFICULTY3_ADVANCED_REPEATER_LENGTH;
        public final ForgeConfigSpec.IntValue BASE_MINING_SPEED;

        public final ForgeConfigSpec.IntValue UPGRADE1_SPEED_PER_UNIT;
        public final ForgeConfigSpec.DoubleValue UPGRADE1_COST_MULTIPLIER_PER_UNIT;
        public final ForgeConfigSpec.IntValue UPGRADE1_COST_OFFSET_PER_UNIT;

        public final ForgeConfigSpec.IntValue UPGRADE2_SPEED_PER_UNIT;
        public final ForgeConfigSpec.DoubleValue UPGRADE2_COST_MULTIPLIER_PER_UNIT;
        public final ForgeConfigSpec.IntValue UPGRADE2_COST_OFFSET_PER_UNIT;




        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("quarry balance");

            PER_BLOCK_ENERGY_COST = builder
                    .comment("The energy cost (in FE) to mine a single block")
                    .defineInRange("perBlockEnergyCost", 200, 0, Integer.MAX_VALUE);

            DIFFICULTY = builder
                    .comment("Difficulty level for quarry logic: 1 = Easy, 2 = Normal, 3 = Hard")
                    .defineInRange("difficulty", 2, 1, 3);


            // direct assignments: compiler sees these as definite assignments
            DIFFICULTY1_TOTAL_LASER_LENGTH = defineDifficulty1(builder);
            DIFFICULTY2_BASIC_LASER_LENGTH = defineDifficulty2Basic(builder);
            DIFFICULTY2_INTERMEDIATE_LASER_LENGTH = defineDifficulty2Intermediate(builder);
            DIFFICULTY2_ADVANCED_LASER_LENGTH = defineDifficulty2Advanced(builder);
            DIFFICULTY3_BASIC_REPEATER_LENGTH = defineDifficulty3Basic(builder);
            DIFFICULTY3_INTERMEDIATE_REPEATER_LENGTH = defineDifficulty3Intermediate(builder);
            DIFFICULTY3_ADVANCED_REPEATER_LENGTH = defineDifficulty3Advanced(builder);
            BASE_MINING_SPEED = defineBaseMiningSpeed(builder);

            UPGRADE1_SPEED_PER_UNIT = defineUpgrade1Speed(builder);
            UPGRADE1_COST_MULTIPLIER_PER_UNIT = defineUpgrade1Multiplier(builder);
            UPGRADE1_COST_OFFSET_PER_UNIT = defineUpgrade1Offset(builder);

            UPGRADE2_SPEED_PER_UNIT = defineUpgrade2Speed(builder);
            UPGRADE2_COST_MULTIPLIER_PER_UNIT = defineUpgrade2Multiplier(builder);
            UPGRADE2_COST_OFFSET_PER_UNIT = defineUpgrade2Offset(builder);




            builder.pop();
        }

        private static ForgeConfigSpec.IntValue defineBaseMiningSpeed(ForgeConfigSpec.Builder builder) {
            return builder
                    .comment("""
            Base mining speed, expressed as percentage-of-block-progress per tick.

            For example:
              - 100    = 1 block per second (100% over 20 ticks)
              - 1000   = 10 blocks per second
              - 10000  = 100 blocks per second

            This value determines how quickly a miner makes progress on a single block
            before applying tier, upgrade, or efficiency modifiers.

            === Warning ===
            While technically you may set this to any value up to 100,000,
            we **strongly discourage** values above 10,000 (i.e. 100 blocks per second)
            unless you have very few quarries and very strong hardware.

            Despite all tick suppression and optimization measures,
            block-breaking is not free—it still consumes memory, CPU cache bandwidth,
            and TPS budget per tick. At very high speeds, especially with many
            concurrent quarries, aggregate load may degrade server performance.

            Range: 1–100000
            Default: 10 (i.e. 0.1 blocks per second)
            """)
                    .defineInRange("baseMiningSpeed", 10, 1, 100_000);
        }

        private static ForgeConfigSpec.IntValue defineUpgrade1Speed(ForgeConfigSpec.Builder b) {
            return b
                    .comment("""
            Mining speed increase from each Upgrade 1 unit, in percent-of-block-progress per tick.

            This value is added per unit installed. For example, with 4 upgrades at 35 each:
              4 × 35 = 140 → 1.4 blocks per second increase.

            Range: 1–100,000
            Default: 35
            """)
                    .defineInRange("upgrade1SpeedPerUnit", 35, 1, 100_000);
        }

        private static ForgeConfigSpec.DoubleValue defineUpgrade1Multiplier(ForgeConfigSpec.Builder b) {
            return b
                    .comment("""
            Multiplier applied to each Upgrade 1 unit's contribution to energy-per-tick cost.

            Energy-per-tick = (speed × baseCost) × multiplier + offset, accumulated per unit.

            Default: 1.0 (cost grows linearly with speed)
            """)
                    .defineInRange("upgrade1CostMultiplierPerUnit", 1.0, 0.0, 1000.0);
        }

        private static ForgeConfigSpec.IntValue defineUpgrade1Offset(ForgeConfigSpec.Builder b) {
            return b
                    .comment("""
            Flat FE/t added per Upgrade 1 unit installed.

            This value is added directly to the total energy-per-tick cost per unit.
            It can be negative to discount the upgrade, or positive to penalize it.

            Default: 0
            """)
                    .defineInRange("upgrade1CostOffsetPerUnit", 0, -100_000, 100_000);
        }


        private static ForgeConfigSpec.IntValue defineUpgrade2Speed(ForgeConfigSpec.Builder b) {
            return b
                    .comment("""
            Mining speed increase from each Upgrade 2 unit, in percent-of-block-progress per tick.

            This value is added per unit installed. For example, with 4 upgrades at 35 each:
              4 × 35 = 140 → 1.4 blocks per second increase.

            Range: 1–100,000
            Default: 35
            """)
                    .defineInRange("upgrade1SpeedPerUnit", 35, 1, 100_000);
        }

        private static ForgeConfigSpec.DoubleValue defineUpgrade2Multiplier(ForgeConfigSpec.Builder b) {
            return b
                    .comment("""
            Multiplier applied to each Upgrade 2 unit's contribution to energy-per-tick cost.

            Energy-per-tick = (speed × baseCost) × multiplier + offset, accumulated per unit.

            Default: 1.0 (cost grows linearly with speed)
            """)
                    .defineInRange("upgrade1CostMultiplierPerUnit", 1.0, 0.0, 1000.0);
        }

        private static ForgeConfigSpec.IntValue defineUpgrade2Offset(ForgeConfigSpec.Builder b) {
            return b
                    .comment("""
            Flat FE/t added per Upgrade 2 unit installed.

            This value is added directly to the total energy-per-tick cost per unit.
            It can be negative to discount the upgrade, or positive to penalize it.

            Default: 0
            """)
                    .defineInRange("upgrade1CostOffsetPerUnit", 0, -100_000, 100_000);
        }





        private static ForgeConfigSpec.IntValue defineDifficulty1(ForgeConfigSpec.Builder b) {
            b.push("difficulty1");
            ForgeConfigSpec.IntValue v = b
                    .comment("Maximum laser length for difficulty 1 (easy mode)")
                    .defineInRange("totalLaserLength", Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
            b.pop();
            return v;
        }

        private static ForgeConfigSpec.IntValue defineDifficulty2Basic(ForgeConfigSpec.Builder b) {
            b.push("difficulty2");
            ForgeConfigSpec.IntValue v = b
                    .comment("Maximum laser length for basic tier in difficulty 2")
                    .defineInRange("basicLaserLength", 50, 0, Integer.MAX_VALUE);
            b.pop();
            return v;
        }

        private static ForgeConfigSpec.IntValue defineDifficulty2Intermediate(ForgeConfigSpec.Builder b) {
            b.push("difficulty2");
            ForgeConfigSpec.IntValue v = b
                    .comment("Maximum laser length for intermediate tier in difficulty 2")
                    .defineInRange("intermediateLaserLength", 250, 0, Integer.MAX_VALUE);
            b.pop();
            return v;
        }

        private static ForgeConfigSpec.IntValue defineDifficulty2Advanced(ForgeConfigSpec.Builder b) {
            b.push("difficulty2");
            ForgeConfigSpec.IntValue v = b
                    .comment("Maximum laser length for advanced tier in difficulty 2")
                    .defineInRange("advancedLaserLength", Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
            b.pop();
            return v;
        }

        private static ForgeConfigSpec.IntValue defineDifficulty3Basic(ForgeConfigSpec.Builder b) {
            b.push("difficulty3");
            ForgeConfigSpec.IntValue v = b
                    .comment("Blocks pushed forward by a basic tier repeater")
                    .defineInRange("basicRepeaterLength", 10, 0, Integer.MAX_VALUE);
            b.pop();
            return v;
        }

        private static ForgeConfigSpec.IntValue defineDifficulty3Intermediate(ForgeConfigSpec.Builder b) {
            b.push("difficulty3");
            ForgeConfigSpec.IntValue v = b
                    .comment("Blocks pushed forward by an intermediate tier repeater")
                    .defineInRange("intermediateRepeaterLength", 25, 0, Integer.MAX_VALUE);
            b.pop();
            return v;
        }

        private static ForgeConfigSpec.IntValue defineDifficulty3Advanced(ForgeConfigSpec.Builder b) {
            b.push("difficulty3");
            ForgeConfigSpec.IntValue v = b
                    .comment("Blocks pushed forward by an advanced tier repeater")
                    .defineInRange("advancedRepeaterLength", 100, 0, Integer.MAX_VALUE);
            b.pop();
            return v;
        }

    }
    public enum CompressionTrustPolicy {
        NO_TRUST,               // Disable all compression — assume all mods are unreliable
        TRUST_VANILLA_ONLY,     // Only Mojang-defined items are eligible
        TRUST_WHITELISTED,      // Trust selected mods (via whitelist)
        TRUST_ALL_EXCEPT,       // Trust everything except explicitly blacklisted items
        FULL_TRUST              // Trust all mods to report correctly
    }


    /** Encapsulates all “item handling” settings—e.g. dupe‐protection, rate‐limits, etc. */
    public static class ItemHandlingConfig {
        public final ForgeConfigSpec.BooleanValue DUPLICATE_PROTECTION;

        public final ForgeConfigSpec.ConfigValue<String> COMPRESSION_TRUST_POLICY_RAW;
        public final ForgeConfigSpec.ConfigValue<String> WHITELIST_FILE_PATH;
        public final ForgeConfigSpec.ConfigValue<String> BLACKLIST_FILE_PATH;
        public final ForgeConfigSpec.IntValue DUPE_CHECKING_GRANULARITY;
        public final ForgeConfigSpec.BooleanValue OVERRIDE_SAVE_INTERVAL;
        public final ForgeConfigSpec.IntValue CUSTOM_SAVE_INTERVAL_SECONDS;
        public final ForgeConfigSpec.BooleanValue HARSH_DUPE_PROTECTION;
        public final ForgeConfigSpec.ConfigValue<String> DUPE_EXEMPT_WHITELIST_PATH;





        public ItemHandlingConfig(ForgeConfigSpec.Builder b) {
            b.push("operator");

            DUPLICATE_PROTECTION = b
                    .comment("Prevent the player from using crash based dupe exploits.")
                    .define("dupeProtectionEnabled", false);

            HARSH_DUPE_PROTECTION = b
                    .comment("""
        Enables *harsh* dupe protection mode — enforces per-item isolation and stricter payout timing.

        === Background ===
        The quarry uses a custom internal buffer data type to safely stage mined items before exporting.
        Each buffer entry consists of:
            - a short-form item ID (compressed)
            - a byte item count (0–255)
            - a timestamp or tick counter (byte-sized)

        In standard dupe protection mode, once an item type is inserted, it begins aging immediately.
        After the delay period (30s + 1s buffer + granularity), items are gradually emitted from that bucket,
        regardless of how many more items are added later.

        === Harsh Mode ===
        In harsh mode, the aging timer **resets every time** more of that item type is inserted into the buffer.
        The buffer will not begin payout until:
            1. The item count reaches the full byte capacity (255), or
            2. No new items are added during the entire dupe protection delay window.

        This prevents timing-based exploits where a player inserts one item to start the timer,
        then injects a burst of items immediately before payout begins.

        === Latency Implications ===
        This mode trades increased latency for stronger consistency guarantees.
        Output delay is now determined by either:
            - How fast a quarry fills a full buffer (255 items), OR
            - How long the buffer goes untouched (i.e., no inserts for 30 + 1 + granularity seconds)

        Worst-case latency scenarios:
            - On a low-tier quarry (1 block every 5 seconds), mining one non-exempt resource every ((30 + 1 + granularity) -1 seconds):
              Payout may be delayed up to 5s * 256 = ~21 minutes before any output is seen.
            - On rare items (e.g. uranium), if fewer than 255 are mined, Items will still be exported once the buffer has been idle for a full protection window.

        === Timing Source ===
        The protection window defaults to:
            - 30s (vanilla save interval) + 1s (buffer) + granularity
        If 'overrideBaseSaveInterval' is enabled, this instead uses:
            - custom_save_interval + 1s + granularity

        === Recommendations ===
        - Enable this on public or PvP servers to guarantee temporal isolation per item type.
        - Disable this if quarry throughput is very low and you need predictable latency.
        - This setting does not affect dupe protection correctness — only output latency.
        """)
                    .define("harshDupeProtection", false);

            DUPE_EXEMPT_WHITELIST_PATH = b
                    .comment("""
        Path to file listing low-value items exempt from harsh or soft dupe protection.

        Items in this list will bypass most or all of the buffer timing system. They are exported
        with minimal delay, even if 'harshDupeProtection' is enabled.

        This allows high-throughput bulk materials (like stone or dirt) to flow without friction,
        while still protecting valuable or dangerous items (e.g. modded ores, redstone, diamonds).

        Format: newline-separated list of item registry names (e.g. minecraft:stone)

        Default: quarrymod/dupe_whitelist.txt
        """)
                    .define("dupeExemptItemWhitelistFile", "quarrymod/dupe_whitelist.txt");




            DUPE_CHECKING_GRANULARITY = b
                    .comment("""
        Controls how often the dupe-protection buffer is scanned (in seconds).
        
        Minecraft autosaves every 30 seconds by default. Between saves, world state and item state can diverge.
        To prevent duplication exploits (e.g., mining items into an unsaved inventory like ender chests),
        the quarry holds mined items in a staging buffer for:
        
            30s (save interval) + 1s (safety buffer) + granularity (this value)
        
        The buffer is scanned every 'granularity' seconds. Lower values reduce delay jitter,
        higher values reduce CPU usage (by doing fewer scans), but use more memory.
        
        All values are safe — dupe protection is guaranteed regardless of granularity —
        **as long as the default 30s autosave interval is not changed** by the server or a mod.
        
        Recommended values:
          - 1: Minimal delay jitter, high CPU cost
          - 5–10: Balanced for typical servers
          - 30–60: Low CPU usage, higher memory retention
          
        Range: 1–120 seconds
        Default: 10
        """)
                    .defineInRange("dupeCheckingGranularity", 10, 1, 120);



            OVERRIDE_SAVE_INTERVAL = b
                    .comment("""
        WARNING: DO NOT ENABLE THIS UNLESS YOUR SERVER HAS A CUSTOM WORLD SAVE INTERVAL.

        Minecraft saves the world every 30 seconds by default. Dupe protection depends on this timing.
        If your server has *intentionally* altered this save interval (e.g. via server.properties or a mod),
        and the interval is LOWER than 30 seconds, you must enable this and specify the correct value below.

        Setting this flag incorrectly will BREAK dupe protection and may enable item duplication exploits.

        Default: false
        """)
                    .define("overrideBaseSaveInterval", false);

            CUSTOM_SAVE_INTERVAL_SECONDS = b
                    .comment("""
        Leave this field unchanged unless overrideBaseSaveInterval is true.

        This value is ignored unless overrideBaseSaveInterval = true.
        It defines the number of seconds between world saves on your server,
        but should ONLY be used if you have explicitly reduced or increased the save interval.

        This is NOT a default, NOT used by vanilla, and NOT required in most cases.

        Set to your real save interval only if you're overriding the 30s vanilla default.

        Range: 1–900
        Default: (unused unless override is true)
        """)
                    .defineInRange("customBaseSaveIntervalSeconds", 30, 1, 900);



            COMPRESSION_TRUST_POLICY_RAW = b
                    .comment("""
                Compression trust policy — determines how much you trust mods to self-report NBT or capabilities.

                Valid values:
                  - NO_TRUST             = Never compress anything. Disables all structural sharing.
                  - TRUST_VANILLA_ONLY   = Only Mojang (vanilla) items are trusted for compression.
                  - TRUST_WHITELISTED    = Trust vanilla + items listed in the whitelist file.
                  - TRUST_ALL_EXCEPT     = Trust everything except items in the blacklist file.
                  - FULL_TRUST           = Trust all mods unless the item claims NBT/capability data.

                NOTE: Compression is always lossless for items that declare NBT or capabilities.
                      These modes control how much you trust mod authors to report item complexity correctly.
                """)
                    .define("compressionTrustPolicy", "NO_TRUST");

            WHITELIST_FILE_PATH = b
                    .comment("Path (relative to config folder) for item whitelist used in TRUST_WHITELISTED mode.")
                    .define("compressionWhitelistFile", "quarrymod/item_whitelist.txt");

            BLACKLIST_FILE_PATH = b
                    .comment("Path (relative to config folder) for item blacklist used in TRUST_ALL_EXCEPT mode.")
                    .define("compressionBlacklistFile", "quarrymod/item_blacklist.txt");

            b.pop();
        }

        public CompressionTrustPolicy getCompressionTrustPolicy() {
            try {
                return CompressionTrustPolicy.valueOf(COMPRESSION_TRUST_POLICY_RAW.get().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return CompressionTrustPolicy.NO_TRUST;
            }
        }

        public Path getWhitelistFilePath(Path configDir) {
            return configDir.resolve(WHITELIST_FILE_PATH.get());
        }

        public Path getBlacklistFilePath(Path configDir) {
            return configDir.resolve(BLACKLIST_FILE_PATH.get());
        }
    }

    public class AdminRuntimeConfig {
        public final ForgeConfigSpec.BooleanValue ENABLE_QUARRY_TICK_SUPPRESSION;


        public AdminRuntimeConfig(ForgeConfigSpec.Builder b) {
            b.push("runtime");

            ENABLE_QUARRY_TICK_SUPPRESSION = b
                    .comment("""
        [ADVANCED] Enables tick suppression inside quarry-managed regions. DO NOT DISABLE unless you fully understand the consequences. (default: true)

        === What This Does ===
        This setting is the foundation of this mod's performance model.

        Minecraft's block-breaking system is highly recursive: breaking a single block can trigger light updates,
        neighbor block updates, redstone propagation, water flow, block drops, random ticks, and even entity AI reevaluation.
        In aggregate, a single block break can consume **thousands of CPU cycles**, even though the actual break operation
        takes only ~5–25 cycles.

        Tick suppression prevents these cascading update storms—one of the primary causes of catastrophic lag
        in older systems like BuildCraft. Vanilla’s update propagation is recursive and often unbounded:
        breaking one block may result in tens of thousands of secondary updates (especially with fluids, redstone, or mobs nearby).

        With suppression enabled, the quarry engine aggressively isolates its logic from vanilla's global tick system.
        It breaks blocks **without triggering surrounding light, redstone, AI, or water updates**. All suppressed updates
        are captured and **replayed cleanly in a non-recursive batch** after the quarry finishes its tick.
        This containment ensures that the CPU cost of block breaking remains stable and bounded.

        === Effects ===
        - Quarry operations remain in the ideal 5–25 cycle range per block.
        - Visual glitches (lightmaps, fluid levels) may appear briefly inside the quarry region.
        - All suppressed updates are reintroduced safely after operation concludes.
        - Suppression is spatially confined: only affects the quarry region.
        - Entities, redstone, water, and random ticks behave normally two blocks below the lowest quarry Y-level.

        === What This Affects ===
        - Only blocks *within the quarry boundary* are suppressed.
        - Blocks *outside the quarry*, even in the same chunk, are unaffected.
        - Tick suppression only applies *above* the current digging plane.
          E.g. if the quarry is at Y=33, then Y=31 and below are fully vanilla.

        === Disabling This ===
        You may disable this if:
        - You are running a **singleplayer world** on fast hardware,
        - You have **1–2 quarries** and minimal mod interaction,
        - You explicitly want full vanilla tick behavior and accept performance loss.

        **WE STRONGLY DISCOURAGE DISABLING THIS ON SERVERS.**
        - 5+ quarries may begin to degrade performance even on moderate hardware.
        - 10+ will cause severe lag or TPS loss on most systems.
        - 15+ will lead to persistent server degradation due to vanilla's recursive update behavior.

        This is not a bug in our code. This is vanilla being inherently unbounded and unoptimized.
        
        === Mini Guide: When Might It Be Safe to Disable This? ===

        While we strongly recommend keeping tick suppression enabled, it may be technically safe to disable in
        **singleplayer worlds** or **small server setups**, depending on your **mod load and tick budget**.

        The critical question is not "how many mods you have," but **how much tick time is consumed by other mods**.
        Some mods actually improve performance (e.g. Sodium, FerriteCore, Entity Culling, Faster Random),
        freeing up CPU time that could absorb vanilla-style update propagation.

        === Common Performance Pitfalls and Historical Examples ===

        Instead of focusing on specific mods, it's more productive to understand **which patterns** tend to harm performance,
        and then recognize which mods (historically or architecturally) have exhibited those behaviors.

          - **Tick Amplification via Time Acceleration**: Systems that accelerate time (e.g., tick multipliers) can cause exponential
            growth in tick work. *Example: ProjectE’s Watch of Flowing Time.*

          - **Tile Entity Proliferation**: Excessive or densely packed tile entities strain the server’s tick scheduler,
            even when idle. *Examples: Older versions of Chisel & Bits, Malisis Doors, Decocraft.*

          - **Overuse of Animated or Shader-bound Textures**: Animated block or entity textures can tank framerate and,
            depending on implementation, leak into server TPS via rendering callbacks or blockstate invalidation.
            *Examples: Many aesthetic or particle-heavy mods.*

          - **Entity AI Complexity**: Mobs with custom pathing, sensing, or multi-stage behavior trees often induce
            unpredictable tick cost spikes, especially when swarming or scheduled concurrently.
            *Example: Alex’s Mobs (well-made, but structurally heavy).*

          - **Recursive or Deferred Simulation Systems**: Mods that simulate mechanical or kinetic systems
            at high fidelity (e.g., via contraptions, motion graphs, or tick queues) impose real CPU overhead
            even when optimized. *Example: Create — technically brilliant, but necessarily expensive.*

        Again, these examples are not criticisms—they’re illustrations of how **even well-engineered systems**
        can saturate the tick budget when scaled. Understanding the performance signature of these patterns
        helps you make informed decisions about disabling tick suppression.

        === Making an Educated Guess ===

        You may consider disabling tick suppression **if all of the following apply**:
          - You are in singleplayer, or on a small, low-concurrency server.
          - Your total mod load includes **few or no large-scale simulation systems**.
          - You use performance-enhancing mods (e.g., Sodium, Lithium, FerriteCore).
          - Your world has ≤ 1–2 quarries, and no ticking tile entity clusters nearby.

        If you are still unsure, the simplest way to make sure you can disable it is if:
          - You have profiled your TPS and observed that you are consistently below 40% tick time usage.

        In all other cases, suppression should remain **enabled** to ensure stable quarry operation
        and to prevent cascading block update storms.

        This guide is not exhaustive—but it should help you reason about **why** the default is what it is,
        and **when** it's safe to override it.

        Default: true
        """)
                    .define("enableQuarryTickSuppression", true);

            b.pop();
        }
    }




    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        // React to config load/reload if needed
    }
}
