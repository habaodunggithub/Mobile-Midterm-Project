package com.example.midtermproject.model;

/**
 * Trạng thái dùng để vẽ một chỗ. Khác {@link SpotStatus} (dữ liệu gốc, không đổi), giá trị này được
 * suy ra từ trạng thái gốc cộng với lựa chọn và vé của người dùng, theo thứ tự ưu tiên:
 * MINE, BOOKED, LOCKED, SELECTED, AVAILABLE (REQUIREMENTS 6.3).
 */
public enum DisplayState {
    AVAILABLE,
    SELECTED,
    MINE,
    BOOKED,
    LOCKED
}
