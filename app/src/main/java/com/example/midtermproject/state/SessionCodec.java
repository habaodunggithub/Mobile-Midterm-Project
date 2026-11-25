package com.example.midtermproject.state;

import java.util.ArrayList;
import java.util.List;

/**
 * Đổi danh sách mã chỗ sang một chuỗi để lưu và ngược lại (REQUIREMENTS 7.3). Các mã nối bằng dấu
 * phẩy và giữ đúng thứ tự: lựa chọn cần giữ thứ tự chọn nên không dùng {@code StringSet} (không có
 * thứ tự). Mã chỗ chỉ gồm A–Z, 0–9 và '-' nên dấu phẩy không gây nhầm.
 * <p>
 * Java thuần. Giải mã chỉ tách chuỗi, còn việc kiểm mã có thật hay không là của
 * {@link VenueMapState#restore}. Dữ liệu null, rỗng hay hỏng chỉ cho ra danh sách rỗng hoặc ngắn hơn,
 * không bao giờ ném lỗi (AC-7.6).
 */
public final class SessionCodec {
    private static final String SEPARATOR = ",";

    private SessionCodec() {
    }

    /** Nối các mã bằng dấu phẩy; danh sách null hoặc rỗng cho chuỗi rỗng. Mã null được bỏ qua. */
    public static String encode(List<String> ids) {
        final StringBuilder builder = new StringBuilder();
        if (ids == null) {
            return "";
        }
        for (String id : ids) {
            if (id == null || id.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(SEPARATOR);
            }
            builder.append(id);
        }
        return builder.toString();
    }

    /** Tách chuỗi thành các mã theo đúng thứ tự; bỏ khoảng trắng thừa và các phần rỗng. */
    public static List<String> decode(String raw) {
        final List<String> ids = new ArrayList<>();
        if (raw == null) {
            return ids;
        }
        for (String part : raw.split(SEPARATOR)) {
            final String id = part.trim();
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return ids;
    }
}
