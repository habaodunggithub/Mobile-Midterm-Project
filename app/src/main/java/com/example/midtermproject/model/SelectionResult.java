package com.example.midtermproject.model;

/**
 * Kết quả của một lần chạm vào chỗ (REQUIREMENTS UC-06): được chọn, được bỏ chọn, hoặc bị từ chối
 * kèm lý do để giao diện hiện thông báo tương ứng (M1 đến M5).
 */
public enum SelectionResult {
    SELECTED,
    DESELECTED,
    /** Đây là vé của chính người dùng (M3). */
    REJECTED_MINE,
    /** Đã có người khác đặt (M1). */
    REJECTED_BOOKED,
    /** Đang bị khóa (M2). */
    REJECTED_LOCKED,
    /** Nằm ngoài bộ lọc hiện tại (M4). */
    REJECTED_FILTERED,
    /** Đã đủ số chỗ tối đa mỗi lần đặt (M5). */
    REJECTED_LIMIT
}
