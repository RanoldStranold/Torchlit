package net.ranold.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.ranold.client.EyeAdaptation;
import net.ranold.world.WorldDarkness;

@Mixin(LightmapRenderStateExtractor.class)
public class MixinLightmapRenderStateExtractor {

    @Shadow
    private boolean needsUpdate;

    // vanilla rebuilds once a tick; eye adaptation needs every frame or it steps
    @Inject(method = "extract", at = @At("HEAD"))
    private void torchlit$everyFrame(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
        EyeAdaptation.frame(Minecraft.getInstance());
        if (EyeAdaptation.isActive()) {
            this.needsUpdate = true;
        }
    }

    @Inject(method = "extract", at = @At("TAIL"))
    private void torchlit$darkenSky(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {

        // only on a rebuild, else skyFactor compounds
        if (renderState.needsUpdate) {
            renderState.skyFactor = WorldDarkness.skyFactor(renderState.skyFactor, partialTicks);
        }
    }
}
