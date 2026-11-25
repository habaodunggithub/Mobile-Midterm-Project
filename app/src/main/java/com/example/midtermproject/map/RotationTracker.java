package com.example.midtermproject.map;

/**
 * Theo dõi góc quay của đường nối hai ngón tay để xoay sơ đồ. Android có {@code ScaleGestureDetector}
 * cho zoom nhưng không có bộ nhận biết cử chỉ xoay, nên lớp này tự tính. Java thuần, test được
 * trên máy tính.
 * <p>
 * Góc đo bằng {@code atan2(dy, dx)} trên tọa độ màn hình (trục y hướng xuống) nên dương là theo chiều
 * kim đồng hồ, cùng quy ước với {@link MapCamera}.
 * <p>
 * Có một ngưỡng (REQUIREMENTS Q32): khi người dùng chụm để zoom, hai ngón thường lệch vài độ mà không
 * chủ ý, nên sơ đồ chỉ bắt đầu xoay khi đường nối đã quay quá ngưỡng kể từ lúc chạm. Từ đó xoay theo
 * ngón liên tục; phần vượt ngưỡng được tính nên sơ đồ không bị giật nhảy ngay lúc bắt đầu.
 */
public final class RotationTracker {
    private final float thresholdRad;

    private float previousAngle;
    private float accumulated;
    private boolean engaged;

    public RotationTracker(float thresholdRad) {
        this.thresholdRad = thresholdRad;
    }

    /** Gọi khi ngón thứ hai chạm xuống. */
    public void begin(float x0, float y0, float x1, float y1) {
        previousAngle = lineAngle(x0, y0, x1, y1);
        accumulated = 0f;
        engaged = false;
    }

    /**
     * Gọi mỗi lần hai ngón di chuyển.
     *
     * @return độ lệch góc (radian, trong (-pi, pi]) cần xoay sơ đồ thêm so với lần gọi trước;
     * 0 nếu chưa vượt ngưỡng
     */
    public float update(float x0, float y0, float x1, float y1) {
        final float angle = lineAngle(x0, y0, x1, y1);
        // Chuẩn hóa để hai ngón đi qua trục +-180 độ không bị coi là quay gần một vòng.
        final float delta = MapCamera.normalizeAngle(angle - previousAngle);
        previousAngle = angle;
        if (engaged) {
            return delta;
        }
        accumulated += delta;
        if (Math.abs(accumulated) <= thresholdRad) {
            return 0f;
        }
        engaged = true;
        return accumulated - Math.signum(accumulated) * thresholdRad;
    }

    /** Đã vượt ngưỡng và đang xoay sơ đồ theo ngón. */
    public boolean isEngaged() {
        return engaged;
    }

    private static float lineAngle(float x0, float y0, float x1, float y1) {
        return (float) Math.atan2(y1 - y0, x1 - x0);
    }
}
