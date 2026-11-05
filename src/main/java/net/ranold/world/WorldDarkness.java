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

        // level track = gameplay (mobs burn ~23640), factor track = render (End flashes add to it). linear: a curve only delays dawn
        float skyLevel = camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_LEVEL, partialTicks);
        float dayness = Math.max(
                Mth.clamp((skyLevel - NIGHT_SKY_LEVEL) / (DAY_SKY_LEVEL - NIGHT_SKY_LEVEL), 0.0f, 1.0f),
                Mth.clamp((vanillaSkyFactor - NIGHT_SKY_FACTOR) / (1.0f - NIGHT_SKY_FACTOR), 0.0f, 1.0f));

        MoonPhase phase = camera.attributeProbe().getValue(EnvironmentAttributes.MOON_PHASE, partialTicks);

        int fromFull = Math.min(phase.index(), MoonPhase.COUNT - phase.index());
        TorchlitConfig config = TorchlitConfig.get();
        ClientLevel current = Minecraft.getInstance().level;
        float dimension = current == null ? 1.0f : TorchlitConfig.dimensionStrength(current.dimension());
        float moonBrightness = Mth.lerp(config.moonInfluence / 100.0f * dimension,
                1.0f, MOON_BRIGHTNESS[Mth.clamp(fromFull, 0, MOON_BRIGHTNESS.length - 1)]);

        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            // vanilla weather darkening does nothing at night
            float rain = level.getRainLevel(partialTicks);
            float thunder = level.getThunderLevel(partialTicks);
            float cover = (RAIN_MOON_COVER * rain + THUNDER_MOON_COVER * thunder) * config.weatherInfluence / 100.0f * dimension;
            moonBrightness *= Mth.clamp(1.0f - cover, 0.0f, 1.0f);
        }

        return Mth.lerp(dayness, moonBrightness, 1.0f);
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
