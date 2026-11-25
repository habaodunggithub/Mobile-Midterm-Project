package com.example.midtermproject.model;

/**
 * Trạng thái gốc của chỗ, do dữ liệu cấp và không bao giờ đổi.
 * "Đang chọn" và "Vé của bạn" không nằm ở đây vì chúng thuộc về phiên làm việc của người dùng.
 */
public enum SpotStatus {
    AVAILABLE,
    BOOKED,
    LOCKED
}
