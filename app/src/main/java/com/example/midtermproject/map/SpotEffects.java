package com.example.midtermproject.map;

import com.example.midtermproject.model.Spot;

import java.util.ArrayList;
import java.util.List;

/**
 * Mốc thời gian và đường cong của các hiệu ứng trên từng chỗ (REQUIREMENTS Q43). Java thuần để kiểm bằng JUnit.
 * <p>
 * Lớp này không vẽ gì: {@code VenueMapView} hỏi nó ở mỗi khung hình "chỗ này đang phóng bao nhiêu, rung lệch bao
 * nhiêu, vòng lan đi tới đâu", rồi {@code SpotRenderer} vẽ theo. Thời gian truyền vào là mili giây
 * ({@code SystemClock.uptimeMillis()} trên máy, số tùy ý trong test).
 * <ul>
 *   <li><b>Mở màn</b>: chỗ hiện ra lần lượt từ sân khấu ra ngoài; chỗ xa tâm sân khấu hơn thì bắt đầu muộn hơn.</li>
 *   <li><b>Nảy</b> (khi chọn hoặc bỏ chọn): phóng 1 → 1,5 → 0,9 → 1, kèm một vòng lan rộng rồi tan.</li>
 *   <li><b>Lật</b> (khi đặt vé xong): từng chỗ vừa thành "vé của bạn" thu nhỏ rồi bung ra, lần lượt theo thứ tự
 *   gần sân khấu trước.</li>
 *   <li><b>Rung</b> (khi chỗ không chọn được): lắc ngang nhỏ dần.</li>
 * </ul>
 * Các hàm hỏi trong khung hình không cấp phát gì; chỉ lúc bắt đầu một hiệu ứng mới tạo đối tượng.
 */
public final class SpotEffects {
    /** Mở màn: mỗi đơn vị sơ đồ cách tâm sân khấu làm chỗ hiện muộn thêm ngần này mili giây. */
    static final float INTRO_DELAY_PER_UNIT_MS = 1.6f;
    static final long INTRO_DURATION_MS = 420L;
    static final long POP_DURATION_MS = 450L;
    static final long RING_DURATION_MS = 520L;
    static final long FLIP_DURATION_MS = 520L;
    /** Lật: chỗ thứ k (theo khoảng cách tới sân khấu) bắt đầu muộn hơn chỗ đầu k lần ngần này. */
    static final long FLIP_STAGGER_MS = 80L;
    static final long SHAKE_DURATION_MS = 400L;
    /** Thời gian vòng chỗ đang xem thu về ôm sát chỗ. */
    static final long FOCUS_SETTLE_MS = 180L;
    /** Số lần lắc qua lại trong một lần rung. */
    static final int SHAKE_CYCLES = 3;
    /** Độ vọt quá của đường cong "bật ra" khi chỗ hiện lên (back easing). */
    private static final float OVERSHOOT = 1.7f;

    private static final int POP = 0;
    private static final int FLIP = 1;
    private static final int SHAKE = 2;

    /** Một hiệu ứng đang chạy trên một chỗ. */
    private static final class Entry {
        final String spotId;
        final int type;
        final long start;

        Entry(String spotId, int type, long start) {
            this.spotId = spotId;
            this.type = type;
            this.start = start;
        }

        long duration() {
            switch (type) {
                case POP:
                    return Math.max(POP_DURATION_MS, RING_DURATION_MS);
                case FLIP:
                    return Math.max(FLIP_DURATION_MS, RING_DURATION_MS);
                default:
                    return SHAKE_DURATION_MS;
            }
        }
    }

    private final float originX;
    private final float originY;
    private final List<Entry> entries = new ArrayList<>();
    /** Thời điểm bắt đầu mở màn; {@code Long.MIN_VALUE} là không có mở màn. */
    private long introStart = Long.MIN_VALUE;
    /** Thời điểm chỗ xa nhất hiện xong, để biết lúc nào mở màn kết thúc. */
    private long introEnd = Long.MIN_VALUE;
    private boolean enabled = true;
    /** Chỗ đang xem vừa được chạm, và lúc vòng đánh dấu của nó bắt đầu thu về (Q49). */
    private String focusId;
    private long focusStart = Long.MIN_VALUE;

