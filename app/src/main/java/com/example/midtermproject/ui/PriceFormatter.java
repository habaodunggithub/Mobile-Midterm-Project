package com.example.midtermproject.ui;

/**
 * Định dạng số tiền kiểu Việt Nam: dấu chấm ngăn cách hàng nghìn, ví dụ 2200000 thành "2.200.000".
 * Chỉ trả về phần số; đơn vị "đ" nằm trong tài nguyên chuỗi (strings.xml) để giao diện không có chữ
 * cứng. Tự ngăn cách thay vì dùng {@code NumberFormat} để kết quả không phụ thuộc ngôn ngữ của máy.
 * Java thuần.
 */
public final class PriceFormatter {
    /** 0,1 triệu đồng, bước làm tròn của {@link #formatMillions}. */
    private static final long TENTH_OF_MILLION = 100_000L;

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

    /**
     * Số tiền tính theo triệu đồng, làm tròn đến 0,1 triệu, dấu phẩy thập phân kiểu Việt Nam:
     * 1900000 thành "1,9", 2500000 thành "2,5", đúng 2 triệu thì chỉ "2". Dùng cho nhãn thanh giá và
     * dòng "Đang lọc"; đơn vị "tr" nằm trong strings.xml. Giá vé không âm, nên số âm được coi là 0.
     */
    public static String formatMillions(long amount) {
        final long nonNegative = Math.max(amount, 0L);
        // Làm tròn lên khi phần dư từ nửa bước (50.000) trở lên; viết kiểu này để không tràn số ở Long.MAX_VALUE.
        final long remainder = nonNegative % TENTH_OF_MILLION;
        final long tenths = nonNegative / TENTH_OF_MILLION + (remainder >= TENTH_OF_MILLION / 2 ? 1 : 0);
        final long whole = tenths / 10;
        final long decimal = tenths % 10;
        return decimal == 0 ? Long.toString(whole) : whole + "," + decimal;
    }
}
