package com.example.midtermproject.model;

import java.util.Objects;

/** Một phân khu có tên, ví dụ "VIP Giữa". Tên được vẽ ở vị trí (labelX, labelY) trên sơ đồ. */
public final class Section {
    /** Mã ngắn dùng làm tiền tố mã chỗ, ví dụ "VIP-G". */
    public final String id;
    public final String name;
    public final ZoneType zone;
    /** Vị trí đặt tên phân khu, trong tọa độ sơ đồ (nằm ở lối đi cạnh phân khu). */
    public final float labelX, labelY;

    public Section(String id, String name, ZoneType zone, float labelX, float labelY) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.zone = Objects.requireNonNull(zone);
        this.labelX = labelX;
        this.labelY = labelY;
    }
}
