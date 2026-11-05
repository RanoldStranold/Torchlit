package net.ranold.mixin.client;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.ranold.world.WorldDarkness;

@Mixin(AtmosphericFogEnvironment.class)
public class MixinAtmosphericFogEnvironment {

    @Inject(method = "getBaseColor", at = @At("RETURN"), cancellable = true)
    private void torchlit$darkenFog(ClientLevel level, Camera camera, int renderDistance, float partialTicks,
                                    CallbackInfoReturnable<Vector3fc> cir) {
        float factor = WorldDarkness.fogFactor(level);
        if (factor == 1.0f) {
            return;
        }

        Vector3fc color = cir.getReturnValue();
        float min = WorldDarkness.MIN_FOG_CHANNEL;
        cir.setReturnValue(new Vector3f(
                Math.max(min, color.x() * factor),
                Math.max(min, color.y() * factor),
                Math.max(min, color.z() * factor)));
    }
}
