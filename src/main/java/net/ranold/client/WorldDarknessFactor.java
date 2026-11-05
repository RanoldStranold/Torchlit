package net.ranold.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.ranold.world.WorldDarkness;

// how much the world darkening dims sky light, so the interface agrees with the world
public final class WorldDarknessFactor {

    private WorldDarknessFactor() {
    }

    public static float sky() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return 1.0f;
        }

        Camera camera = client.gameRenderer.mainCamera();
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        Float vanilla = camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, partialTick);
        if (vanilla == null) {
            return 1.0f;
        }

        // End: sky column reads 15 but renders at factor 0
        if (vanilla <= 0.0f) {
            return 0.0f;
        }

        return WorldDarkness.skyFactor(vanilla, partialTick) / vanilla;
    }

    public static float block() {
        return 1.0f;
    }
}
