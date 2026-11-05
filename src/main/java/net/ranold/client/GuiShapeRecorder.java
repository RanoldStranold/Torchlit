package net.ranold.client;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;

public final class GuiShapeRecorder {

    public record Blit(GpuTextureView texture, GpuSampler sampler, Matrix3x2f pose,
                       int x0, int y0, int x1, int y1,
                       float minU, float maxU, float minV, float maxV) {
        public int width() {
            return this.x1 - this.x0;
        }

        public int height() {
            return this.y1 - this.y0;
        }

        public boolean intersects(int left, int top, int right, int bottom) {
            return this.x0 < right && this.x1 > left && this.y0 < bottom && this.y1 > top;
        }
    }

    private static final List<Blit> RECORDED = new ArrayList<>();
    private static boolean recording;
    private static int paused;

    private GuiShapeRecorder() {
    }

    public static void begin() {
        RECORDED.clear();
        recording = true;
        paused = 0;
    }

    public static void pause() {
        paused++;
    }

    public static void resume() {
        if (paused > 0) {
            paused--;
        }
    }

    public static List<Blit> end() {
        recording = false;
        return RECORDED;
    }

    public static void record(GpuTextureView texture, GpuSampler sampler, Matrix3x2fc pose,
                              int x0, int y0, int x1, int y1,
                              float minU, float maxU, float minV, float maxV) {
        if (!recording || paused > 0 || texture == null || x1 <= x0 || y1 <= y0) {
            return;
        }
        // copy the pose per sprite: a later pose puts slot contents near the screen corner
        RECORDED.add(new Blit(texture, sampler, new Matrix3x2f(pose),
                x0, y0, x1, y1, minU, maxU, minV, maxV));
    }
}
