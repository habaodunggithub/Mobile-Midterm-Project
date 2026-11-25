package com.example.midtermproject.map;

/**
 * Góc nhìn của sơ đồ: tỉ lệ, góc xoay và độ dịch. Java thuần, không dùng android.*, nên test được
 * bằng JUnit trên máy tính.
 * <p>
 * Quy ước (REQUIREMENTS 6.4): trục y hướng xuống, góc dương là theo chiều kim đồng hồ trên màn hình.
 * <pre>
 *   screen = s * R(theta) * world + t        R(theta) = [[cos, -sin], [sin, cos]]
 *   world  = R(-theta) * (screen - t) / s
 * </pre>
 * Lớp này mới có phần "vừa khít" (fit), góc mặc định và đổi tọa độ; pan, zoom, xoay, kẹp biên
 * được thêm ở Chặng 3.
 */
public final class MapCamera {
    private static final float HALF_PI = (float) (Math.PI / 2.0);

    /** Lề giữa sơ đồ và mép vùng nhìn khi tính fit, tính bằng px. */
    private final float paddingPx;

    private float worldWidth;
    private float worldHeight;
    private float viewWidth;
    private float viewHeight;
    /** Phần dưới cùng của vùng nhìn bị bảng chi tiết (thu gọn) che, tính bằng px. */
    private float bottomInset;

    private float scale = 1f;
    private float angle;
    private float tx;
    private float ty;

    public MapCamera(float paddingPx) {
        this.paddingPx = paddingPx;
    }

    public void setWorldSize(float width, float height) {
        this.worldWidth = width;
        this.worldHeight = height;
    }

    public void setViewport(float viewWidth, float viewHeight, float bottomInset) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.bottomInset = bottomInset;
    }

    /** Chưa có kích thước vùng nhìn hoặc sơ đồ thì chưa tính được gì (UC-01, error course). */
    public boolean isReady() {
        return viewWidth > 0f && viewHeight - bottomInset > 0f && worldWidth > 0f && worldHeight > 0f;
    }

    /** Dọc (rộng nhỏ hơn cao): sân khấu bên trái, tức -90 độ. Ngang: sân khấu ở trên, tức 0. */
    public float defaultAngle() {
        return viewWidth < viewHeight ? -HALF_PI : 0f;
    }

    /**
     * Tỉ lệ để toàn bộ sơ đồ vừa vùng nhìn khi xoay một góc {@code angle}: khung bao của sơ đồ đã
     * xoay rộng {@code W|cos| + H|sin|} và cao {@code W|sin| + H|cos|}.
     */
    public float fitScale(float angle) {
        final float c = Math.abs((float) Math.cos(angle));
        final float s = Math.abs((float) Math.sin(angle));
        final float boxWidth = worldWidth * c + worldHeight * s;
        final float boxHeight = worldWidth * s + worldHeight * c;
        final float availableWidth = viewWidth - 2f * paddingPx;
        final float availableHeight = viewHeight - bottomInset - 2f * paddingPx;
        return Math.min(availableWidth / boxWidth, availableHeight / boxHeight);
    }

    /**
     * Góc nhìn mặc định: thấy trọn sơ đồ ở góc mặc định, tâm sơ đồ nằm giữa vùng nhìn
     * (phần phía trên bảng chi tiết). Trả về {scale, angle, tx, ty}.
     */
    public float[] fitTarget() {
        if (!isReady()) {
            return new float[]{1f, defaultAngle(), 0f, 0f};
        }
        final float targetAngle = defaultAngle();
        final float targetScale = fitScale(targetAngle);
        final float c = (float) Math.cos(targetAngle);
        final float s = (float) Math.sin(targetAngle);
        final float cx = worldWidth / 2f;
        final float cy = worldHeight / 2f;
        final float centerOnScreenX = targetScale * (cx * c - cy * s);
        final float centerOnScreenY = targetScale * (cx * s + cy * c);
        final float regionCenterX = viewWidth / 2f;
        final float regionCenterY = (viewHeight - bottomInset) / 2f;
        return new float[]{targetScale, targetAngle,
                regionCenterX - centerOnScreenX, regionCenterY - centerOnScreenY};
    }

    public void set(float scale, float angle, float tx, float ty) {
        this.scale = scale;
        this.angle = angle;
        this.tx = tx;
        this.ty = ty;
    }

    /** Đưa về góc nhìn mặc định. */
    public void resetToFit() {
        final float[] target = fitTarget();
        set(target[0], target[1], target[2], target[3]);
    }

    public float getScale() {
        return scale;
    }

    public float getAngle() {
        return angle;
    }

    public float getTx() {
        return tx;
    }

    public float getTy() {
        return ty;
    }

    public float worldToScreenX(float wx, float wy) {
        return scale * (wx * (float) Math.cos(angle) - wy * (float) Math.sin(angle)) + tx;
    }

    public float worldToScreenY(float wx, float wy) {
        return scale * (wx * (float) Math.sin(angle) + wy * (float) Math.cos(angle)) + ty;
    }

    public float screenToWorldX(float sx, float sy) {
        final float dx = (sx - tx) / scale;
        final float dy = (sy - ty) / scale;
        return dx * (float) Math.cos(angle) + dy * (float) Math.sin(angle);
    }

    public float screenToWorldY(float sx, float sy) {
        final float dx = (sx - tx) / scale;
        final float dy = (sy - ty) / scale;
        return -dx * (float) Math.sin(angle) + dy * (float) Math.cos(angle);
    }
}
