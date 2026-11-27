package com.example.midtermproject.decor;

/**
 * Chuyển động của từng chữ trong tên đêm nhạc "GIEO" (REQUIREMENTS Q49). Java thuần để kiểm bằng JUnit.
 * <p>
 * Hai pha cho mỗi chữ thứ i:
 * <ol>
 *   <li><b>Rơi</b> khi mở app: chữ bắt đầu muộn hơn chữ trước {@link #DROP_STAGGER_MS}, rơi từ cao
 *   {@link #DROP_HEIGHT_DP} xuống, nghiêng {@link #DROP_TILT_DEG} rồi thẳng lại, vọt quá một chút (back easing)
 *   giống một miếng dán vừa được ấn xuống.</li>
 *   <li><b>Lắc</b> sau khi rơi xong: nghiêng qua lại rất chậm, biên độ tăng dần từ 0 trong {@link #SWAY_RAMP_MS}
 *   nên không có cú giật lúc chuyển pha. Mỗi chữ lệch pha một chút để cả chữ trông như đang "thở".</li>
 * </ol>
 * Hàm không cấp phát gì: kết quả ghi vào mảng có sẵn.
 */
public final class TitleMotion {
    static final long DROP_MS = 600L;
    static final long DROP_STAGGER_MS = 90L;
    static final float DROP_HEIGHT_DP = 40f;
    static final float DROP_TILT_DEG = -12f;
    /** Một chu kỳ lắc qua rồi lắc lại. */
    static final long SWAY_PERIOD_MS = 9600L;
    static final float SWAY_DEG = 3f;
    static final float SWAY_LIFT_DP = 1f;
    static final long SWAY_RAMP_MS = 1200L;
    /** Lệch pha giữa hai chữ liền nhau, radian. */
    static final double SWAY_PHASE_STEP = 0.9;
    /** Độ vọt quá của đường cong rơi (back easing). */
    private static final float OVERSHOOT = 1.7f;

    /** Chỉ số trong mảng kết quả. */
    public static final int DY = 0;
    public static final int ROTATION = 1;
    public static final int ALPHA = 2;

    private TitleMotion() {
    }

    /** Thời điểm chữ cuối cùng rơi xong, tính từ lúc bắt đầu. */
    public static long dropEnd(int letters) {
        return (letters - 1) * DROP_STAGGER_MS + DROP_MS;
    }

    /**
     * Trạng thái chữ thứ {@code index} sau {@code elapsedMs} kể từ lúc bắt đầu.
     *
     * @param out {@code out[DY]} độ dịch dọc (dp, âm là cao hơn vị trí gốc), {@code out[ROTATION]} góc nghiêng (độ,
     *            dương là theo chiều kim đồng hồ), {@code out[ALPHA]} độ rõ từ 0 đến 1
     */
    public static void letterAt(int index, long elapsedMs, float[] out) {
        final long local = elapsedMs - index * DROP_STAGGER_MS;
        if (local <= 0) {
            out[DY] = -DROP_HEIGHT_DP;
            out[ROTATION] = DROP_TILT_DEG;
            out[ALPHA] = 0f;
            return;
        }
        if (local < DROP_MS) {
            final float p = (float) local / DROP_MS;
            // Back easing: e(p) = 1 + (c+1)(p−1)³ + c(p−1)², đi từ 0 lên hơn 1 một chút rồi về 1.
            final float q = p - 1f;
            final float e = 1f + (OVERSHOOT + 1f) * q * q * q + OVERSHOOT * q * q;
            out[DY] = -DROP_HEIGHT_DP * (1f - e);
            out[ROTATION] = DROP_TILT_DEG * (1f - e);
            out[ALPHA] = Math.min(1f, p * 3f);
            return;
        }
        // Lắc: góc = A·r(t)·sin(ωt + φ), với r(t) tăng tuyến tính từ 0 lên 1 trong SWAY_RAMP_MS.
        final long swayTime = local - DROP_MS;
        final float ramp = Math.min(1f, (float) swayTime / SWAY_RAMP_MS);
        final double angle = 2 * Math.PI * (swayTime % SWAY_PERIOD_MS) / SWAY_PERIOD_MS;
        final double phase = index * SWAY_PHASE_STEP;
        // Trừ sin(φ) để tại lúc bắt đầu lắc góc đúng bằng 0, nối liền với cuối pha rơi.
        final float wave = (float) (Math.sin(angle + phase) - Math.sin(phase));
        out[ROTATION] = SWAY_DEG * ramp * wave / 2f;
        out[DY] = -SWAY_LIFT_DP * ramp * wave / 2f;
        out[ALPHA] = 1f;
    }
}
