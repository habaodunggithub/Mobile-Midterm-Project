package com.example.midtermproject.state;

import java.util.List;

/**
 * Nơi lưu lựa chọn và vé của bạn giữa các lần mở app (ADR-08). Là interface để phần dùng nó không
 * phụ thuộc {@code android.*}; bản thật là {@code PrefsSessionStore} dùng SharedPreferences.
 */
public interface SessionStore {
    /** Đọc dữ liệu đã lưu; chưa có hoặc hỏng thì trả {@link SessionData#EMPTY}, không ném lỗi. */
    SessionData load();

    /** Ghi lựa chọn (đúng thứ tự) và vé của bạn, không chặn giao diện. */
    void save(List<String> selected, List<String> mine);

    /** Xóa mọi dữ liệu đã lưu (làm mới dữ liệu, AC-8.2). */
    void clear();
}
