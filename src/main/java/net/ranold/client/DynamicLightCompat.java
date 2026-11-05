package net.ranold.client;

import java.lang.reflect.Method;

import net.minecraft.core.BlockPos;
import net.ranold.torchlit;

// LambDynamicLights by reflection, nothing linked
public final class DynamicLightCompat {

    private static final String LDL_CLASS = "dev.lambdaurora.lambdynlights.LambDynLights";

    private static final Method GET_INSTANCE;
    private static final Method GET_LIGHT_LEVEL;
    private static final boolean AVAILABLE;

    static {
        Method getInstance = null;
        Method getLightLevel = null;

        try {
            Class<?> lambDynLights = Class.forName(LDL_CLASS);
            getInstance = lambDynLights.getMethod("get");
            getLightLevel = lambDynLights.getMethod("getDynamicLightLevel", BlockPos.class);
            torchlit.LOGGER.info("LambDynamicLights found; inventory darkness will follow dynamic light");
        } catch (ClassNotFoundException e) {
        } catch (Throwable t) {
            torchlit.LOGGER.warn("LambDynamicLights is present but its light query could not be resolved; "
                    + "inventory darkness will use world light only", t);
            getInstance = null;
            getLightLevel = null;
        }

        GET_INSTANCE = getInstance;
        GET_LIGHT_LEVEL = getLightLevel;
        AVAILABLE = getInstance != null && getLightLevel != null;
    }

    private DynamicLightCompat() {
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static float lightAt(BlockPos pos) {
        if (!AVAILABLE) {
            return 0.0f;
        }

        try {
            Object instance = GET_INSTANCE.invoke(null);
            if (instance == null) {
                return 0.0f;
            }
            return ((Number) GET_LIGHT_LEVEL.invoke(instance, pos)).floatValue();
        } catch (Throwable t) {
            return 0.0f;
        }
    }
}
