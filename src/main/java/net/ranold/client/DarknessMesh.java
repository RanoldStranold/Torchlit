package net.ranold.client;

import org.jspecify.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

// a grid only so alpha can vary across a sprite (smooth torch glow)
public final class DarknessMesh implements GuiElementRenderState {

    private final GpuTextureView texture;
    private final GpuSampler sampler;
    private final Matrix3x2f pose;
    private final int originX;
    private final int originY;
    private final int width;
    private final int height;
    private final int cols;
    private final int rows;

    private final int[] vertexAlpha;

    private final float minU;
    private final float minV;
    private final float maxU;
    private final float maxV;
    private final ScreenRectangle bounds;
    @Nullable
    private final ScreenRectangle scissorArea;

    public DarknessMesh(GpuTextureView texture, GpuSampler sampler, Matrix3x2f pose,
                        int originX, int originY, int width, int height,
                        int cols, int rows, int[] vertexAlpha,
                        float minU, float minV, float maxU, float maxV,
                        @Nullable ScreenRectangle scissorArea) {
        this.texture = texture;
        this.sampler = sampler;
        this.pose = pose;
        this.originX = originX;
        this.originY = originY;
        this.width = width;
        this.height = height;
        this.cols = cols;
        this.rows = rows;
        this.vertexAlpha = vertexAlpha;
        this.minU = minU;
        this.minV = minV;
        this.maxU = maxU;
        this.maxV = maxV;
        this.bounds = new ScreenRectangle(originX, originY, width, height);
        this.scissorArea = scissorArea;
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        Vector2f corner = new Vector2f();

        for (int row = 0; row < this.rows; row++) {
            float topFraction = row / (float) this.rows;
            float bottomFraction = (row + 1) / (float) this.rows;

            for (int col = 0; col < this.cols; col++) {
                int topLeft = alphaAt(col, row);
                int topRight = alphaAt(col + 1, row);
                int bottomRight = alphaAt(col + 1, row + 1);
                int bottomLeft = alphaAt(col, row + 1);

                if ((topLeft | topRight | bottomRight | bottomLeft) == 0) {
                    continue;
                }

                float leftFraction = col / (float) this.cols;
                float rightFraction = (col + 1) / (float) this.cols;

                vertex(consumer, corner, leftFraction, topFraction, topLeft);
                vertex(consumer, corner, leftFraction, bottomFraction, bottomLeft);
                vertex(consumer, corner, rightFraction, bottomFraction, bottomRight);
                vertex(consumer, corner, rightFraction, topFraction, topRight);
            }
        }
    }

    private int alphaAt(int col, int row) {
        return this.vertexAlpha[row * (this.cols + 1) + col];
    }

    private void vertex(VertexConsumer consumer, Vector2f scratch,
                        float xFraction, float yFraction, int alpha) {
        scratch.set(this.originX + xFraction * this.width, this.originY + yFraction * this.height);
        this.pose.transformPosition(scratch);

        consumer.addVertex(scratch.x, scratch.y, 0.0f)
                .setUv(this.minU + xFraction * (this.maxU - this.minU),
                        this.minV + yFraction * (this.maxV - this.minV))
                .setColor(alpha << 24);
    }

    @Override
    public RenderPipeline pipeline() {
        return DarknessPipeline.GUI_MASK;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.singleTexture(this.texture, this.sampler);
    }

    @Override
    @Nullable
    public ScreenRectangle scissorArea() {
        return this.scissorArea;
    }

    @Override
    public ScreenRectangle bounds() {
        return this.bounds;
    }
}
