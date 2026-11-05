package net.ranold.client;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.ranold.torchlit;

public final class DarknessPipeline {

    // must be registered or it is never compiled
    public static final RenderPipeline GUI_MASK = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(torchlit.MOD_ID, "pipeline/gui_mask"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(torchlit.MOD_ID, "core/gui_mask"))
                    .build());

    private DarknessPipeline() {
    }

    public static void init() {
    }
}
