package com.example.midtermproject.decor;

import com.example.midtermproject.model.Section;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.Venue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.LongFunction;

/**
 * Nội dung dòng chữ chạy quanh viền sticker của sơ đồ, giống dòng credit chạy quanh mép bìa đĩa
 * trong ảnh tham chiếu (REQUIREMENTS Q42). Lấy từ dữ liệu thật: "SÂN KHẤU", rồi từng phân khu kèm giá
 * thấp nhất của khu đó, ví dụ {@code "SÂN KHẤU • STANDING A TỪ 900.000 Đ • … • "}.
 * <p>
 * Chuỗi được dựng một lần khi gắn sơ đồ hoặc khi viền đổi kích thước, viết hoa sẵn, nên {@code onDraw} chỉ việc vẽ
 * (không nối chuỗi). Java thuần.
 */
public final class TickerText {
    /** Dấu ngăn giữa các mục; chuỗi kết thúc bằng dấu này để lặp lại liền mạch. */
    public static final String SEPARATOR = " • ";

    private TickerText() {
    }

    /**
     * Các mục của dòng chữ chạy, đã viết hoa theo tiếng Việt: "SÂN KHẤU" rồi mỗi khu có chỗ một mục.
     *
     * @param stageLabel    chữ "SÂN KHẤU" (lấy từ strings.xml)
     * @param sectionFormat mẫu cho một khu, {@code %1$s} là tên khu, {@code %2$s} là giá, ví dụ "%1$s từ %2$s đ"
     * @param price         cách viết số tiền, ví dụ {@code PriceFormatter::format}
     */
    public static List<String> items(Venue venue, String stageLabel, String sectionFormat, LongFunction<String> price) {
        final Locale vietnamese = Locale.forLanguageTag("vi-VN");
        final List<String> items = new ArrayList<>();
        items.add(stageLabel.toUpperCase(vietnamese));
        final List<Section> sections = venue.getSections();
        for (int i = 0; i < sections.size(); i++) {
            final Section section = sections.get(i);
            final long cheapest = cheapestPrice(venue, section.id);
            if (cheapest < 0) {
                continue;  // khu không có chỗ nào thì không có giá để ghi
            }
            items.add(String.format(vietnamese, sectionFormat, section.name, price.apply(cheapest))
                    .toUpperCase(vietnamese));
        }
        return items;
    }

    /** Một vòng chữ chạy gồm mọi mục, mỗi mục theo sau bởi {@link #SEPARATOR}. */
    public static String build(Venue venue, String stageLabel, String sectionFormat, LongFunction<String> price) {
        final List<String> items = items(venue, stageLabel, sectionFormat, price);
        return fitToColumns(items, totalLength(items, items.size()));
    }

    /**
     * Dựng một vòng chữ dài đúng {@code columns} ký tự, để vừa khít chu vi viền khi vẽ bằng font đơn cách (mọi ký
     * tự rộng bằng nhau, như Space Mono): số ký tự = chu vi / bề rộng một ký tự.
     * <p>
     * Lấy các mục nguyên theo vòng (hết danh sách thì quay lại đầu) nhiều nhất có thể mà không quá {@code columns};
     * phần còn thiếu được bù bằng dấu cách chèn lần lượt sau các dấu "•", nên mục nào cũng hiện trọn vẹn và khoảng
     * cách chữ giữ nguyên. Nếu {@code columns} ngắn hơn cả mục đầu thì cắt mục đầu.
     */
    public static String fitToColumns(List<String> items, int columns) {
        if (items.isEmpty() || columns <= 0) {
            return "";
        }
        int count = 0;
        while (totalLength(items, count + 1) <= columns) {
            count++;
        }
        if (count == 0) {
            return items.get(0).substring(0, Math.min(columns, items.get(0).length()));
        }
        // Số dấu cách cần bù, chia đều cho các dấu ngăn; các dấu ngăn đầu nhận thêm một nếu chia còn dư.
        final int missing = columns - totalLength(items, count);
        final StringBuilder text = new StringBuilder(columns);
        for (int i = 0; i < count; i++) {
            text.append(items.get(i % items.size())).append(SEPARATOR);
            final int extra = missing / count + (i < missing % count ? 1 : 0);
            for (int k = 0; k < extra; k++) {
                text.append(' ');
            }
        }
        return text.toString();
    }

    /** Tổng số ký tự của {@code count} mục đầu (lấy theo vòng), mỗi mục kèm một dấu ngăn. */
    static int totalLength(List<String> items, int count) {
        int length = 0;
        for (int i = 0; i < count; i++) {
            length += items.get(i % items.size()).length() + SEPARATOR.length();
        }
        return length;
    }

    /** Giá thấp nhất trong một khu, hoặc −1 nếu khu không có chỗ nào. */
    static long cheapestPrice(Venue venue, String sectionId) {
        long cheapest = -1;
        final List<Spot> spots = venue.getSpots();
        for (int i = 0; i < spots.size(); i++) {
            final Spot spot = spots.get(i);
            if (spot.sectionId.equals(sectionId) && (cheapest < 0 || spot.price < cheapest)) {
                cheapest = spot.price;
            }
        }
        return cheapest;
    }
}
