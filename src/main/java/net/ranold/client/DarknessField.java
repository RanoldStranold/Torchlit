package net.ranold.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.ranold.client.GuiShapeRecorder.Blit;

public final class DarknessField {

    private static final int MIN_DIVISIONS = 1;

    private DarknessField() {
    }

    public record PointLight(float x, float y, float luminance, float radius) {
    }

    public static void submit(GuiGraphicsExtractor graphics, List<Blit> sprites,
                              float ambient, List<PointLight> lights, float maxDarkness) {
        if (sprites.isEmpty()) {
            return;
        }

        int step = Mth.clamp(InventoryDarkness.CELL_SIZE, 1, 32);

        for (Blit sprite : sprites) {
            submitSprite(graphics, sprite, step, ambient, lights, maxDarkness);
        }
    }

    private static void submitSprite(GuiGraphicsExtractor graphics, Blit sprite, int step,
                                     float ambient, List<PointLight> lights, float maxDarkness) {
        int cols = Math.max(MIN_DIVISIONS, Math.ceilDiv(sprite.width(), step));
        int rows = Math.max(MIN_DIVISIONS, Math.ceilDiv(sprite.height(), step));

        int[] vertexAlpha = new int[(cols + 1) * (rows + 1)];
        boolean anyDarkness = false;

        for (int row = 0; row <= rows; row++) {
            float y = sprite.y0() + (row / (float) rows) * sprite.height();

            for (int col = 0; col <= cols; col++) {
                float x = sprite.x0() + (col / (float) cols) * sprite.width();
                int alpha = alphaAt(x, y, ambient, lights, maxDarkness);
                vertexAlpha[row * (cols + 1) + col] = alpha;
                anyDarkness |= alpha > 0;
            }
        }

        if (!anyDarkness) {
            return;
        }

        DarknessMesh mesh = new DarknessMesh(
                sprite.texture(), sprite.sampler(), sprite.pose(),
                sprite.x0(), sprite.y0(), sprite.width(), sprite.height(),
                cols, rows, vertexAlpha,
                sprite.minU(), sprite.minV(), sprite.maxU(), sprite.maxV(),
                null);

        graphics.guiRenderState.addGuiElement(mesh);
    }

    private static int alphaAt(float x, float y, float ambient, List<PointLight> lights, float maxDarkness) {
        float light = ambient;

        for (PointLight source : lights) {
            if (source.radius() <= 0.0f) {
                continue;
            }

            float dx = x - source.x();
            float dy = y - source.y();
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance >= source.radius()) {
                continue;
            }

            float t = 1.0f - distance / source.radius();
            float contribution = source.luminance() * t * t * (3.0f - 2.0f * t);
            if (contribution > light) {
                light = contribution;
            }
        }

        return alphaForLight(light, maxDarkness);
    }

    public static int alphaForLight(float light, float maxDarkness) {

        light = EyeAdaptation.perceived(light);
        int fullBright = Math.max(1, InventoryDarkness.FULL_BRIGHT_LEVEL);
        float t = Mth.clamp(light / fullBright, 0.0f, 1.0f);
        float darkness = (float) Math.pow(1.0f - t, InventoryDarkness.CURVE);
        float maxAlpha = maxDarkness * 255.0f;
        return Mth.clamp(Math.round(darkness * maxAlpha), 0, 255);
    }
}
