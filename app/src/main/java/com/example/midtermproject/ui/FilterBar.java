package com.example.midtermproject.ui;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.IdRes;

import com.example.midtermproject.R;
import com.example.midtermproject.model.ZoneType;
import com.example.midtermproject.state.VenueMapState;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Điều khiển thanh lọc: ô tìm kiếm và chip khu. Mỗi thao tác của người dùng chỉ gọi
 * {@code state.setFilter(...)}; ngược lại, mỗi khi state báo bộ lọc đổi (ví dụ Xóa lọc hoặc khôi phục
 * khi mở lại), các control được cập nhật theo state. Nhờ vậy bộ lọc chỉ có một bản thật, nằm ở
 * {@link VenueMapState}, còn control chỉ là hình chiếu của nó.
 */
final class FilterBar {
    private final VenueMapState state;
    private final TextInputEditText search;
    private final ChipGroup zoneChips;
    /** Đang cập nhật control từ state: bỏ qua sự kiện do chính việc đó gây ra, để không lặp vô hạn. */
    private boolean syncing;

    FilterBar(View root, VenueMapState state) {
        this.state = state;
        this.search = root.findViewById(R.id.et_search);
        this.zoneChips = root.findViewById(R.id.chip_group_zone);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                if (!syncing) {
                    state.setFilter(state.getFilter().withQuery(text.toString()));
                }
            }
        });
        // Bấm nút tìm trên bàn phím chỉ để thu bàn phím: bộ lọc đã áp dụng từ lúc gõ.
        search.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) {
                return false;
            }
            hideKeyboard(view);
            view.clearFocus();
            return true;
        });
        zoneChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!syncing && !checkedIds.isEmpty()) {
                state.setFilter(state.getFilter().withZone(zoneOf(checkedIds.get(0))));
            }
        });

        state.addListener(this::syncFromState);
        syncFromState();
    }

    /** Đưa các control về đúng bộ lọc hiện tại của state. */
    private void syncFromState() {
        final String query = state.getFilter().rawQuery;
        syncing = true;
        // Chỉ đặt lại chữ khi khác, nếu không con trỏ nhảy về đầu mỗi lần người dùng gõ một ký tự.
        if (!search.getText().toString().equals(query)) {
            search.setText(query);
            search.setSelection(search.length());
        }
        zoneChips.check(chipIdOf(state.getFilter().zone));
        syncing = false;
    }

    private void hideKeyboard(View view) {
        final InputMethodManager keyboard =
                (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) {
            keyboard.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /** Chip "Tất cả" ứng với {@code null}, đúng như {@code SpotFilter.zone}. */
    private static ZoneType zoneOf(@IdRes int chipId) {
        if (chipId == R.id.chip_zone_standing) {
            return ZoneType.STANDING;
        }
        if (chipId == R.id.chip_zone_vip) {
            return ZoneType.VIP;
        }
        if (chipId == R.id.chip_zone_standard) {
            return ZoneType.STANDARD;
        }
        return null;
    }

    @IdRes
    private static int chipIdOf(ZoneType zone) {
        if (zone == null) {
            return R.id.chip_zone_all;
        }
        switch (zone) {
            case STANDING:
                return R.id.chip_zone_standing;
            case VIP:
                return R.id.chip_zone_vip;
            default:
                return R.id.chip_zone_standard;
        }
    }
}
