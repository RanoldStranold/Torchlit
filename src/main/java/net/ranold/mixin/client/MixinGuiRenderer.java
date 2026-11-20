package net.ranold.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.ranold.client.GuiTint;

// every gui element is built here
@Mixin(GuiRenderer.class)
public abstract class MixinGuiRenderer {

    @WrapOperation(
            method = "addElementToMesh",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/state/gui/GuiElementRenderState;buildVertices(Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"))
    private void torchlit$tint(GuiElementRenderState element, VertexConsumer consumer, Operation<Void> original) {
        VertexConsumer tinted = GuiTint.wrap(element, consumer);
        original.call(element, tinted);
        GuiTint.finish(tinted);
    }
}
