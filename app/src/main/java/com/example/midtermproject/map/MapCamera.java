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
 * Lớp này lo phần "vừa khít" (fit), góc mặc định, đổi tọa độ, pan có giới hạn biên, zoom và xoay;
 * tự chỉnh về góc vuông và kiểm tra góc mặc định; snapshot được thêm ở commit sau của Chặng 3.
 */
public final class MapCamera {
    private static final float HALF_PI = (float) (Math.PI / 2.0);
    /** Góc lệch không quá ngần này so với góc mặc định (0,5 độ) thì coi như đang ở góc mặc định. */
    private static final float DEFAULT_ANGLE_TOLERANCE = (float) Math.toRadians(0.5);
    /** Phóng to tối đa bằng ngần này lần mức "vừa khít" ở góc mặc định (REQUIREMENTS Q15). */
    private static final float MAX_ZOOM_FACTOR = 6f;

    /** Lề giữa sơ đồ và mép vùng nhìn khi tính fit, tính bằng px. */
    private final float paddingPx;
    /** Khi kéo, mép khung bao của sơ đồ được lùi vào trong vùng nhìn tối đa bằng khoảng này (48dp). */
    private final float panMarginPx;

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

    /** Lề kéo mặc định bằng 3 lần lề fit (48dp khi lề fit là 16dp). */
    public MapCamera(float paddingPx) {
        this(paddingPx, 3f * paddingPx);
    }

