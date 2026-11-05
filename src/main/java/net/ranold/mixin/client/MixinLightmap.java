package net.ranold.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.renderpearl.api.GpuFormat;

import net.minecraft.client.renderer.Lightmap;
import net.ranold.client.EyeAdaptation;
import net.ranold.world.WorldDarkness;

// lightmap uniforms: 0 sky, 1 block, 2 night vision, 3 darkness, 4 boss, 5 brightness, then 4 vec3
@Mixin(Lightmap.class)
public class MixinLightmap {

    // 16-bit lightmap, must match MixinRenderPipelineBuilder
    @ModifyArg(
            method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/api/device/GpuDevice;createTexture(Ljava/lang/String;ILcom/mojang/renderpearl/api/GpuFormat;IIII)Lcom/mojang/renderpearl/api/textures/GpuTexture;"),
            index = 2)
    private GpuFormat torchlit$highPrecisionTexture(GpuFormat format) {
        return WorldDarkness.LIGHTMAP_FORMAT;
    }

    // ordinal 5 = BrightnessFactor. 2 extra floats fill the padding before the first vec3 (24 -> 32 bytes). padding now full
    @Redirect(
            method = "render",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
                    ordinal = 5))
    private Std140Builder torchlit$darkeningFactor(Std140Builder builder, float brightnessFactor) {
        return builder.putFloat(brightnessFactor)
                .putFloat(WorldDarkness.darkeningFactor())
                .putFloat(EyeAdaptation.packed());
    }
}
