package net.ranold.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.ranold.client.GuiShapeRecorder;

@Mixin(GuiGraphicsExtractor.class)
public abstract class MixinGuiGraphicsExtractor {

    // the Identifier overload calls this one; hooking both double-records
    @Inject(
            method = "innerBlit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"
                    + "Lcom/mojang/renderpearl/api/textures/GpuTextureView;"
                    + "Lcom/mojang/renderpearl/api/textures/GpuSampler;IIIIFFFFI)V",
            at = @At("HEAD"))
    private void torchlit$recordBlit(RenderPipeline pipeline, GpuTextureView texture, GpuSampler sampler,
                                     int x0, int y0, int x1, int y1,
                                     float u0, float u1, float v0, float v1,
                                     int color, CallbackInfo ci) {
        GuiShapeRecorder.record(texture, sampler, ((GuiGraphicsExtractor) (Object) this).pose(),
                x0, y0, x1, y1, u0, u1, v0, v1);
    }

    // tooltip backgrounds
    @Inject(method = "innerTiledBlit", at = @At("HEAD"))
    private void torchlit$recordTiledBlit(RenderPipeline pipeline, GpuTextureView texture, GpuSampler sampler,
                                          int tileWidth, int tileHeight, int x0, int y0, int x1, int y1,
                                          float u0, float u1, float v0, float v1,
                                          int color, CallbackInfo ci) {
        GuiShapeRecorder.record(texture, sampler, ((GuiGraphicsExtractor) (Object) this).pose(),
                x0, y0, x1, y1, u0, u1, v0, v1);
    }
}
