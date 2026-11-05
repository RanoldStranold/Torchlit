package net.ranold.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.ranold.client.DarknessField.PointLight;
import net.ranold.config.TorchlitConfig;

public final class HotbarDarkness {

    private static final int HOTBAR_WIDTH = 182;
    private static final int HOTBAR_HEIGHT = 22;
    private static final int SLOT_SPACING = 20;
    private static final int FIRST_SLOT_CENTRE_OFFSET = 11;
    private static final int SLOT_COUNT = 9;

    private static final int STATUS_BAR_HEIGHT = 32;

    private static final LightSmoother SMOOTHER = new LightSmoother(
            InventoryDarkness.LIGHT_SWITCH_LEVELS, InventoryDarkness.LIGHT_SWITCH_SECONDS);

    private HotbarDarkness() {
    }

    public static void render(GuiGraphicsExtractor graphics, List<GuiShapeRecorder.Blit> drawn) {
        if (TorchlitConfig.get().hotbarDarkness <= 0) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }

        if (client.player.isCreative() && !TorchlitConfig.get().affectCreative) {
            return;
        }

        if (!TorchlitConfig.isActiveIn(client.level.dimension())) {
            return;
        }

        float ambient = SMOOTHER.update(AmbientLight.atPlayer(client), InventoryDarkness.SMOOTHING_SECONDS);
        if (ambient >= InventoryDarkness.FULL_BRIGHT_LEVEL) {
            return;
        }

        int left = graphics.guiWidth() / 2 - HOTBAR_WIDTH / 2;
        int hotbarTop = graphics.guiHeight() - HOTBAR_HEIGHT;

        List<PointLight> lights = collectLights(client.player, left, hotbarTop);

        int top = TorchlitConfig.get().darkenStatusBars ? hotbarTop - STATUS_BAR_HEIGHT : hotbarTop;
        int right = left + HOTBAR_WIDTH;
        int bottom = graphics.guiHeight();
        List<GuiShapeRecorder.Blit> hud = drawn.stream()
                .filter(blit -> blit.intersects(left, top, right, bottom))
                .toList();

        DarknessField.submit(graphics, hud, ambient, lights, TorchlitConfig.get().hotbarDarkness / 100.0f);
    }

    private static List<PointLight> collectLights(Player player, int left, int top) {
        List<PointLight> lights = new ArrayList<>();
        Inventory inventory = player.getInventory();
        float centreY = top + HOTBAR_HEIGHT / 2.0f;

        if (TorchlitConfig.get().itemsEmitLight) {
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                int luminance = LightSources.luminanceOf(inventory.getItem(slot));
                if (luminance > 0) {
                    lights.add(GuiDarkness.itemLight(slotCentreX(left, slot), centreY, luminance));
                }
            }
        }

        int selected = inventory.getSelectedSlot();
        if (InventoryDarkness.HELD_ITEM_RADIUS > 0.0f && selected >= 0 && selected < SLOT_COUNT) {
            lights.add(new PointLight(
                    slotCentreX(left, selected),
                    centreY,
                    InventoryDarkness.FULL_BRIGHT_LEVEL,
                    InventoryDarkness.HELD_ITEM_RADIUS));
        }

        return lights;
    }

    private static float slotCentreX(int left, int slot) {
        return left + FIRST_SLOT_CENTRE_OFFSET + slot * SLOT_SPACING;
    }
}
