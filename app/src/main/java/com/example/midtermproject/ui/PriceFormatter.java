package com.example.midtermproject.ui;

/**
 * Định dạng số tiền kiểu Việt Nam: dấu chấm ngăn cách hàng nghìn, ví dụ 2200000 thành "2.200.000".
 * Chỉ trả về phần số; đơn vị "đ" nằm trong tài nguyên chuỗi (strings.xml) để giao diện không có chữ
 * cứng. Tự ngăn cách thay vì dùng {@code NumberFormat} để kết quả không phụ thuộc ngôn ngữ của máy.
 * Java thuần.
 */
public final class PriceFormatter {
    private PriceFormatter() {
    }

    public static String format(long amount) {
        // Không dùng Math.abs: với Long.MIN_VALUE nó vẫn trả về số âm.
        final String text = Long.toString(amount);
        final String digits = amount < 0 ? text.substring(1) : text;
        final StringBuilder builder = new StringBuilder(digits.length() + digits.length() / 3 + 1);
        if (amount < 0) {
            builder.append('-');
        }
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) {
                builder.append('.');
            }
            builder.append(digits.charAt(i));
        }
        return builder.toString();
    }
}
