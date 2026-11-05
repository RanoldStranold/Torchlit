#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform LightmapInfo {
    float SkyFactor;
    float BlockFactor;
    float NightVisionFactor;
    float DarknessScale;
    float BossOverlayWorldDarkeningFactor;
    float BrightnessFactor;

    // torchlit: 2 extra floats in the padding before the first vec3
    float DarkeningFactor;
    float Exposure;
    vec3 BlockLightTint;
    vec3 SkyLightColor;
    vec3 AmbientColor;
    vec3 NightVisionColor;
} lightmapInfo;

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

float get_brightness(float level) {
    return level / (4.0 - 3.0 * level);
}

vec3 notGamma(vec3 color) {
    float maxComponent = max(max(color.x, color.y), color.z);

    // torchlit: black stays black instead of dividing by zero
    if (maxComponent <= 0.0) {
        return color;
    }
    float maxInverted = 1.0f - maxComponent;
    float maxScaled = 1.0f - maxInverted * maxInverted * maxInverted * maxInverted;
    return color * (maxScaled / maxComponent);
}

float parabolicMixFactor(float level) {
    return (2.0 * level - 1.0) * (2.0 * level - 1.0);
}

// torchlit: quartic, 0 unlit, 1 full
float coverage(float level) {
    float inverted = 1.0 - level;
    return 1.0 - inverted * inverted * inverted * inverted;
}

void main() {
    float block_level = floor(texCoord.x * 16) / 15;
    float sky_level = floor(texCoord.y * 16) / 15;

    float block_coverage = coverage(block_level);
    float sky_coverage = coverage(sky_level);

    float block_brightness = get_brightness(block_level) * lightmapInfo.BlockFactor
        * mix(1.0, block_coverage, lightmapInfo.DarkeningFactor);
    float sky_brightness = get_brightness(sky_level) * lightmapInfo.SkyFactor
        * mix(1.0, sky_coverage, lightmapInfo.DarkeningFactor);

    // torchlit: ambient color only as far as the cell's own light earns it
    float lit = max(block_coverage, sky_coverage * min(1.0, lightmapInfo.SkyFactor));
    vec3 ambientColor = lightmapInfo.AmbientColor * mix(1.0, lit, lightmapInfo.DarkeningFactor);

    vec3 nightVisionColor = lightmapInfo.NightVisionColor * lightmapInfo.NightVisionFactor;
    vec3 color = max(ambientColor, nightVisionColor);

    color += lightmapInfo.SkyLightColor * sky_brightness;

    vec3 BlockLightColor = mix(lightmapInfo.BlockLightTint, vec3(1.0), 0.9 * parabolicMixFactor(block_level));
    color += BlockLightColor * block_brightness;

    color = mix(color, color * vec3(0.7, 0.6, 0.6), lightmapInfo.BossOverlayWorldDarkeningFactor);

    color = color - vec3(lightmapInfo.DarknessScale);

    color = clamp(color, 0.0, 1.0);
    vec3 notGamma = notGamma(color);
    color = mix(color, notGamma, lightmapInfo.BrightnessFactor);

    // torchlit: eye adaptation, constants must match EyeAdaptation
    if (lightmapInfo.Exposure > 0.0) {
        float adapted = floor(lightmapInfo.Exposure) / 100.0;
        float glare = fract(lightmapInfo.Exposure);

        float cell = max(block_level, sky_level * min(1.0, lightmapInfo.SkyFactor)) * 15.0;
        color *= 1.0 - 0.72 * smoothstep(2.0, 11.0, adapted - cell);

        float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
        color *= 1.0 + glare * 1.5 * smoothstep(0.15, 0.8, luminance);
        color = clamp(color, 0.0, 1.0);
    }

    fragColor = vec4(color, 1.0);
}
