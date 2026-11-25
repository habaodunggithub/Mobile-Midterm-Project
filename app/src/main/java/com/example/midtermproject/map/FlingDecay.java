package com.example.midtermproject.map;

/**
 * Phép toán quán tính khi vuốt nhanh rồi nhấc tay (AC-2.14): vận tốc giảm theo hàm mũ
 * {@code v(t) = v0 * e^(-t / tau)}, nên quãng đường đã đi là
 * {@code d(t) = v0 * tau * (1 - e^(-t / tau))} và tiến dần tới {@code v0 * tau}.
 * Java thuần, test được trên máy tính.
 */
public final class FlingDecay {
    /** Hằng số thời gian suy giảm, tính bằng giây: càng nhỏ thì sơ đồ dừng càng nhanh. */
    public static final float TAU_SECONDS = 0.25f;
    /** Dưới vận tốc này (px/giây) thì coi như đã dừng. */
    public static final float MIN_SPEED_PX_PER_SEC = 50f;

    private FlingDecay() {
    }

    /**
     * Thời gian (mili giây) để vận tốc ban đầu {@code speed} (px/giây, không âm) giảm xuống còn
     * {@link #MIN_SPEED_PX_PER_SEC}; bằng 0 nếu vận tốc đã nhỏ hơn ngưỡng.
     */
    public static long durationMillis(float speed) {
        if (speed <= MIN_SPEED_PX_PER_SEC) {
            return 0L;
        }
        return Math.round(1000.0 * TAU_SECONDS * Math.log(speed / MIN_SPEED_PX_PER_SEC));
    }

    /**
     * Quãng đường (px, cùng dấu với {@code velocity}) đã trượt sau {@code elapsedMillis} kể từ lúc
     * nhấc tay với vận tốc ban đầu {@code velocity} (px/giây, có dấu).
     */
    public static float distanceAt(float velocity, long elapsedMillis) {
        final double t = elapsedMillis / 1000.0;
        return (float) (velocity * TAU_SECONDS * (1.0 - Math.exp(-t / TAU_SECONDS)));
    }
}
