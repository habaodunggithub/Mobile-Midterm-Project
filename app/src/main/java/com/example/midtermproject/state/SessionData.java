package com.example.midtermproject.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dữ liệu được lưu qua các lần mở app (REQUIREMENTS 7.3): mã các chỗ đang chọn theo thứ tự chọn và mã
 * các vé của bạn. Bất biến. Các mã chưa được kiểm; {@link VenueMapState#restore} sẽ loại mã sai.
 */
public final class SessionData {
    /** Dữ liệu rỗng, dùng khi chưa lưu gì (AC-7.7) hoặc dữ liệu hỏng hoàn toàn. */
    public static final SessionData EMPTY =
            new SessionData(Collections.<String>emptyList(), Collections.<String>emptyList());

    public final List<String> selected;
    public final List<String> mine;

    public SessionData(List<String> selected, List<String> mine) {
        this.selected = Collections.unmodifiableList(new ArrayList<>(selected));
        this.mine = Collections.unmodifiableList(new ArrayList<>(mine));
    }
}