    /** @param originX, originY tâm sân khấu theo tọa độ sơ đồ: nơi các làn sóng bắt đầu */
    public SpotEffects(float originX, float originY) {
        this.originX = originX;
        this.originY = originY;
    }

    /** Tắt thì mọi hiệu ứng đang chạy bị xóa và mọi câu hỏi trả về giá trị "đứng yên". */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clear();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void clear() {
        entries.clear();
        introStart = Long.MIN_VALUE;
        introEnd = Long.MIN_VALUE;
        focusId = null;
        focusStart = Long.MIN_VALUE;
    }

    /** Chỗ vừa thành chỗ đang xem: vòng đánh dấu của nó xuất hiện to rồi thu về ôm sát (Q49). */
    public void settleFocus(Spot spot, long now) {
        if (!enabled) {
            return;
        }
        focusId = spot.id;
        focusStart = now;
    }

    /**
     * Độ "chưa thu về" của vòng chỗ đang xem: 1 lúc vừa chạm (vòng to nhất), giảm về 0 khi đã ôm sát chỗ. Đi theo
     * (1 − p)² với p là tiến độ từ 0 đến 1, tức là thu nhanh lúc đầu rồi chậm dần, như ống kính vừa lấy nét xong.
     * Chỗ khác hoặc đã xong thì là 0.
     */
    public float focusSettleOf(Spot spot, long now) {
        if (focusId == null || !focusId.equals(spot.id)) {
            return 0f;
        }
        return focusSettle(now - focusStart);
    }

    /** Bắt đầu mở màn; {@code farthest} là khoảng cách lớn nhất từ sân khấu tới một chỗ, để biết lúc kết thúc. */
    public void startIntro(long now, float farthest) {
        if (!enabled) {
            return;
        }
        introStart = now;
        introEnd = now + (long) (farthest * INTRO_DELAY_PER_UNIT_MS) + INTRO_DURATION_MS;
    }

    public void pop(Spot spot, long now) {
        start(spot, POP, now);
    }

    public void shake(Spot spot, long now) {
        start(spot, SHAKE, now);
    }

    /** Lật các chỗ vừa đặt, chỗ gần sân khấu lật trước, mỗi chỗ sau trễ {@link #FLIP_STAGGER_MS}. */
    public void flipInWave(List<Spot> spots, long now) {
        if (!enabled) {
            return;
        }
        final List<Spot> ordered = new ArrayList<>(spots);
        ordered.sort((a, b) -> Float.compare(distance(a), distance(b)));
        for (int k = 0; k < ordered.size(); k++) {
            start(ordered.get(k), FLIP, now + k * FLIP_STAGGER_MS);
        }
    }

    private void start(Spot spot, int type, long start) {
        if (!enabled) {
            return;
        }
        // Mỗi chỗ chỉ giữ một hiệu ứng: hiệu ứng mới thay hiệu ứng cũ (bấm liên tục không bị chồng).
        removeEntryOf(spot.id);
        entries.add(new Entry(spot.id, type, start));
    }

