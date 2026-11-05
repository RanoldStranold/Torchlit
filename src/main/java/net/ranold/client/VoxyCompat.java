package net.ranold.client;

import net.minecraft.resources.Identifier;
import net.ranold.torchlit;

// voxy ssao paints (0,0,0,0) pixels red as missing data. our black lightmap hits it (vines under leaves)
public final class VoxyCompat {

    private static final String SSAO_SHADER = "post/ssao.comp";
    private static final String RED_MARKER = "ocolour = vec4(1.0f, 0.0f, 0.0f, 1.0f);";
    private static final String BLACK = "ocolour = vec4(0.0f, 0.0f, 0.0f, 1.0f);";

    private VoxyCompat() {
    }

    public static String patchShader(Identifier id, String source) {
        if (source == null || !id.getPath().equals(SSAO_SHADER)) {
            return source;
        }
        if (!source.contains(RED_MARKER)) {
            torchlit.LOGGER.warn("Voxy's {} no longer contains the missing-data marker Torchlit patches; "
                    + "unlit distant faces may render red", SSAO_SHADER);
            return source;
        }
        torchlit.LOGGER.info("Patched Voxy's {} so unlit faces render black instead of red", SSAO_SHADER);
        return source.replace(RED_MARKER, BLACK);
    }
}
