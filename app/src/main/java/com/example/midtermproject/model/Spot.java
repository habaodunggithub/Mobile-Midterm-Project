package com.example.midtermproject.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Một chỗ bán được 1 vé: ghế ngồi (hình tròn) hoặc ô đứng (hình vuông).
 * Bất biến: mọi trường đều final. Hình học nằm trong tọa độ sơ đồ (world units).
 */
public final class Spot {
    /** Mã ổn định giữa các lần chạy, dạng "VIP-G-C05". */
    public final String id;
    public final String sectionId;
    public final ZoneType zone;
    public final SpotKind kind;
    public final String rowLabel;
    /** Số thứ tự trong hàng, bắt đầu từ 1 và tăng từ trái sang phải trên màn hình. */
    public final int number;
    /** Nhãn ngắn dùng để vẽ, dạng "C05". Tính sẵn một lần để onDraw không phải tạo chuỗi. */
    public final String label;
    /**
     * Khóa dùng để tìm kiếm: mã chỗ viết hoa và bỏ dấu gạch, ví dụ "VIPGC05". Tính sẵn một lần để
     * việc lọc không phải tạo chuỗi mới cho từng chỗ.
     */
    public final String searchKey;
    public final SpotStatus baseStatus;
    /** Giá vé tính bằng đồng. */
    public final long price;
    /** Tâm của chỗ. */
    public final float cx, cy;
    /** Ghế: bán kính. Ô đứng: nửa cạnh. */
    public final float size;

    public Spot(String sectionId, ZoneType zone, SpotKind kind, String rowLabel, int number,
                SpotStatus baseStatus, long price, float cx, float cy, float size) {
        if (number < 1) {
            throw new IllegalArgumentException("number must be >= 1: " + number);
        }
        if (size <= 0f) {
            throw new IllegalArgumentException("size must be > 0: " + size);
        }
        this.sectionId = Objects.requireNonNull(sectionId);
        this.zone = Objects.requireNonNull(zone);
        this.kind = Objects.requireNonNull(kind);
        this.rowLabel = Objects.requireNonNull(rowLabel);
        this.baseStatus = Objects.requireNonNull(baseStatus);
        this.number = number;
        this.price = price;
        this.cx = cx;
        this.cy = cy;
        this.size = size;
        this.label = String.format(Locale.ROOT, "%s%02d", rowLabel, number);
        this.id = sectionId + "-" + label;
        this.searchKey = id.replace("-", "").toUpperCase(Locale.ROOT);
    }

    /**
     * Điểm (wx, wy) trong tọa độ sơ đồ có chạm vào chỗ này không, tính cả dung sai {@code tolerance}
     * (đơn vị sơ đồ) quanh mép. Ghế là hình tròn nên so khoảng cách tới tâm; ô đứng là hình vuông
     * nên so độ lệch theo từng trục (AC-3.9).
     */
    public boolean hitTest(float wx, float wy, float tolerance) {
        final float dx = wx - cx;
        final float dy = wy - cy;
        final float reach = size + tolerance;
        if (kind == SpotKind.STANDING_CELL) {
            return Math.abs(dx) <= reach && Math.abs(dy) <= reach;
        }
        return dx * dx + dy * dy <= reach * reach;
    }

    /** Bình phương khoảng cách từ tâm chỗ tới điểm (wx, wy); dùng để chọn chỗ gần nhất. */
    public float distanceSqTo(float wx, float wy) {
        final float dx = wx - cx;
        final float dy = wy - cy;
        return dx * dx + dy * dy;
    }
}
