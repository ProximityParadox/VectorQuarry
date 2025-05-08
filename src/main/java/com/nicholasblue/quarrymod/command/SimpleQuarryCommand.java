package com.nicholasblue.quarrymod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.nicholasblue.quarrymod.QuarryMod;
import com.nicholasblue.quarrymod.network.SuppressionNetwork;
import com.nicholasblue.quarrymod.util.QuarryPlacementScheduler;
import com.nicholasblue.quarrymod.util.RaycastUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import com.nicholasblue.quarrymod.data.BlockIndexer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import com.nicholasblue.quarrymod.client.SuppressionDebugRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * /simplequarry command root
 *
 * Hierarchy implemented so far:
 *   /simplequarry version
 *   /simplequarry debug blockindexer
 *
 * Add additional sub-branches by writing more private builder methods and
 * wiring them into {@link #root()}.
 */
public final class SimpleQuarryCommand {
    public static final SuppressionDebugRenderer INSTANCE = new SuppressionDebugRenderer();


    /* ─────────────────────────────────────────── public entry point ────────────────────────────────────────── */

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(root());
    }

    /* ──────────────────────────────────────────── root and branches ───────────────────────────────────────── */

    /** literal("simplequarry") with all first-level children attached */
    private static LiteralArgumentBuilder<CommandSourceStack> root() {
        return Commands.literal("simplequarry")
                .then(version())
                .then(debugBranch());
    }

    /** /simplequarry version */
    private static LiteralArgumentBuilder<CommandSourceStack> version() {
        return Commands.literal("version")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(
                            () -> Component.literal("Simple Quarry v1.0.0"),
                            false
                    );
                    return 1;
                });
    }

    /** /simplequarry debug …  (container for future debug subcommands) */
    private static LiteralArgumentBuilder<CommandSourceStack> debugBranch() {
        return Commands.literal("debug")
                .then(debugBlockIndexer())   // additional debug subcommands go here
                .then(debugIdOfBlock())
                .then(debugBlockOfId())
                .then(debugIdAtCursor())
                .then(debugToggleSuppressionVisualizer())
                .then(debugsimplePlaceQuarries())
                .then(debugDelayedPlaceQuarries());

    }

    /** /simplequarry debug blockindexer */
    private static LiteralArgumentBuilder<CommandSourceStack> debugBlockIndexer() {
        return Commands.literal("blockindexer")
                .executes(ctx -> {
                    int count = BlockIndexer.size();
                    ctx.getSource().sendSuccess(
                            () -> Component.literal(
                                    "[SimpleQuarry] Indexed " + count + " placeable blocks."),
                            false
                    );
                    return 1;
                });
    }
    /** /simplequarry debug id_of_block <namespace:id> */
    private static LiteralArgumentBuilder<CommandSourceStack> debugIdOfBlock() {
        return Commands.literal("id_of_block")
                .then(Commands.argument("block", ResourceLocationArgument.id())
                        .suggests(PLACEABLE_BLOCKS)
                        .executes(ctx -> {
                            ResourceLocation rl =
                                    ResourceLocationArgument.getId(ctx, "block");
                            Block b = BuiltInRegistries.BLOCK
                                    .getOptional(rl)
                                    .orElse(null);

                            if (b == null) {
                                ctx.getSource().sendFailure(
                                        Component.literal("[SimpleQuarry] Unknown block " + rl));
                                return 0;
                            }

                            int id = BlockIndexer.id(b);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal(
                                            rl + " → index " + id),
                                    false);
                            return 1;
                        }));
    }

    /** /simplequarry debug block_of_id <int> */
    private static LiteralArgumentBuilder<CommandSourceStack> debugBlockOfId() {
        return Commands.literal("block_of_id")
                .then(Commands.argument("id", IntegerArgumentType.integer(0))
                        .executes(ctx -> {
                            int id = IntegerArgumentType.getInteger(ctx, "id");
                            Block b = BlockIndexer.block(id);

                            if (b == null) {
                                ctx.getSource().sendFailure(
                                        Component.literal("[SimpleQuarry] No block mapped to index " + id));
                                return 0;
                            }

                            ResourceLocation rl =
                                    BuiltInRegistries.BLOCK.getKey(b);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal(
                                            "index " + id + " → " + rl),
                                    false);
                            return 1;
                        }));
    }
    /** /simplequarry debug id_at_cursor */
    private static LiteralArgumentBuilder<CommandSourceStack> debugIdAtCursor() {
        return Commands.literal("id_at_cursor")
                .executes(ctx -> {

                    /* ensure sender is a server-side player */
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        ctx.getSource().sendFailure(
                                Component.literal("[SimpleQuarry] Command must be run by a player."));
                        return 0;
                    }

                    BlockHitResult hit = RaycastUtil.traceBlock(player); // 5-block reach
                    if (hit.getType() != HitResult.Type.BLOCK) {
                        ctx.getSource().sendFailure(
                                Component.literal("[SimpleQuarry] No block in line of sight."));
                        return 0;
                    }

                    Block block = player.level().getBlockState(hit.getBlockPos()).getBlock();
                    Integer id  = BlockIndexer.id(block);

                    if (id == 0 && block == Blocks.AIR) {          // example edge-case guard
                        ctx.getSource().sendFailure(
                                Component.literal("[SimpleQuarry] Air or unindexed block."));
                        return 0;
                    }

                    ctx.getSource().sendSuccess(
                            () -> Component.literal(
                                    BuiltInRegistries.BLOCK.getKey(block) + " → index " + id),
                            false
                    );
                    return 1;
                });
    }
    private static LiteralArgumentBuilder<CommandSourceStack> debugToggleSuppressionVisualizer() {
        return Commands.literal("VisualizeSuppression")
                .executes(ctx -> {

                    QuarryMod.getDebugRenderer().flipDebugRender();

                    if (QuarryMod.getDebugRenderer().isDebugEnabled()) {
                        // Request full sync
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        SuppressionNetwork.sendSnapshotToClient(player);
                    }


                    ctx.getSource().sendSuccess(
                            () -> Component.literal("[SimpleQuarry] Toggled suppression visualization."),
                            false
                    );
                    return 1;
                });
    }


    private static LiteralArgumentBuilder<CommandSourceStack> debugsimplePlaceQuarries() {
        return Commands.literal("place_simple_quarries")
                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                ctx.getSource().sendFailure(Component.literal("[SimpleQuarry] Command must be run by a player."));
                                return 0;
                            }

                            var level = player.level();
                            var block = BuiltInRegistries.BLOCK.get(new ResourceLocation("quarrymod:quarry_block"));

                            if (block == null) {
                                ctx.getSource().sendFailure(Component.literal("[SimpleQuarry] Could not find quarrymod:quarry_block."));
                                return 0;
                            }

                            BlockPos origin = player.blockPosition();

                            int spacing = 9;  // Optimal spacing (rigorously derived above)
                            int count = IntegerArgumentType.getInteger(ctx, "count");
                            int side = (int) Math.ceil(Math.sqrt(count)); // Calculate grid side length

                            // Centering the grid on the player
                            int offset = (side - 1) * spacing / 2;

                            int placed = 0;

                            for (int ix = 0; ix < side && placed < count; ix++) {
                                for (int iz = 0; iz < side && placed < count; iz++) {
                                    int x = origin.getX() + (ix * spacing) - offset;
                                    int y = origin.getY();
                                    int z = origin.getZ() + (iz * spacing) - offset;

                                    BlockPos pos = new BlockPos(x, y, z);
                                    BlockState state = block.defaultBlockState();

                                    level.setBlock(pos, state, Block.UPDATE_ALL_IMMEDIATE);
                                    placed++;
                                }
                            }

                            final int totalPlaced = placed;
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("[SimpleQuarry] Placed " + totalPlaced + " quarry blocks in optimal grid around player."), false);

                            return 1;
                        }));
    }





    private static LiteralArgumentBuilder<CommandSourceStack> debugDelayedPlaceQuarries() {
        return Commands.literal("place_delayed_quarries")
                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                ctx.getSource().sendFailure(Component.literal("[SimpleQuarry] Command must be run by a player."));
                                return 0;
                            }

                            ServerLevel level = player.serverLevel();
                            var block = BuiltInRegistries.BLOCK.get(new ResourceLocation("quarrymod:quarry_block"));

                            if (block == null) {
                                ctx.getSource().sendFailure(Component.literal("[SimpleQuarry] Could not find quarrymod:quarry_block."));
                                return 0;
                            }

                            BlockPos origin = player.blockPosition();
                            int spacing = 9;
                            int count = IntegerArgumentType.getInteger(ctx, "count");
                            int side = (int) Math.ceil(Math.sqrt(count));
                            int offset = (side - 1) * spacing / 2;

                            List<BlockPos> positions = new ArrayList<>();
                            for (int ix = 0; ix < side && positions.size() < count; ix++) {
                                for (int iz = 0; iz < side && positions.size() < count; iz++) {
                                    int x = origin.getX() + (ix * spacing) - offset;
                                    int y = origin.getY();
                                    int z = origin.getZ() + (iz * spacing) - offset;
                                    positions.add(new BlockPos(x, y, z));
                                }
                            }

                            QuarryPlacementScheduler.schedule(level, positions, block);
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("[SimpleQuarry] Scheduled " + positions.size() + " quarries with one-per-tick delay."), false);
                            return 1;
                        }));
    }












    /* ─────────────────────────────────────────────── utilities ────────────────────────────────────────────── */

    private SimpleQuarryCommand() {}  // static-only class
    private static final SuggestionProvider<CommandSourceStack> PLACEABLE_BLOCKS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    BlockIndexer.keyStream().map(ResourceLocation::toString),
                    builder);


}