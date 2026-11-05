package net.ranold.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.ranold.config.TorchlitConfig;
import net.ranold.world.WorldDarkness;

// eyes follow the brightest light around and in view. only ever takes light away or glares
public final class EyeAdaptation {

    private static final float ADJUST_TO_BRIGHT_SECONDS = 2.0f;
    private static final float ADJUST_TO_DARK_SECONDS = 20.0f;

    private static final float GLARE_DEAD_ZONE_LEVELS = 1.0f;
    private static final float FULL_GLARE_LEVELS = 10.0f;

    private static final float SCENE_SMOOTHING_SECONDS = 0.35f;

    // GLARE and SHADE_* must match lightmap.fsh
    public static final float GLARE = 1.5f;

    public static final float SHADE_START_LEVELS = 2.0f;
    public static final float SHADE_FULL_LEVELS = 11.0f;
    public static final float SHADE = 0.72f;

    private static final double RAY_LENGTH = 32.0;
    private static final float CONE_DEGREES = 12.0f;

    private static final float MAX_FRAME_SECONDS = 0.25f;

    private static float measured = Float.NaN;

    private static float scene = Float.NaN;

    private static float adapted = Float.NaN;
    private static float glare;
    private static boolean active;
    private static long lastFrameNanos;

    private EyeAdaptation() {
    }

    public static float adapted() {
        return active ? adapted : 0.0f;
    }

    public static float glare() {
        return active ? glare : 0.0f;
    }

    // whole part = adapted level x100, fraction = glare
    public static float packed() {
        if (!active || !TorchlitConfig.get().enabled) {
            return 0.0f;
        }
        float strength = strength();
        return Math.round(adapted * strength * 100.0f) + Math.min(glare * strength, 0.999f);
    }

    public static boolean isActive() {
        return active;
    }

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.level == null || player == null) {
            measured = Float.NaN;
            scene = Float.NaN;
            adapted = Float.NaN;
            glare = 0.0f;
            active = false;
            return;
        }
        if (client.isPaused()) {
            return;
        }

        measured = sceneLight(client.level, player);

        active = WorldDarkness.isDarkening() && strength() > 0.0f;
        if (Float.isNaN(adapted) || !active) {
            scene = measured;
            adapted = measured;
            glare = 0.0f;
        }
    }

    public static void frame(Minecraft client) {
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 0.0f : (now - lastFrameNanos) / 1_000_000_000.0f;
        lastFrameNanos = now;

        if (!active || Float.isNaN(measured) || Float.isNaN(adapted) || client.isPaused()) {
            return;
        }
        if (dt >= MAX_FRAME_SECONDS) {
            scene = measured;
            adapted = measured;
        } else {
            scene += (measured - scene) * (1.0f - (float) Math.exp(-dt / SCENE_SMOOTHING_SECONDS));
            float seconds = scene > adapted ? ADJUST_TO_BRIGHT_SECONDS : ADJUST_TO_DARK_SECONDS;
            adapted += (scene - adapted) * (1.0f - (float) Math.exp(-dt / seconds));
        }

        glare = Mth.clamp((scene - adapted - GLARE_DEAD_ZONE_LEVELS) / FULL_GLARE_LEVELS, 0.0f, 1.0f);
    }

    public static float perceived(float light) {
        if (!active || !TorchlitConfig.get().enabled) {
            return light;
        }
        float strength = strength();
        float t = Mth.clamp(light / 15.0f, 0.0f, 1.0f);
        float result = light * (1.0f + glare * strength * GLARE * smoothstep(0.15f, 0.8f, t));
        float deficit = adapted * strength - light;
        return result * (1.0f - SHADE * smoothstep(SHADE_START_LEVELS, SHADE_FULL_LEVELS, deficit));
    }

    private static float sceneLight(Level level, LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        float atEye = AmbientLight.at(level, BlockPos.containing(eye));

        float pitch = player.getXRot();
        float yaw = player.getYRot();
        float sum = 0.0f;
        int rays = 0;
        for (float[] offset : new float[][] {
                {0, 0}, {CONE_DEGREES, 0}, {-CONE_DEGREES, 0}, {0, CONE_DEGREES}, {0, -CONE_DEGREES}}) {
            Vec3 direction = Vec3.directionFromRotation(pitch + offset[0], yaw + offset[1]);
            sum += lightAlong(level, player, eye, direction);
            rays++;
        }

        // max: a fire beside you sets your eyes whichever way you face
        return Math.max(atEye, sum / rays);
    }

    private static float lightAlong(Level level, LocalPlayer player, Vec3 eye, Vec3 direction) {
        Vec3 end = eye.add(direction.scale(RAY_LENGTH));
        BlockHitResult hit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
        BlockPos pos = hit.getType() == HitResult.Type.BLOCK
                ? hit.getBlockPos().relative(hit.getDirection())
                : BlockPos.containing(end);
        return AmbientLight.at(level, pos);
    }

    private static float strength() {
        return TorchlitConfig.get().eyeAdaptation / 100.0f;
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }
}
