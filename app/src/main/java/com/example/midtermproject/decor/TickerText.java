package com.example.midtermproject.decor;

import com.example.midtermproject.model.Section;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.Venue;

import java.util.List;
import java.util.Locale;
import java.util.function.LongFunction;

/**
 * Nội dung dòng chữ chạy quanh viền sticker của sơ đồ, giống dòng credit chạy quanh mép bìa đĩa
 * trong ảnh tham chiếu (REQUIREMENTS Q42). Lấy từ dữ liệu thật: "SÂN KHẤU", rồi từng phân khu kèm giá
 * thấp nhất của khu đó, ví dụ {@code "SÂN KHẤU • STANDING A TỪ 900.000 Đ • … • "}.
 * <p>
 * Chuỗi được dựng một lần khi gắn sơ đồ, viết hoa sẵn, nên {@code onDraw} chỉ việc vẽ (không nối chuỗi).
 * Java thuần.
 */
public final class TickerText {
    /** Dấu ngăn giữa các mục; chuỗi kết thúc bằng dấu này để lặp lại liền mạch. */
    public static final String SEPARATOR = " • ";

    private TickerText() {
    }

    /**
     * @param stageLabel    chữ "SÂN KHẤU" (lấy từ strings.xml)
     * @param sectionFormat mẫu cho một khu, {@code %1$s} là tên khu, {@code %2$s} là giá, ví dụ "%1$s từ %2$s đ"
     * @param price         cách viết số tiền, ví dụ {@code PriceFormatter::format}
     * @return một vòng chữ chạy, đã viết hoa theo tiếng Việt
     */
    public static String build(Venue venue, String stageLabel, String sectionFormat, LongFunction<String> price) {
        final Locale vietnamese = Locale.forLanguageTag("vi-VN");
        final StringBuilder text = new StringBuilder(stageLabel).append(SEPARATOR);
        final List<Section> sections = venue.getSections();
        for (int i = 0; i < sections.size(); i++) {
            final Section section = sections.get(i);
            final long cheapest = cheapestPrice(venue, section.id);
            if (cheapest < 0) {
                continue;  // khu không có chỗ nào thì không có giá để ghi
            }
            text.append(String.format(vietnamese, sectionFormat, section.name, price.apply(cheapest)))
                    .append(SEPARATOR);
        }
        return text.toString().toUpperCase(vietnamese);
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
