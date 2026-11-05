package net.ranold.client;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class ContainerLocation {

    private static final long CLAIM_WINDOW_MS = 1000L;

    private static final double MAX_REACH = 8.0;

    @Nullable
    private static BlockPos lastUsedPos;
    @Nullable
    private static Direction lastUsedFace;
    private static long lastUsedAt;

    @Nullable
    private static BlockPos openPos;
    @Nullable
    private static Direction openFace;

    private ContainerLocation() {
    }

    public static void recordUse(BlockPos pos, Direction face) {
        lastUsedPos = pos.immutable();
        lastUsedFace = face;
        lastUsedAt = System.currentTimeMillis();
    }

    public static void onScreenOpened(boolean fromBlock) {
        if (fromBlock
                && lastUsedPos != null
                && System.currentTimeMillis() - lastUsedAt <= CLAIM_WINDOW_MS) {
            openPos = lastUsedPos;
            openFace = lastUsedFace;
        } else {
            openPos = null;
            openFace = null;
        }
    }

    public static float lightLevel(Level level, BlockPos playerPos) {
        BlockPos pos = openPos;
        if (pos == null || !pos.closerThan(playerPos, MAX_REACH)) {
            return -1.0f;
        }

        float light = AmbientLight.at(level, pos);
        if (openFace != null) {
            // solid blocks read 0 inside, so use the clicked face too
            light = Math.max(light, AmbientLight.at(level, pos.relative(openFace)));
        }
        return light;
    }
}
