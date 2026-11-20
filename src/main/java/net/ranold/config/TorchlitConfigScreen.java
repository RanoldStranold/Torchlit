package net.ranold.config;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class TorchlitConfigScreen {

    private TorchlitConfigScreen() {
    }

    public static Screen create(Screen parent) {
        TorchlitConfig config = TorchlitConfig.get();
        TorchlitConfig defaults = new TorchlitConfig();
        TorchlitConfig.Preset previousPreset = config.matchingPreset();
        TorchlitConfig.Preset[] chosenPreset = {previousPreset};

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("torchlit.config.title"));

        // globalized layout: tabs drew dead scroll arrows. sidebar removed by MixinClothGlobalizedScreen
        builder.setGlobalized(true);
        builder.setGlobalizedExpanded(false);
        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("torchlit.config.general"));
        general.addEntry(toggle(entries, "enabled", config.enabled, defaults.enabled,
                value -> config.enabled = value));

        ConfigCategory world = builder.getOrCreateCategory(Component.translatable("torchlit.config.world"));
        world.addEntry(percent(entries, "world_darkness", config.worldDarkness, defaults.worldDarkness,
                value -> config.worldDarkness = value));
        world.addEntry(percent(entries, "moon_influence", config.moonInfluence, defaults.moonInfluence,
                value -> config.moonInfluence = value));
        world.addEntry(percent(entries, "weather_influence", config.weatherInfluence, defaults.weatherInfluence,
                value -> config.weatherInfluence = value));
        world.addEntry(percent(entries, "fog_darkness", config.fogDarkness, defaults.fogDarkness,
                value -> config.fogDarkness = value));
        world.addEntry(percent(entries, "eye_adaptation", config.eyeAdaptation, defaults.eyeAdaptation,
                value -> config.eyeAdaptation = value));

        SubCategoryBuilder dimensions = entries.startSubCategory(Component.translatable("torchlit.config.dimensions"));
        for (Map.Entry<String, Integer> dimension : knownDimensions(config).entrySet()) {
            String id = dimension.getKey();
            dimensions.add(entries.startIntSlider(Component.literal(id), dimension.getValue(), 0, 100)
                    .setDefaultValue(100)
                    .setTextGetter(percent -> Component.literal(percent + "%"))
                    .setTooltip(Component.translatable("torchlit.config.dimension.tooltip"))
                    .setSaveConsumer(value -> config.dimensionDarkness.put(id, value))
                    .build());
        }
        world.addEntry(dimensions.setExpanded(true).build());

        ConfigCategory hud = builder.getOrCreateCategory(Component.translatable("torchlit.config.hud"));
        hud.addEntry(percent(entries, "container_darkness", config.containerDarkness, defaults.containerDarkness,
                value -> config.containerDarkness = value));
        hud.addEntry(percent(entries, "hotbar_darkness", config.hotbarDarkness, defaults.hotbarDarkness,
                value -> config.hotbarDarkness = value));
        hud.addEntry(entries.startIntSlider(Component.translatable("torchlit.config.light_detail"), config.lightDetail, 1, 16)
                .setDefaultValue(defaults.lightDetail)
                .setTextGetter(pixels -> Component.literal(pixels + " px"))
                .setTooltip(Component.translatable("torchlit.config.light_detail.tooltip"))
                .setSaveConsumer(value -> config.lightDetail = value)
                .build());
        hud.addEntry(toggle(entries, "books_and_signs", config.darkenBooksAndSigns, defaults.darkenBooksAndSigns,
                value -> config.darkenBooksAndSigns = value));
        hud.addEntry(toggle(entries, "status_bars", config.darkenStatusBars, defaults.darkenStatusBars,
                value -> config.darkenStatusBars = value));
        hud.addEntry(toggle(entries, "items_emit_light", config.itemsEmitLight, defaults.itemsEmitLight,
                value -> config.itemsEmitLight = value));
        hud.addEntry(toggle(entries, "affect_creative", config.affectCreative, defaults.affectCreative,
                value -> config.affectCreative = value));

        ConfigCategory presets = builder.getOrCreateCategory(Component.translatable("torchlit.config.presets"));
        presets.addEntry(entries.startEnumSelector(Component.translatable("torchlit.config.preset"),
                        TorchlitConfig.Preset.class, previousPreset)
                .setDefaultValue(TorchlitConfig.Preset.DEFAULT)
                .setEnumNameProvider(preset -> Component.translatable(
                        "torchlit.config.preset." + preset.name().toLowerCase(java.util.Locale.ROOT)))
                .setTooltip(Component.translatable("torchlit.config.preset.tooltip.casual"),
                        Component.translatable("torchlit.config.preset.tooltip.hardcore"))
                .setSaveConsumer(value -> chosenPreset[0] = value)
                .build());

        builder.setSavingRunnable(() -> {
            if (chosenPreset[0] != previousPreset && chosenPreset[0] != TorchlitConfig.Preset.CUSTOM) {
                config.apply(chosenPreset[0]);
            }
            config.preset = config.matchingPreset();
            TorchlitConfig.save();
        });

        Screen screen = builder.build();
        ConfigScreens.mark(screen);
        return screen;
    }

    private static Map<String, Integer> knownDimensions(TorchlitConfig config) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() != null) {
            for (ResourceKey<Level> level : client.getConnection().levels()) {
                config.dimensionDarkness.putIfAbsent(level.identifier().toString(), 100);
            }
        }
        return new TreeMap<>(config.dimensionDarkness);
    }

    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> percent(
            ConfigEntryBuilder entries, String key, int value, int defaultValue, Consumer<Integer> save) {
        return entries.startIntSlider(Component.translatable("torchlit.config." + key), value, 0, 100)
                .setDefaultValue(defaultValue)
                .setTextGetter(percent -> Component.literal(percent + "%"))
                .setTooltip(Component.translatable("torchlit.config." + key + ".tooltip"))
                .setSaveConsumer(save)
                .build();
    }

    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> toggle(
            ConfigEntryBuilder entries, String key, boolean value, boolean defaultValue, Consumer<Boolean> save) {
        return entries.startBooleanToggle(Component.translatable("torchlit.config." + key), value)
                .setDefaultValue(defaultValue)
                .setTooltip(Component.translatable("torchlit.config." + key + ".tooltip"))
                .setSaveConsumer(save)
                .build();
    }
}
