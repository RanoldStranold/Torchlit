package net.ranold.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.ranold.client.DarknessField.PointLight;

class DarknessFieldTest {

    private static final PointLight LEFT = new PointLight(0.0f, 0.0f, 14.0f, 56.0f);
    private static final PointLight RIGHT = new PointLight(72.0f, 0.0f, 14.0f, 56.0f);

    @Test
    void oneLightKeepsItsFalloff() {
        assertEquals(14.0f, DarknessField.lightAt(0.0f, 0.0f, 0.0f, List.of(LEFT)), 1.0e-4f);
        // smoothstep halfway out
        assertEquals(7.0f, DarknessField.lightAt(28.0f, 0.0f, 0.0f, List.of(LEFT)), 1.0e-4f);
        assertEquals(0.0f, DarknessField.lightAt(56.0f, 0.0f, 0.0f, List.of(LEFT)), 1.0e-4f);
    }

    @Test
    void ambientIsAFloor() {
        assertEquals(9.0f, DarknessField.lightAt(56.0f, 0.0f, 9.0f, List.of(LEFT)), 1.0e-4f);
        assertEquals(14.0f, DarknessField.lightAt(0.0f, 0.0f, 9.0f, List.of(LEFT)), 1.0e-4f);
    }

    // two glows meeting must not leave a crease: no kink midway (taking the brightest made a V there),
    // and between them it is at least what either torch gives alone
    @Test
    void twoLightsBlendWithoutACrease() {
        List<PointLight> both = List.of(LEFT, RIGHT);
        float step = 0.5f;
        float kink = DarknessField.lightAt(36.0f - step, 0.0f, 0.0f, both)
                - 2.0f * DarknessField.lightAt(36.0f, 0.0f, 0.0f, both)
                + DarknessField.lightAt(36.0f + step, 0.0f, 0.0f, both);
        assertTrue(Math.abs(kink) < 0.05f, "kink where the glows meet: " + kink);
        for (float x = 0.0f; x <= 72.0f; x += 1.0f) {
            float light = DarknessField.lightAt(x, 0.0f, 0.0f, both);
            float alone = Math.max(DarknessField.lightAt(x, 0.0f, 0.0f, List.of(LEFT)),
                    DarknessField.lightAt(x, 0.0f, 0.0f, List.of(RIGHT)));
            assertTrue(light >= alone - 1.0e-4f, "darker than one torch at x " + x);
        }
    }

    @Test
    void neverBrighterThanFull() {
        List<PointLight> stacked = List.of(LEFT, LEFT, LEFT, LEFT);
        assertTrue(DarknessField.lightAt(0.0f, 0.0f, 0.0f, stacked) <= 15.0f);
    }
}
