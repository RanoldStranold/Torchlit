package net.ranold.config;

import java.lang.ref.WeakReference;

// no cloth types here, safe to load without cloth config
public final class ConfigScreens {

    private static WeakReference<Object> current = new WeakReference<>(null);

    private ConfigScreens() {
    }

    public static void mark(Object screen) {
        current = new WeakReference<>(screen);
    }

    public static boolean isOurs(Object screen) {
        return screen != null && current.get() == screen;
    }
}
