package net.ranold.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.util.Mth;

// splits each quad into a grid before tinting, so darkness that changes across a big sprite (item glow) keeps its shape.
// only for position/uv/color formats; call finish() after the element is built
final class SubdividedTint implements VertexConsumer {

    private static final int MAX_DIVISIONS = 256;

    private VertexConsumer delegate;
    private GuiTint.Context context;

    private final float[] x = new float[4];
    private final float[] y = new float[4];
    private final float[] z = new float[4];
    private final float[] u = new float[4];
    private final float[] v = new float[4];
    private final int[] color = new int[4];
    private boolean hasUv;
    private int count;

    // reused for every element on the render thread
    SubdividedTint use(VertexConsumer delegate, GuiTint.Context context) {
        this.delegate = delegate;
        this.context = context;
        this.count = 0;
        this.hasUv = false;
        return this;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        if (this.count == 4) {
            this.flush();
        }
        int i = this.count++;
        this.x[i] = x;
        this.y[i] = y;
        this.z[i] = z;
        this.u[i] = 0.0f;
        this.v[i] = 0.0f;
        this.color[i] = -1;
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        this.color[this.count - 1] = (a << 24) | (r << 16) | (g << 8) | b;
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        this.color[this.count - 1] = color;
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        this.u[this.count - 1] = u;
        this.v[this.count - 1] = v;
        this.hasUv = true;
        return this;
    }

    // not in position/uv/color formats: the buffer would skip these too
    @Override
    public VertexConsumer setUv1(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer setUv3(float u, float v) {
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        return this;
    }

    void finish() {
        if (this.count == 4) {
            this.flush();
        }
        this.count = 0;
    }

    // corners in drawing order: 0, 1 along t, 2 opposite, 3 along s
    private void flush() {
        this.count = 0;
        float minX = Math.min(Math.min(this.x[0], this.x[1]), Math.min(this.x[2], this.x[3]));
        float maxX = Math.max(Math.max(this.x[0], this.x[1]), Math.max(this.x[2], this.x[3]));
        float minY = Math.min(Math.min(this.y[0], this.y[1]), Math.min(this.y[2], this.y[3]));
        float maxY = Math.max(Math.max(this.y[0], this.y[1]), Math.max(this.y[2], this.y[3]));

        // no item light reaches it: darkness is flat, keep the quad whole
        if (!this.context.lightsReach(minX, minY, maxX, maxY)) {
            float darkness = this.context.ambientDarkness();
            this.emit(0.0f, 0.0f, darkness);
            this.emit(0.0f, 1.0f, darkness);
            this.emit(1.0f, 1.0f, darkness);
            this.emit(1.0f, 0.0f, darkness);
            return;
        }

        int cols = divisions(this.x[3] - this.x[0], this.y[3] - this.y[0], this.context.cell());
        int rows = divisions(this.x[1] - this.x[0], this.y[1] - this.y[0], this.context.cell());
        if (cols == 1 && rows == 1) {
            this.emit(0.0f, 0.0f, this.context.darkness(this.x[0], this.y[0]));
            this.emit(0.0f, 1.0f, this.context.darkness(this.x[1], this.y[1]));
            this.emit(1.0f, 1.0f, this.context.darkness(this.x[2], this.y[2]));
            this.emit(1.0f, 0.0f, this.context.darkness(this.x[3], this.y[3]));
            return;
        }

        // each grid point once, shared by up to four cells
        float[] darkness = new float[(cols + 1) * (rows + 1)];
        for (int row = 0; row <= rows; row++) {
            float t = row / (float) rows;
            for (int col = 0; col <= cols; col++) {
                float s = col / (float) cols;
                darkness[row * (cols + 1) + col] = this.context.darkness(this.lerpX(s, t), this.lerpY(s, t));
            }
        }

        for (int row = 0; row < rows; row++) {
            float t0 = row / (float) rows;
            float t1 = (row + 1) / (float) rows;
            for (int col = 0; col < cols; col++) {
                float s0 = col / (float) cols;
                float s1 = (col + 1) / (float) cols;
                this.emit(s0, t0, darkness[row * (cols + 1) + col]);
                this.emit(s0, t1, darkness[(row + 1) * (cols + 1) + col]);
                this.emit(s1, t1, darkness[(row + 1) * (cols + 1) + col + 1]);
                this.emit(s1, t0, darkness[row * (cols + 1) + col + 1]);
            }
        }
    }

    // up to two cells (an item, a glyph) stay whole: colors still blend between the corners
    private static int divisions(float dx, float dy, int cell) {
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 2 * cell) {
            return 1;
        }
        return Mth.clamp(Mth.ceil(length / cell), 1, MAX_DIVISIONS);
    }

    private float lerpX(float s, float t) {
        return this.x[0] * (1.0f - s) * (1.0f - t) + this.x[1] * (1.0f - s) * t + this.x[2] * s * t + this.x[3] * s * (1.0f - t);
    }

    private float lerpY(float s, float t) {
        return this.y[0] * (1.0f - s) * (1.0f - t) + this.y[1] * (1.0f - s) * t + this.y[2] * s * t + this.y[3] * s * (1.0f - t);
    }

    private void emit(float s, float t, float darkness) {
        float w0 = (1.0f - s) * (1.0f - t);
        float w1 = (1.0f - s) * t;
        float w2 = s * t;
        float w3 = s * (1.0f - t);

        this.delegate.addVertex(
                this.x[0] * w0 + this.x[1] * w1 + this.x[2] * w2 + this.x[3] * w3,
                this.y[0] * w0 + this.y[1] * w1 + this.y[2] * w2 + this.y[3] * w3,
                this.z[0] * w0 + this.z[1] * w1 + this.z[2] * w2 + this.z[3] * w3);
        if (this.hasUv) {
            this.delegate.setUv(
                    this.u[0] * w0 + this.u[1] * w1 + this.u[2] * w2 + this.u[3] * w3,
                    this.v[0] * w0 + this.v[1] * w1 + this.v[2] * w2 + this.v[3] * w3);
        }
        this.delegate.setColor(GuiTint.tint(this.mixColor(w0, w1, w2, w3), darkness));
    }

    private int mixColor(float w0, float w1, float w2, float w3) {
        if (this.color[0] == this.color[1] && this.color[0] == this.color[2] && this.color[0] == this.color[3]) {
            return this.color[0];
        }
        int result = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            float channel = ((this.color[0] >>> shift) & 0xFF) * w0 + ((this.color[1] >>> shift) & 0xFF) * w1
                    + ((this.color[2] >>> shift) & 0xFF) * w2 + ((this.color[3] >>> shift) & 0xFF) * w3;
            result |= Mth.clamp(Math.round(channel), 0, 255) << shift;
        }
        return result;
    }
}
