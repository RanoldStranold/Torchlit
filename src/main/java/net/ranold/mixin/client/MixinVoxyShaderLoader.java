package net.ranold.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resources.Identifier;
import net.ranold.client.VoxyCompat;

// voxy reads shaders from its own jar, so patch the source as it loads
@Pseudo
@Mixin(targets = "me.cortex.voxy.client.core.gl.shader.ShaderLoader$ShaderLoadingParser")
public abstract class MixinVoxyShaderLoader {

    @Inject(method = "loadShaderAsset", at = @At("RETURN"), cancellable = true, require = 0)
    private static void torchlit$patchShader(Identifier id, CallbackInfoReturnable<String> cir) {
        String patched = VoxyCompat.patchShader(id, cir.getReturnValue());
        if (patched != cir.getReturnValue()) {
            cir.setReturnValue(patched);
        }
    }
}