    public MapCamera(float paddingPx, float panMarginPx) {
        this.paddingPx = paddingPx;
        this.panMarginPx = panMarginPx;
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

    /** Nhỏ nhất: vừa khít toàn sơ đồ ở góc xoay hiện tại, nên không thu nhỏ hơn mức thấy trọn sơ đồ. */
    public float getMinScale() {
        return fitScale(angle);
    }

    /** Lớn nhất: 6 lần mức vừa khít ở góc mặc định, nên một chỗ to khoảng 80dp khi dọc. */
    public float getMaxScale() {
        return MAX_ZOOM_FACTOR * fitScale(defaultAngle());
    }

    /**
     * Phóng/thu theo hệ số {@code factor} quanh điểm (fx, fy) tính bằng px màn hình, thường là tâm
     * hai ngón. Điểm sơ đồ nằm dưới (fx, fy) đứng yên: từ {@code f = s*R*w + t} cho cả hai tỉ lệ
     * ta có {@code t' = f - (s'/s) * (f - t)} (AC-2.3). Tỉ lệ mới bị kẹp trong [min, max] (AC-2.4).
     */
    public void zoomBy(float factor, float fx, float fy) {
        if (!isReady()) {
            return;
        }
        final float newScale = Math.max(getMinScale(), Math.min(getMaxScale(), scale * factor));
        final float ratio = newScale / scale;
        tx = fx - ratio * (fx - tx);
        ty = fy - ratio * (fy - ty);
        scale = newScale;
        clamp();
    }

    /**
     * Xoay sơ đồ thêm {@code delta} radian (dương là theo chiều kim đồng hồ) quanh điểm (fx, fy) tính
     * bằng px màn hình, thường là tâm hai ngón. Điểm sơ đồ dưới (fx, fy) đứng yên: từ
     * {@code f = s*R(theta)*w + t} ta có {@code t' = f - R(delta) * (f - t)} (AC-2.8).
     * <p>
     * Xoay làm khung bao đổi kích thước nên mức "vừa khít" cũng đổi; nếu tỉ lệ hiện tại nhỏ hơn mức
     * nhỏ nhất mới thì phóng lên quanh cùng điểm đó.
     */
    public void rotateBy(float delta, float fx, float fy) {
        if (!isReady()) {
            return;
        }
        final float c = (float) Math.cos(delta);
        final float s = (float) Math.sin(delta);
        final float dx = tx - fx;
        final float dy = ty - fy;
        tx = fx + c * dx - s * dy;
        ty = fy + s * dx + c * dy;
        angle = normalizeAngle(angle + delta);

        final float minScale = getMinScale();
        if (scale < minScale) {
            final float ratio = minScale / scale;
            tx = fx - ratio * (fx - tx);
            ty = fy - ratio * (fy - ty);
            scale = minScale;
        }
        clamp();
    }

    /**
     * Góc vuông (0, 90, 180, 270 độ) gần góc hiện tại nhất, nếu góc hiện tại lệch không quá
     * {@code toleranceRad}; ngược lại trả về NaN (AC-2.10). Kết quả nằm trong (-pi, pi].
     */
    public float nearestRightAngle(float toleranceRad) {
        final float current = normalizeAngle(angle);
        final float target = Math.round(current / HALF_PI) * HALF_PI;
        if (Math.abs(current - target) <= toleranceRad) {
            return normalizeAngle(target);
        }
        return Float.NaN;
    }

    /** Sơ đồ đang ở góc mặc định (sai số 0,5 độ) thì ẩn nút la bàn (AC-2.11). */
    public boolean isAtDefaultAngle() {
        return Math.abs(normalizeAngle(angle - defaultAngle())) <= DEFAULT_ANGLE_TOLERANCE;
    }

    /** Góc phải quay (theo chiều ngắn nhất, trong (-pi, pi]) để đi từ {@code from} tới {@code to}. */
    public static float shortestAngleDelta(float from, float to) {
        return normalizeAngle(to - from);
    }

    /** Đưa góc về khoảng (-pi, pi] để hai góc cách nhau 360 độ được coi là một. */
    public static float normalizeAngle(float angle) {
        final float pi = (float) Math.PI;
        final float twoPi = 2f * pi;
        float result = angle % twoPi;
        // So sánh bằng float: góc (float) pi là biên và phải được giữ nguyên là +pi.
        if (result > pi) {
            result -= twoPi;
        } else if (result <= -pi) {
            result += twoPi;
        }
        return result;
    }

    /**
     * Dịch sơ đồ theo ngón tay một đoạn (dx, dy) tính bằng px màn hình, rồi kẹp lại trong giới hạn.
     * Độ dịch nằm trong tọa độ màn hình nên không phụ thuộc góc xoay.
     */
    public void panBy(float dx, float dy) {
        if (!isReady()) {
            return;
        }
        tx += dx;
        ty += dy;
        clamp();
    }

    /**
     * Kẹp độ dịch theo khung bao của sơ đồ đã phóng và xoay (hình chữ nhật nhỏ nhất theo trục màn
     * hình chứa nó). Với mỗi trục: khung bao nhỏ hơn vùng nhìn thì căn giữa; ngược lại mép khung bao
     * chỉ được lùi vào trong mép vùng nhìn tối đa {@code panMarginPx}, nên sơ đồ không bao giờ bị
     * kéo hoàn toàn ra khỏi màn hình (AC-2.2).
     */
    private void clamp() {
        final float c = (float) Math.cos(angle);
        final float s = (float) Math.sin(angle);
        // Ba góc còn lại của sơ đồ sau khi phóng và xoay (góc (0, 0) nằm ở gốc), chưa dịch.
        final float x1 = scale * worldWidth * c;
        final float y1 = scale * worldWidth * s;
        final float x2 = -scale * worldHeight * s;
        final float y2 = scale * worldHeight * c;
        final float x3 = x1 + x2;
        final float y3 = y1 + y2;
        final float minX = Math.min(Math.min(0f, x1), Math.min(x2, x3));
        final float maxX = Math.max(Math.max(0f, x1), Math.max(x2, x3));
        final float minY = Math.min(Math.min(0f, y1), Math.min(y2, y3));
        final float maxY = Math.max(Math.max(0f, y1), Math.max(y2, y3));
        tx = clampAxis(tx, minX, maxX, viewWidth);
        ty = clampAxis(ty, minY, maxY, viewHeight - bottomInset);
    }

    private float clampAxis(float translation, float min, float max, float viewSize) {
        final float size = max - min;
        if (size <= viewSize) {
            return (viewSize - size) / 2f - min;
        }
        // Mép trái của khung bao là translation + min, phải nằm trong [viewSize - size - margin, margin].
        final float lowest = viewSize - size - panMarginPx - min;
        final float highest = panMarginPx - min;
        return Math.max(lowest, Math.min(highest, translation));
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
