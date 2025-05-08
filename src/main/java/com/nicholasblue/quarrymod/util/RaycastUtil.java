package com.nicholasblue.quarrymod.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

/**
 * Static helper for server-side ray tracing from a player’s POV.
 *
 * All methods are side-effect-free and null-safe; they never load chunks.
 */
public final class RaycastUtil {

    /**
     * Performs a ray trace from the given player’s eye position out to {@code maxDistance}
     * meters in the direction the player is currently looking.
     *
     * @param player      Server-side player (must be non-null)
     * @param maxDistance Maximum distance to trace (vanilla reach ≈ 5–6)
     * @return BlockHitResult with type {@link HitResult.Type#BLOCK} if a block
     *         was hit, or {@link HitResult.Type#MISS} otherwise.  Never null.
     */
    public static BlockHitResult traceBlock(ServerPlayer player, double maxDistance) {
        Vec3 eyePos   = player.getEyePosition(1.0F);
        Vec3 lookVec  = player.getLookAngle();
        Vec3 reachVec = eyePos.add(lookVec.x * maxDistance,
                lookVec.y * maxDistance,
                lookVec.z * maxDistance);

        return player.level().clip(new ClipContext(
                eyePos,
                reachVec,
                ClipContext.Block.OUTLINE,      // collide with block outlines
                ClipContext.Fluid.NONE,         // ignore fluids; change if needed
                player));
    }

    /** hard‐coded vanilla reach convenience (5.0D) */
    public static BlockHitResult traceBlock(ServerPlayer player) {
        return traceBlock(player, 5.0D);
    }

    /* ── utility class: no instances ── */
    private RaycastUtil() {}
}
