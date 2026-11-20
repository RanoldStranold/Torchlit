package net.ranold.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

// tints each vertex color by the darkness at its own position (text glyphs and other small shapes)
final class VertexTint implements VertexConsumer {

    private VertexConsumer delegate;
    private GuiTint.Context context;
    private float darkness;

    // reused for every element on the render thread
    VertexTint use(VertexConsumer delegate, GuiTint.Context context) {
        this.delegate = delegate;
        this.context = context;
        return this;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.darkness = this.context.darkness(x, y);
        this.delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        int tinted = GuiTint.tint((a << 24) | (r << 16) | (g << 8) | b, this.darkness);
        this.delegate.setColor((tinted >> 16) & 0xFF, (tinted >> 8) & 0xFF, tinted & 0xFF, a);
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        this.delegate.setColor(GuiTint.tint(color, this.darkness));
        return this;
    }

    @Override
    public void addVertex(float x, float y, float z, int color, float u, float v,
                          int overlayCoords, int lightCoords, float nx, float ny, float nz) {
        this.delegate.addVertex(x, y, z, GuiTint.tint(color, this.context.darkness(x, y)), u, v,
                overlayCoords, lightCoords, nx, ny, nz);
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        this.delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        this.delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        this.delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv3(float u, float v) {
        this.delegate.setUv3(u, v);
        return this;
    }

    @Override
    public VertexConsumer setOverlay(int packedOverlayCoords) {
        this.delegate.setOverlay(packedOverlayCoords);
        return this;
    }

    @Override
    public VertexConsumer setLight(int packedLightCoords) {
        this.delegate.setLight(packedLightCoords);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        this.delegate.setNormal(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        this.delegate.setLineWidth(width);
        return this;
    }
}
