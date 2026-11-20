package net.ranold.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.ranold.client.DarknessField.PointLight;
import net.ranold.config.TorchlitConfig;
import net.ranold.mixin.client.AbstractContainerScreenAccessor;

public final class GuiDarkness {

    static final float SLOT_CENTRE = 8.0f;

    private static final LightSmoother SMOOTHER = new LightSmoother(
            InventoryDarkness.LIGHT_SWITCH_LEVELS, InventoryDarkness.LIGHT_SWITCH_SECONDS);

    private GuiDarkness() {
    }

    private static final GuiTint.Context CONTEXT = new GuiTint.Context();

    public static boolean appliesTo(Screen screen) {
        if (screen instanceof AbstractContainerScreen<?>) {
            return TorchlitConfig.get().containerDarkness > 0;
        }
        return TorchlitConfig.get().darkenBooksAndSigns
                && TorchlitConfig.get().containerDarkness > 0
                && isReading(screen);
    }

    public static boolean belongsToBlock(Screen screen) {
        return screen instanceof LecternScreen
                || screen instanceof AbstractSignEditScreen
                || (screen instanceof AbstractContainerScreen<?> && !(screen instanceof InventoryScreen));
    }

    public static boolean exemptsWidgets(Screen screen) {
        return screen != null && !(screen instanceof AbstractContainerScreen<?>) && appliesTo(screen);
    }

    private static boolean isReading(Screen screen) {

        return screen instanceof BookViewScreen
                || screen instanceof BookEditScreen
                || screen instanceof BookSignScreen
                || screen instanceof AbstractSignEditScreen;
    }

    public static void resetSmoothing() {
        SMOOTHER.reset();
    }

    // everything the screen adds from here is tinted once compute() has set the light
    public static void begin() {
        CONTEXT.resetUnbounded();
        GuiTint.begin(CONTEXT);
    }

    public static void end() {
        GuiTint.end();
    }

    public static void compute(Screen screen, int mouseX, int mouseY) {
        if (!appliesTo(screen)) {
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

        float available = AmbientLight.atOpenContainer(client);
        if (!(screen instanceof AbstractContainerScreen<?>)) {
            available = Math.max(available, heldLight(client.player));
        }
        float ambient = SMOOTHER.update(available, InventoryDarkness.SMOOTHING_SECONDS);

        if (ambient >= InventoryDarkness.FULL_BRIGHT_LEVEL) {
            return;
        }

        List<PointLight> lights = List.of();
        if (TorchlitConfig.get().itemsEmitLight && screen instanceof AbstractContainerScreen<?>) {
            AbstractContainerScreenAccessor access = (AbstractContainerScreenAccessor) screen;
            lights = collectLights(access.torchlit$menu(),
                    access.torchlit$leftPos(), access.torchlit$topPos(), mouseX, mouseY);
        }

        CONTEXT.set(ambient, lights, TorchlitConfig.get().containerDarkness / 100.0f);
    }

    private static List<PointLight> collectLights(AbstractContainerMenu menu, int panelX, int panelY,
                                                  int mouseX, int mouseY) {
        List<PointLight> lights = new ArrayList<>();

        for (Slot slot : menu.slots) {
            int luminance = LightSources.luminanceOf(slot.getItem());
            if (luminance > 0) {
                lights.add(itemLight(panelX + slot.x + SLOT_CENTRE, panelY + slot.y + SLOT_CENTRE,
                        luminance));
            }
        }

        ItemStack carried = menu.getCarried();
        int carriedLuminance = LightSources.luminanceOf(carried);
        if (carriedLuminance > 0) {
            lights.add(itemLight(mouseX, mouseY, carriedLuminance));
        }

        return lights;
    }

    private static float heldLight(LocalPlayer player) {
        if (!TorchlitConfig.get().itemsEmitLight) {
            return 0.0f;
        }
        return Math.max(LightSources.luminanceOf(player.getMainHandItem()),
                LightSources.luminanceOf(player.getOffhandItem()));
    }

    static PointLight itemLight(float x, float y, int luminance) {
        return new PointLight(x, y, luminance, luminance * InventoryDarkness.LIGHT_RADIUS_PER_LUMINANCE);
    }
}
