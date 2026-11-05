package net.ranold.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public final class AmbientLight {

    private AmbientLight() {
    }

    public static float at(Level level, BlockPos pos) {

        if (hasEnhancedVision()) {
            return InventoryDarkness.FULL_BRIGHT_LEVEL;
        }

        float skyLight = Math.max(0, level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken());
        float blockLight = level.getBrightness(LightLayer.BLOCK, pos);

        float light = Math.max(
                skyLight * WorldDarknessFactor.sky(),
                blockLight * WorldDarknessFactor.block());

        if (InventoryDarkness.RESPECT_DYNAMIC_LIGHTS) {
            light = Math.max(light, DynamicLightCompat.lightAt(pos));
        }

        return light;
    }

    // same effects the world stands down for
    private static boolean hasEnhancedVision() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        return player.hasEffect(MobEffects.NIGHT_VISION)
                || (player.hasEffect(MobEffects.CONDUIT_POWER) && player.getWaterVision() > 0.0f);
    }

    public static float atPlayer(Minecraft client) {
        return at(client.level, BlockPos.containing(client.player.getEyePosition()));
    }

    public static float atOpenContainer(Minecraft client) {
        BlockPos playerPos = BlockPos.containing(client.player.getEyePosition());

        float atContainer = ContainerLocation.lightLevel(client.level, playerPos);
        if (atContainer >= 0.0f) {
            return atContainer;
        }

        return at(client.level, playerPos);
    }
}
