package com.example.midtermproject.map;

/**
 * Độ mờ của từng chỗ khi đổi bộ lọc (REQUIREMENTS Q49 "lọc mượt"). Java thuần để kiểm bằng JUnit.
 * <p>
 * Mỗi chỗ có một mức mờ từ 0 (rõ) đến 1 (mờ hẳn, chỗ ngoài bộ lọc). Mỗi khung hình, mức đó tiến về mục tiêu
 * (0 hoặc 1) thêm {@code dt / FADE_MS}, tức là đi hết từ rõ sang mờ trong {@link #FADE_MS} mili giây. Nhờ vậy khi bật
 * một bộ lọc, các chỗ bị lọc ra mờ dần thay vì tắt bụp, mắt kịp thấy chỗ nào vừa bị loại.
 * <p>
 * Không cấp phát gì trong khung hình: mảng mức mờ cấp một lần cho mỗi sơ đồ.
 */
public final class DimFader {
    /** Thời gian đi hết từ rõ sang mờ (hoặc ngược lại). */
    static final float FADE_MS = 200f;
    /** Khoảng thời gian lớn nhất tính cho một khung, để khung đầu sau một lúc đứng yên không nhảy cóc. */
    static final float MAX_FRAME_MS = 32f;

    private float[] levels = new float[0];
    private boolean ready;
    private boolean enabled = true;
    private float step;
    private boolean moving;

    /** Cấp mảng cho {@code count} chỗ; khung đầu tiên sau đó lấy ngay mức mục tiêu, không mờ dần. */
    public void reset(int count) {
        levels = new float[count];
        ready = false;
    }

    /** Tắt thì mọi chỗ nhận ngay mức mục tiêu (khi người dùng tắt hiệu ứng động của hệ thống). */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gọi một lần đầu mỗi khung hình.
     *
     * @param dtMs thời gian kể từ khung trước, mili giây
     */
    public void beginFrame(float dtMs) {
        step = Math.min(Math.max(dtMs, 0f), MAX_FRAME_MS) / FADE_MS;
        moving = false;
    }

    /**
     * Mức mờ của chỗ thứ {@code index} trong khung này, sau khi tiến về mục tiêu.
     *
     * @param dimmed chỗ có nằm ngoài bộ lọc không (mục tiêu là 1 nếu có, 0 nếu không)
     */
    public float level(int index, boolean dimmed) {
        final float target = dimmed ? 1f : 0f;
        float current = levels[index];
        if (!ready || !enabled) {
            current = target;
        } else if (current < target) {
            current = Math.min(target, current + step);
        } else if (current > target) {
            current = Math.max(target, current - step);
        }
        levels[index] = current;
        if (current != target) {
            moving = true;
        }
        return current;
    }

    /** Gọi cuối mỗi khung hình; từ khung sau, các mức mờ mới tiến dần. */
    public void endFrame() {
        ready = true;
    }

    /** Trong khung vừa vẽ còn chỗ nào chưa tới mức mục tiêu không (để View biết có cần vẽ tiếp). */
    public boolean isMoving() {
        return moving;
    }
}
