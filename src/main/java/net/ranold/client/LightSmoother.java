package net.ranold.client;

public final class LightSmoother {

    private static final float MAX_FRAME_SECONDS = 0.25f;

    private static final float CAUGHT_UP = 0.25f;

    // a jump this big (torch placed or broken) follows over jumpSeconds instead
    private final float jumpThreshold;
    private final float jumpSeconds;

    private float current = Float.NaN;
    private long lastNanos;
    private boolean jumping;

    public LightSmoother() {
        this(Float.POSITIVE_INFINITY, 0.0f);
    }

    public LightSmoother(float jumpThreshold, float jumpSeconds) {
        this.jumpThreshold = jumpThreshold;
        this.jumpSeconds = jumpSeconds;
    }

    public float update(float target, float smoothingSeconds) {
        long now = System.nanoTime();

        if (Float.isNaN(this.current) || this.lastNanos == 0L || smoothingSeconds <= 0.0f) {
            this.current = target;
            this.lastNanos = now;
            this.jumping = false;
            return this.current;
        }

        float dtSeconds = (now - this.lastNanos) / 1_000_000_000.0f;
        this.lastNanos = now;

        if (dtSeconds >= MAX_FRAME_SECONDS) {
            this.current = target;
            this.jumping = false;
            return this.current;
        }

        float gap = Math.abs(target - this.current);
        if (gap >= this.jumpThreshold) {
            this.jumping = true;
        } else if (gap <= CAUGHT_UP) {
            this.jumping = false;
        }

        float seconds = this.jumping ? this.jumpSeconds : smoothingSeconds;
        if (seconds <= 0.0f) {
            this.current = target;
        } else {
            this.current += (target - this.current) * (1.0f - (float) Math.exp(-dtSeconds / seconds));
        }
        return this.current;
    }

    public void reset() {
        this.current = Float.NaN;
        this.lastNanos = 0L;
        this.jumping = false;
    }
}
