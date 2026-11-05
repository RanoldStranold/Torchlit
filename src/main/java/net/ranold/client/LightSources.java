package net.ranold.client;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public final class LightSources {

    private LightSources() {
    }

    public static int luminanceOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        if (stack.getItem() instanceof BlockItem blockItem) {
            return blockItem.getBlock().defaultBlockState().getLightEmission();
        }

        return 0;
    }
}
