package net.ranold.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.fabricmc.loader.api.FabricLoader;

public final class TorchlitModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // the screen needs cloth config
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return ModMenuApi.super.getModConfigScreenFactory();
        }
        return TorchlitConfigScreen::create;
    }
}
