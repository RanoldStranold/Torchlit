package net.ranold.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.ranold.client.GuiDarkness;
import net.ranold.client.GuiShapeRecorder;
import net.ranold.client.InventoryDarkness;

@Mixin(Screen.class)
public abstract class MixinScreen {

    @Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void torchlit$removeBackgroundTint(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (InventoryDarkness.REMOVE_BACKGROUND_TINT && GuiDarkness.appliesTo((Screen) (Object) this)) {
            ci.cancel();
        }
    }

    // final method, so screens overriding extractRenderState can't skip it
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"))
    private void torchlit$beginRecording(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                         CallbackInfo ci) {
        if (GuiDarkness.appliesTo((Screen) (Object) this)) {
            GuiShapeRecorder.begin();
        }
    }

    @Inject(
            method = "extractRenderStateWithTooltipAndSubtitles",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;extractDeferredElements(IIF)V"
            )
    )
    private void torchlit$drawDarkness(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                       CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (GuiDarkness.appliesTo(screen)) {
            GuiDarkness.render(graphics, screen, GuiShapeRecorder.end(), mouseX, mouseY);

            GuiShapeRecorder.begin();
        }
    }

    // tooltips in a second pass, one pass left them half dim over the panel
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
    private void torchlit$drawDeferredDarkness(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                               CallbackInfo ci) {
        if (GuiDarkness.appliesTo((Screen) (Object) this)) {
            GuiDarkness.renderDeferred(graphics, GuiShapeRecorder.end());
        }
    }
}
