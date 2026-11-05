package net.ranold.mixin.client;

import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import net.minecraft.resources.Identifier;
import net.ranold.world.WorldDarkness;

// lightmap color target must match the lightmap texture format (MixinLightmap)
@Mixin(RenderPipeline.Builder.class)
public abstract class MixinRenderPipelineBuilder {

    private static final Identifier LIGHTMAP = Identifier.withDefaultNamespace("pipeline/lightmap");

    @Shadow
    private Optional<Identifier> location;

    @Shadow
    @Final
    private ColorTargetState[] colorTargetStates;

    @Inject(method = "build", at = @At("HEAD"))
    private void torchlit$highPrecisionLightmap(CallbackInfoReturnable<RenderPipeline> cir) {
        if (!this.location.map(LIGHTMAP::equals).orElse(false)) {
            return;
        }
        ColorTargetState target = this.colorTargetStates[0];
        if (target != null) {
            this.colorTargetStates[0] = new ColorTargetState(
                    target.blendFunction(), WorldDarkness.LIGHTMAP_FORMAT, target.writeMask());
        }
    }
}
