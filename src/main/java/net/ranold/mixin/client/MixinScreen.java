package net.ranold.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.ranold.client.GuiDarkness;
import net.ranold.client.GuiTint;
import net.ranold.client.InventoryDarkness;

@Mixin(Screen.class)
public abstract class MixinScreen {

    @Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void torchlit$removeBackgroundTint(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (InventoryDarkness.REMOVE_BACKGROUND_TINT && GuiDarkness.appliesTo((Screen) (Object) this)) {
            ci.cancel();
            return;
        }
        // the dim overlay behind the screen stays vanilla
        GuiTint.pause();
    }

    @Inject(method = "extractTransparentBackground", at = @At("RETURN"))
    private void torchlit$resumeAfterBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        GuiTint.resume();
    }

    // final method, so screens overriding extractRenderState can't skip it. tooltips come later in it, same tint
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"))
    private void torchlit$beginTint(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                    CallbackInfo ci) {
        if (GuiDarkness.appliesTo((Screen) (Object) this)) {
            GuiDarkness.begin();
        }
    }

    @Inject(
            method = "extractRenderStateWithTooltipAndSubtitles",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;extractDeferredElements(IIF)V"
            )
    )
    private void torchlit$computeTint(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                      CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (GuiDarkness.appliesTo(screen)) {
            GuiDarkness.compute(screen, mouseX, mouseY);
        }
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
    private void torchlit$endTint(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                  CallbackInfo ci) {
        if (GuiDarkness.appliesTo((Screen) (Object) this)) {
            GuiDarkness.end();
        }
    }
}
