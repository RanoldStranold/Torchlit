package net.ranold.client;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;

import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.Mth;
import net.ranold.client.DarknessField.PointLight;
import net.ranold.config.TorchlitConfig;

// interface darkness by tinting: every element a screen or the hotbar adds is multiplied by the darkness
// where it is drawn. one multiply per element, so overlaps never darken twice (gui blending is linear)
public final class GuiTint {

    // one per pass (screen, hotbar), reused every frame
    public static final class Context {
        private int left = Integer.MIN_VALUE;
        private int top = Integer.MIN_VALUE;
        private int right = Integer.MAX_VALUE;
        private int bottom = Integer.MAX_VALUE;

        private boolean active;
        private float ambient;
        private float ambientDarkness;
        private List<PointLight> lights = List.of();
        private float maxDarkness;

        // darkness sampled once per frame on a shared grid, like the old per-sprite mask grid
        private float[] lattice = new float[0];
        private int[] stamp = new int[0];
        private int generation;
        private int latticeCols;
        private int latticeRows;
        private int cell = 8;

        // only elements overlapping this rectangle are tinted
        public void reset(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.active = false;
        }

        public void resetUnbounded() {
            this.reset(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        public void set(float ambient, List<PointLight> lights, float maxDarkness) {
            this.ambient = ambient;
            this.lights = lights;
            this.maxDarkness = maxDarkness;
            this.ambientDarkness = DarknessField.darknessForLight(ambient, maxDarkness);
            this.active = maxDarkness > 0.0f;

            // fixed for the frame: the grid and the subdivision must agree
            this.cell = Math.max(1, TorchlitConfig.get().lightDetail);
            Window window = Minecraft.getInstance().getWindow();
            this.latticeCols = window.getGuiScaledWidth() / this.cell + 2;
            this.latticeRows = window.getGuiScaledHeight() / this.cell + 2;
            int size = this.latticeCols * this.latticeRows;
            if (this.lattice.length < size) {
                this.lattice = new float[size];
                this.stamp = new int[size];
            }
            this.generation++;
        }

        float ambientDarkness() {
            return this.ambientDarkness;
        }

        boolean lightsReach(float minX, float minY, float maxX, float maxY) {
            return DarknessField.lightsReach(minX, minY, maxX, maxY, this.lights);
        }

        boolean accepts(@Nullable ScreenRectangle bounds) {
            if (this.left == Integer.MIN_VALUE) {
                return true;
            }
            return bounds != null && bounds.left() < this.right && bounds.right() > this.left
                    && bounds.top() < this.bottom && bounds.bottom() > this.top;
        }

        float darkness(float x, float y) {
            if (this.lights.isEmpty()) {
                return this.ambientDarkness;
            }
            int cell = this.cell;
            float gx = x / cell;
            float gy = y / cell;
            int i = Mth.floor(gx);
            int j = Mth.floor(gy);
            if (i < 0 || j < 0 || i + 1 >= this.latticeCols || j + 1 >= this.latticeRows) {
                return this.exact(x, y);
            }
            float fx = gx - i;
            float fy = gy - j;
            float top = Mth.lerp(fx, this.point(i, j), this.point(i + 1, j));
            float bottom = Mth.lerp(fx, this.point(i, j + 1), this.point(i + 1, j + 1));
            return Mth.lerp(fy, top, bottom);
        }

        private float point(int i, int j) {
            int index = j * this.latticeCols + i;
            if (this.stamp[index] != this.generation) {
                this.lattice[index] = this.exact(i * this.cell, j * this.cell);
                this.stamp[index] = this.generation;
            }
            return this.lattice[index];
        }

        private float exact(float x, float y) {
            float light = DarknessField.lightAt(x, y, this.ambient, this.lights);
            return light == this.ambient ? this.ambientDarkness : DarknessField.darknessForLight(light, this.maxDarkness);
        }

        int cell() {
            return this.cell;
        }
    }

    // gui elements are built one at a time on the render thread
    private static final SubdividedTint SUBDIVIDED = new SubdividedTint();
    private static final VertexTint PER_VERTEX = new VertexTint();

    // cleared with the gui render state each frame
    private static final Map<Object, Context> TAGS = new IdentityHashMap<>();

    private static @Nullable Context capturing;
    private static int paused;
    private static @Nullable Context source;

    private GuiTint() {
    }

    public static void begin(Context context) {
        capturing = context;
        paused = 0;
    }

    public static void end() {
        capturing = null;
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

    public static void clear() {
        TAGS.clear();
        capturing = null;
        paused = 0;
        source = null;
    }

    // states added while a screen or the hotbar extracts
    public static void tagExtracted(Object state, @Nullable ScreenRectangle bounds) {
        Context context = capturing;
        if (context != null && paused == 0 && context.accepts(bounds)) {
            TAGS.put(state, context);
        }
    }

    // item blits, glyphs and picture-in-picture blits are made at prepare time from a tagged state
    public static void tagPrepared(Object element) {
        if (source != null) {
            TAGS.put(element, source);
        }
    }

    public static <T> Consumer<T> trackSource(Consumer<T> consumer) {
        return state -> {
            Context previous = source;
            source = TAGS.get(state);
            try {
                consumer.accept(state);
            } finally {
                source = previous;
            }
        };
    }

    // returns the consumer itself when the element is not darkened
    public static VertexConsumer wrap(GuiElementRenderState element, VertexConsumer consumer) {
        Context context = TAGS.get(element);
        if (context == null || !context.active) {
            return consumer;
        }
        return subdivides(element.pipeline())
                ? SUBDIVIDED.use(consumer, context)
                : PER_VERTEX.use(consumer, context);
    }

    public static void finish(VertexConsumer consumer) {
        if (consumer instanceof SubdividedTint tint) {
            tint.finish();
        }
    }

    // these formats hold only position, uv and color, so any other attribute is dropped by the buffer anyway
    private static boolean subdivides(RenderPipeline pipeline) {
        if (pipeline.getPrimitiveTopology() != PrimitiveTopology.QUADS) {
            return false;
        }
        VertexFormat format = pipeline.getVertexFormatBinding(0);
        return format == DefaultVertexFormat.POSITION_COLOR || format == DefaultVertexFormat.POSITION_TEX_COLOR;
    }

    static int tint(int argb, float darkness) {
        if (darkness <= 0.0f) {
            return argb;
        }
        float keep = 1.0f - Math.min(darkness, 1.0f);
        int r = Math.round(((argb >> 16) & 0xFF) * keep);
        int g = Math.round(((argb >> 8) & 0xFF) * keep);
        int b = Math.round((argb & 0xFF) * keep);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
