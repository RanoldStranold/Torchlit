package net.ranold.config;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

// entry in sodium's video settings that opens our screen. only loaded by sodium
public final class TorchlitSodiumConfig implements ConfigEntryPoint {

    // torch flame orange
    private static final int THEME = 0xFFF0A030;

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        // the screen needs cloth config
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return;
        }
        builder.registerOwnModOptions()
                .setName("Torchlit")
                .setColorTheme(builder.createColorTheme().setBaseThemeRGB(THEME))
                .setNonTintedIcon(Identifier.fromNamespaceAndPath("torchlit", "icon.png"))
                .addPage(builder.createExternalPage()
                        .setName(Component.translatable("torchlit.config.sodium_page"))
                        .setScreenConsumer(parent -> Minecraft.getInstance()
                                .setScreenAndShow(TorchlitConfigScreen.create(parent))));
    }
}
