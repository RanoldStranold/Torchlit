package net.ranold.client;

import java.util.List;

import net.minecraft.util.Mth;

// interface darkness at a screen point: ambient light, item lights, eye adaptation
public final class DarknessField {

    private DarknessField() {
    }

    public record PointLight(float x, float y, float luminance, float radius) {
    }

    // brighter of the ambient light and the item lights at a screen point.
    // item lights blend like a screen layer: taking only the brightest made a dark crease where two glows met
    public static float lightAt(float x, float y, float ambient, List<PointLight> lights) {
        float unlit = 1.0f;

        for (PointLight source : lights) {
            if (source.radius() <= 0.0f) {
                continue;
            }

            float dx = x - source.x();
            float dy = y - source.y();
            float distanceSquared = dx * dx + dy * dy;
            if (distanceSquared >= source.radius() * source.radius()) {
                continue;
            }

            float t = 1.0f - (float) Math.sqrt(distanceSquared) / source.radius();
            float contribution = source.luminance() * t * t * (3.0f - 2.0f * t);
            unlit *= 1.0f - Mth.clamp(contribution / InventoryDarkness.FULL_BRIGHT_LEVEL, 0.0f, 1.0f);
        }

        return Math.max(ambient, (1.0f - unlit) * InventoryDarkness.FULL_BRIGHT_LEVEL);
    }

    // true when some light reaches into the rectangle; otherwise darkness there is the ambient value everywhere
    public static boolean lightsReach(float minX, float minY, float maxX, float maxY, List<PointLight> lights) {
        for (PointLight source : lights) {
            if (source.radius() <= 0.0f) {
                continue;
            }
            float dx = source.x() - Mth.clamp(source.x(), minX, maxX);
            float dy = source.y() - Mth.clamp(source.y(), minY, maxY);
            if (dx * dx + dy * dy < source.radius() * source.radius()) {
                return true;
            }
        }
        return false;
    }

    // 0 = untouched, maxDarkness = fully dark
    public static float darknessForLight(float light, float maxDarkness) {
        light = EyeAdaptation.perceived(light);
        int fullBright = Math.max(1, InventoryDarkness.FULL_BRIGHT_LEVEL);
        float t = Mth.clamp(light / fullBright, 0.0f, 1.0f);
        return (float) Math.pow(1.0f - t, InventoryDarkness.CURVE) * maxDarkness;
    }
}
