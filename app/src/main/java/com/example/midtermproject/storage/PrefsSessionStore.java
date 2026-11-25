package com.example.midtermproject.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.midtermproject.state.SessionCodec;
import com.example.midtermproject.state.SessionData;
import com.example.midtermproject.state.SessionStore;

import java.util.List;

/**
 * Lưu lựa chọn và vé của bạn bằng SharedPreferences, tệp {@value #FILE_NAME} (REQUIREMENTS 7.3).
 * Mỗi danh sách là một chuỗi mã nối bằng dấu phẩy (xem {@link SessionCodec}). Ghi bằng {@code apply()}
 * để không chặn luồng giao diện.
 */
public final class PrefsSessionStore implements SessionStore {
    static final String FILE_NAME = "venue_session";
    private static final String KEY_SELECTED = "selected_ids";
    private static final String KEY_MINE = "my_ticket_ids";

    private final SharedPreferences prefs;

    public PrefsSessionStore(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public SessionData load() {
        // getString trả null khi chưa có khóa, và decode(null) cho danh sách rỗng (AC-7.7).
        return new SessionData(
                SessionCodec.decode(prefs.getString(KEY_SELECTED, null)),
                SessionCodec.decode(prefs.getString(KEY_MINE, null)));
    }

    @Override
    public void save(List<String> selected, List<String> mine) {
        prefs.edit()
                .putString(KEY_SELECTED, SessionCodec.encode(selected))
                .putString(KEY_MINE, SessionCodec.encode(mine))
                .apply();
    }

    @Override
    public void clear() {
        prefs.edit().clear().apply();
    }
}