    private void removeEntryOf(String spotId) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).spotId.equals(spotId)) {
                entries.remove(i);
            }
        }
    }

    /**
     * Còn hiệu ứng nào đang chạy không (để View biết có cần vẽ khung tiếp theo). Đồng thời bỏ các hiệu ứng
     * đã xong.
     */
    public boolean isRunning(long now) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            final Entry e = entries.get(i);
            if (now >= e.start + e.duration()) {
                entries.remove(i);
            }
        }
        // Kiểm focusStart trước khi trừ: với Long.MIN_VALUE, phép trừ bị tràn số thành số âm.
        final boolean settling = focusStart != Long.MIN_VALUE && now - focusStart < FOCUS_SETTLE_MS;
        return !entries.isEmpty() || now < introEnd || settling;
    }

    /** Hệ số phóng của chỗ ở thời điểm {@code now}: 1 là bình thường, 0 là chưa hiện. */
    public float scaleOf(Spot spot, long now) {
        float scale = 1f;
        if (introStart != Long.MIN_VALUE && now < introEnd) {
            final long delay = (long) (distance(spot) * INTRO_DELAY_PER_UNIT_MS);
            scale = introScale(now - introStart - delay);
        }
        final Entry e = entryOf(spot.id);
        if (e != null) {
            if (e.type == POP) {
                scale *= popScale(now - e.start);
            } else if (e.type == FLIP) {
                scale *= flipScale(now - e.start);
            }
        }
        return scale;
    }

    /** Độ lệch ngang khi rung, từ −1 đến 1 (nhân với biên độ khi vẽ). */
    public float shakeOf(Spot spot, long now) {
        final Entry e = entryOf(spot.id);
        return e != null && e.type == SHAKE ? shakeOffset(now - e.start) : 0f;
    }

    /** Tiến độ của vòng lan quanh chỗ, từ 0 đến 1; −1 nếu không có vòng nào. */
    public float ringOf(Spot spot, long now) {
        final Entry e = entryOf(spot.id);
        if (e == null || e.type == SHAKE) {
            return -1f;
        }
        return ringProgress(now - e.start);
    }

    private Entry entryOf(String spotId) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).spotId.equals(spotId)) {
                return entries.get(i);
            }
        }
        return null;
    }

    private float distance(Spot spot) {
        return (float) Math.hypot(spot.cx - originX, spot.cy - originY);
    }

    // ----- Các đường cong, theo thời gian đã trôi kể từ lúc hiệu ứng bắt đầu (mili giây). -----

    /**
     * Mở màn: "bật ra" từ 0 lên 1, vọt quá một chút rồi về 1 (back easing):
     * f(p) = 1 + (c+1)(p−1)³ + c(p−1)², với c = {@link #OVERSHOOT}.
     */
    static float introScale(long elapsed) {
        if (elapsed <= 0) {
            return 0f;
        }
        if (elapsed >= INTRO_DURATION_MS) {
            return 1f;
        }
        final float q = (float) elapsed / INTRO_DURATION_MS - 1f;
        return 1f + (OVERSHOOT + 1f) * q * q * q + OVERSHOOT * q * q;
    }

    /** Nảy: đi qua các mốc 1 → 1,5 (35%) → 0,9 (70%) → 1, nối mượt bằng smoothstep. */
    static float popScale(long elapsed) {
        return keyframes(elapsed, POP_DURATION_MS, 1.5f, 0.9f);
    }

    /** Lật: thu nhỏ còn 0,2 rồi bung quá 1,25 trước khi về 1. */
    static float flipScale(long elapsed) {
        if (elapsed < 0) {
            return 1f;   // chưa tới lượt chỗ này
        }
        return keyframes(elapsed, FLIP_DURATION_MS, 0.2f, 1.25f);
    }

    private static float keyframes(long elapsed, long duration, float first, float second) {
        if (elapsed <= 0 || elapsed >= duration) {
            return 1f;
        }
        final float p = (float) elapsed / duration;
        if (p < 0.35f) {
            return lerp(1f, first, smooth(p / 0.35f));
        }
        if (p < 0.7f) {
            return lerp(first, second, smooth((p - 0.35f) / 0.35f));
        }
        return lerp(second, 1f, smooth((p - 0.7f) / 0.3f));
    }

    /** Vòng lan: tiến độ 0 → 1 chậm dần (ease-out bậc hai); −1 khi chưa bắt đầu hoặc đã xong. */
    static float ringProgress(long elapsed) {
        if (elapsed < 0 || elapsed >= RING_DURATION_MS) {
            return -1f;
        }
        final float p = (float) elapsed / RING_DURATION_MS;
        return 1f - (1f - p) * (1f - p);
    }

    static float focusSettle(long elapsed) {
        if (elapsed < 0 || elapsed >= FOCUS_SETTLE_MS) {
            return 0f;
        }
        final float remaining = 1f - (float) elapsed / FOCUS_SETTLE_MS;
        return remaining * remaining;
    }

    /** Rung: sin của {@link #SHAKE_CYCLES} vòng, biên độ giảm tuyến tính về 0. */
    static float shakeOffset(long elapsed) {
        if (elapsed <= 0 || elapsed >= SHAKE_DURATION_MS) {
            return 0f;
        }
        final float p = (float) elapsed / SHAKE_DURATION_MS;
        return (float) Math.sin(p * SHAKE_CYCLES * 2 * Math.PI) * (1f - p);
    }

    /** smoothstep: 3t² − 2t³, bắt đầu và kết thúc êm. */
    private static float smooth(float t) {
        return t * t * (3f - 2f * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
