package net.ranold.world;

import com.mojang.renderpearl.api.GpuFormat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import net.ranold.config.TorchlitConfig;
import net.ranold.mixin.client.ClientLevelAccessor;

public final class WorldDarkness {

    private static final float NIGHT_SKY_FACTOR = 0.24f;

    private static final float NIGHT_SKY_LEVEL = 4.0f;
    private static final float DAY_SKY_LEVEL = 15.0f;

    public static final GpuFormat LIGHTMAP_FORMAT = GpuFormat.RGBA16_UNORM;

    public static final float MIN_FOG_CHANNEL = 0.03f;

    private static final float RAIN_MOON_COVER = 0.6f;
    private static final float RAIN_SUN_COVER = 0.3f;
    private static final float THUNDER_SUN_COVER = 0.3f;

    // must match WeatherAttributes RAIN / THUNDER alpha on sky_light_level and sky_light_factor
    private static final float VANILLA_RAIN_BLEND = 0.3125f;
    private static final float VANILLA_THUNDER_BLEND = 0.52734375f;

    // full, gibbous, half, crescent, new
    private static final float[] MOON_BRIGHTNESS = {1.0f, 0.75f, 0.45f, 0.06f, 0.0f};
    private static final float THUNDER_MOON_COVER = 0.35f;

    private WorldDarkness() {
    }

    public static float skyFactor(float vanilla, float partialTicks) {
        ClientLevel level = darkenedLevel();
        if (level == null) {
            return vanilla;
        }
        if (!level.dimensionType().hasSkyLight()) {
            return 0.0f;
        }
        return vanilla * nightFactor(vanilla, partialTicks);
    }

    public static float darkeningFactor() {
        ClientLevel level = darkenedLevel();
        if (level == null) {
            return 0.0f;
        }
        return TorchlitConfig.get().worldDarkness / 100.0f * TorchlitConfig.dimensionStrength(level.dimension());
    }

    public static float fogFactor(Level level) {
        if (!isDarkening()) {
            return 1.0f;
        }

        float strength = TorchlitConfig.get().fogDarkness / 100.0f * TorchlitConfig.dimensionStrength(level.dimension());
        if (level.dimension() == Level.NETHER) {
            return Mth.lerp(strength, 1.0f, 0.5f);
        }
        if (level.dimension() == Level.END) {
            return Mth.lerp(strength, 1.0f, 0.0f);
        }
        return 1.0f;
    }

    private static float nightFactor(float vanillaSkyFactor, float partialTicks) {
        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        ClientLevel level = Minecraft.getInstance().level;

        // vanilla weather pulls both tracks toward night; take it back out so a storm never counts as night
        float rainBlend = 0.0f;
        float thunderBlend = 0.0f;
        if (level != null && level.canHaveWeather()) {
            float thunder = level.getThunderLevel(1.0f);
            rainBlend = Math.max(0.0f, level.getRainLevel(1.0f) - thunder) * VANILLA_RAIN_BLEND;
            thunderBlend = thunder * VANILLA_THUNDER_BLEND;
        }
        float skyLevel = clearValue(camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_LEVEL, partialTicks),
                NIGHT_SKY_LEVEL, rainBlend, thunderBlend);
        float skyFactor = clearValue(vanillaSkyFactor, NIGHT_SKY_FACTOR, rainBlend, thunderBlend);

        // level track = gameplay (mobs burn ~23640), factor track = render (End flashes add to it). linear: a curve only delays dawn
        float dayness = Math.max(
                Mth.clamp((skyLevel - NIGHT_SKY_LEVEL) / (DAY_SKY_LEVEL - NIGHT_SKY_LEVEL), 0.0f, 1.0f),
                Mth.clamp((skyFactor - NIGHT_SKY_FACTOR) / (1.0f - NIGHT_SKY_FACTOR), 0.0f, 1.0f));

        MoonPhase phase = camera.attributeProbe().getValue(EnvironmentAttributes.MOON_PHASE, partialTicks);

        int fromFull = Math.min(phase.index(), MoonPhase.COUNT - phase.index());
        TorchlitConfig config = TorchlitConfig.get();
        float dimension = level == null ? 1.0f : TorchlitConfig.dimensionStrength(level.dimension());
        float moonBrightness = Mth.lerp(config.moonInfluence / 100.0f * dimension,
                1.0f, MOON_BRIGHTNESS[Mth.clamp(fromFull, 0, MOON_BRIGHTNESS.length - 1)]);

        // weather covers the moon by night and dims the sun by day, on top of vanilla's own storm dimming
        float nightCover = 0.0f;
        float dayCover = 0.0f;
        if (level != null) {
            float rain = level.getRainLevel(partialTicks);
            float thunder = level.getThunderLevel(partialTicks);
            float strength = config.weatherInfluence / 100.0f * dimension;
            nightCover = Mth.clamp((RAIN_MOON_COVER * rain + THUNDER_MOON_COVER * thunder) * strength, 0.0f, 1.0f);
            dayCover = Mth.clamp((RAIN_SUN_COVER * rain + THUNDER_SUN_COVER * thunder) * strength, 0.0f, 1.0f);
        }

        return Mth.lerp(dayness, moonBrightness * (1.0f - nightCover), 1.0f - dayCover);
    }

    // undoes WeatherAttributes: rain blends toward the night value by 0.3125 x (rain - thunder), then thunder by 0.527 x thunder
    private static float clearValue(float value, float night, float rainBlend, float thunderBlend) {
        float beforeThunder = (value - night * thunderBlend) / (1.0f - thunderBlend);
        return (beforeThunder - night * rainBlend) / (1.0f - rainBlend);
    }

    public static boolean isDarkening() {
        return darkenedLevel() != null;
    }

    private static ClientLevel darkenedLevel() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return null;
        }

        if (!TorchlitConfig.isActiveIn(level.dimension())) {
            return null;
        }

        // stand down fully: night vision scales toward 1 and can't lift a zeroed cell
        if (player.hasEffect(MobEffects.NIGHT_VISION)
                || (player.hasEffect(MobEffects.CONDUIT_POWER) && player.getWaterVision() > 0.0f)) {
            return null;
        }

        // lightning flash
        if (((ClientLevelAccessor) level).torchlit$getSkyFlashTime() > 0) {
            return null;
        }

        return level;
    }
}
