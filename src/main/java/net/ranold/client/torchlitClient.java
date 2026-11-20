package net.ranold.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.ranold.config.TorchlitConfig;

public class torchlitClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TorchlitConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(EyeAdaptation::tick);
    }
}
