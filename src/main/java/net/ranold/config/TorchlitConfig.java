package net.ranold.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.ranold.torchlit;

public final class TorchlitConfig {

    public enum Preset {
        CUSTOM, CASUAL, DEFAULT, HARDCORE
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static TorchlitConfig instance;

    public boolean enabled = true;

    public int worldDarkness = 100;
    public int moonInfluence = 100;
    public int weatherInfluence = 100;
    public int fogDarkness = 100;
    public int eyeAdaptation = 100;
    public Map<String, Integer> dimensionDarkness = new LinkedHashMap<>();

    // old on/off format, converted in sanitise
    private Map<String, Boolean> dimensions;

    public int containerDarkness = 99;
    public int hotbarDarkness = 98;
    public boolean darkenBooksAndSigns = true;
    public boolean darkenStatusBars = false;
    public boolean itemsEmitLight = true;
    public boolean affectCreative = false;

    public Preset preset = Preset.DEFAULT;

    public static TorchlitConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    // unknown dimensions are 100% and get recorded for the screen
    public static float dimensionStrength(ResourceKey<Level> dimension) {
        TorchlitConfig config = get();
        String id = dimension.identifier().toString();
        Integer percent = config.dimensionDarkness.get(id);
        if (percent == null) {
            config.dimensionDarkness.put(id, 100);
            save();
            return 1.0f;
        }
        return percent / 100.0f;
    }

    public static boolean isActiveIn(ResourceKey<Level> dimension) {
        return get().enabled && dimensionStrength(dimension) > 0.0f;
    }

    public void apply(Preset target) {
        switch (target) {
            case CASUAL -> setAll(50, 50, 50, false);
            case DEFAULT -> {
                TorchlitConfig defaults = new TorchlitConfig();
                setAll(defaults.worldDarkness, defaults.containerDarkness, defaults.hotbarDarkness,
                        defaults.darkenStatusBars);
            }
            case HARDCORE -> setAll(100, 100, 100, true);
            case CUSTOM -> {
            }
        }
        this.preset = target;
    }

    public Preset matchingPreset() {
        for (Preset candidate : new Preset[] {Preset.CASUAL, Preset.DEFAULT, Preset.HARDCORE}) {
            TorchlitConfig probe = new TorchlitConfig();
            probe.apply(candidate);
            if (probe.worldDarkness == this.worldDarkness
                    && probe.moonInfluence == this.moonInfluence
                    && probe.weatherInfluence == this.weatherInfluence
                    && probe.fogDarkness == this.fogDarkness
                    && probe.eyeAdaptation == this.eyeAdaptation
                    && probe.containerDarkness == this.containerDarkness
                    && probe.hotbarDarkness == this.hotbarDarkness
                    && probe.darkenStatusBars == this.darkenStatusBars) {
                return candidate;
            }
        }
        return Preset.CUSTOM;
    }

    private void setAll(int world, int containers, int hotbar, boolean statusBars) {
        this.worldDarkness = world;
        this.moonInfluence = world;
        this.weatherInfluence = world;
        this.fogDarkness = world;
        this.eyeAdaptation = world;
        this.containerDarkness = containers;
        this.hotbarDarkness = hotbar;
        this.darkenStatusBars = statusBars;
    }

    public static void load() {
        Path path = path();
        TorchlitConfig loaded = null;
        if (Files.exists(path)) {
            try {
                loaded = GSON.fromJson(Files.readString(path), TorchlitConfig.class);
            } catch (Exception e) {
                torchlit.LOGGER.warn("Could not read {}, using defaults", path, e);
            }
        }
        instance = loaded != null ? loaded : new TorchlitConfig();
        instance.sanitise();
        save();
    }

    public static void save() {
        if (instance == null) {
            return;
        }
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(instance));
        } catch (Exception e) {
            torchlit.LOGGER.warn("Could not write {}", path, e);
        }
    }

    private void sanitise() {
        this.worldDarkness = clampPercent(this.worldDarkness);
        this.moonInfluence = clampPercent(this.moonInfluence);
        this.weatherInfluence = clampPercent(this.weatherInfluence);
        this.fogDarkness = clampPercent(this.fogDarkness);
        this.eyeAdaptation = clampPercent(this.eyeAdaptation);
        this.containerDarkness = clampPercent(this.containerDarkness);
        this.hotbarDarkness = clampPercent(this.hotbarDarkness);
        if (this.dimensionDarkness == null) {
            this.dimensionDarkness = new LinkedHashMap<>();
        }
        if (this.dimensions != null) {
            this.dimensions.forEach((id, on) -> this.dimensionDarkness.putIfAbsent(id, Boolean.TRUE.equals(on) ? 100 : 0));
            this.dimensions = null;
        }
        this.dimensionDarkness.replaceAll((id, percent) -> clampPercent(percent == null ? 100 : percent));
        for (ResourceKey<Level> vanilla : java.util.List.of(Level.OVERWORLD, Level.NETHER, Level.END)) {
            this.dimensionDarkness.putIfAbsent(vanilla.identifier().toString(), 100);
        }
        if (this.preset == null) {
            this.preset = Preset.CUSTOM;
        }
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("torchlit.json");
    }
}
