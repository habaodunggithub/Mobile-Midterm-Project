package com.example.midtermproject.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.midtermproject.R;
import com.example.midtermproject.model.DisplayState;
import com.example.midtermproject.model.Section;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.Venue;
import com.example.midtermproject.state.VenueMapState;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.HashMap;
import java.util.Map;

/**
 * Bảng chi tiết ở đáy màn hình. Dạng thu gọn hiện chỗ đang xem và tóm tắt lựa chọn; kéo lên hoặc
 * bấm phần đầu để mở rộng. Lớp này chỉ <b>đọc</b> từ {@link VenueMapState} rồi hiển thị, không giữ
 * bản sao lựa chọn nào (dữ liệu đi một chiều).
 */
final class DetailSheet {
    /** Bảng mở rộng tối đa 60% chiều cao màn hình (AC-5.2). */
    private static final float MAX_HEIGHT_FRACTION = 0.6f;

    /** Báo cho sơ đồ biết bảng thu gọn đang che bao nhiêu pixel phía dưới. */
    interface PeekHeightListener {
        void onPeekHeightChanged(int peekHeightPx);
    }

    /** Việc người dùng yêu cầu từ bảng; Activity thực hiện bằng cách đi qua state và sơ đồ. */
    interface Callbacks {
        /** Bấm một dòng: đưa chỗ đó về giữa sơ đồ. */
        void onLocateRequested(Spot spot);

        /** Bấm ✕: bỏ chỗ khỏi lựa chọn. */
        void onRemoveRequested(Spot spot);
    }

    private final Context context;
    private final VenueMapState state;
    private final SpotListAdapter adapter;
    private final RecyclerView list;
    private final View sheet;
    private final View header;
    private final TextView focusInfo;
    private final TextView focusState;
    private final TextView summary;
    private final Button bookButton;
    private final Drawable chipBackground;
    private final BottomSheetBehavior<View> behavior;
    /** Tên phân khu theo mã, dựng một lần để hiển thị không phải duyệt danh sách mỗi lần. */
    private final Map<String, String> sectionNames = new HashMap<>();

    private int navigationInset;
    private PeekHeightListener peekHeightListener;

    DetailSheet(View sheet, Venue venue, VenueMapState state, Callbacks callbacks) {
        this.context = sheet.getContext();
        this.state = state;
        this.sheet = sheet;
        this.header = sheet.findViewById(R.id.sheet_header);
        this.focusInfo = sheet.findViewById(R.id.tv_focus_info);
        this.focusState = sheet.findViewById(R.id.tv_focus_state);
        this.summary = sheet.findViewById(R.id.tv_selection_summary);
        this.bookButton = sheet.findViewById(R.id.btn_book);
        // mutate(): chỉ đổi màu của chip này, không ảnh hưởng mọi nơi dùng chung drawable.
        this.chipBackground = focusState.getBackground().mutate();
        this.behavior = BottomSheetBehavior.from(sheet);

        for (Section section : venue.getSections()) {
            sectionNames.put(section.id, section.name);
        }
        adapter = new SpotListAdapter(context, venue, state, new SpotListAdapter.Listener() {
            @Override
            public void onSpotClicked(Spot spot) {
                callbacks.onLocateRequested(spot);
            }

            @Override
            public void onRemoveClicked(Spot spot) {
                callbacks.onRemoveRequested(spot);
            }
        });
        list = sheet.findViewById(R.id.rv_spots);
        list.setLayoutManager(new LinearLayoutManager(context));
        list.setAdapter(adapter);
        behavior.setMaxHeight((int) (context.getResources().getDisplayMetrics().heightPixels * MAX_HEIGHT_FRACTION));
        header.setOnClickListener(v -> toggle());
        // Chiều cao phần đầu đổi (ví dụ cỡ chữ hệ thống) thì peekHeight phải đổi theo.
        header.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom - top != oldBottom - oldTop) {
                updatePeekHeight();
            }
        });
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                updateHeaderAction();
                updateListVisibility(newState);
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
                // Danh sách hiện dần khi kéo lên: 0 ở dạng thu gọn, đủ rõ khi bảng mới lên được một phần tư.
                list.setAlpha(Math.max(0f, Math.min(1f, slideOffset * 4f)));
            }
        });
        // Sau khi xoay máy, BottomSheetBehavior tự khôi phục trạng thái cũ mà không báo callback nào,
        // nên đồng bộ lại danh sách và chữ hướng dẫn mỗi khi bảng được bố trí lại.
        sheet.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            updateListVisibility(behavior.getState());
            updateHeaderAction();
        });
        updateListVisibility(behavior.getState());
        updateHeaderAction();
        render();
    }

    void setPeekHeightListener(PeekHeightListener listener) {
        this.peekHeightListener = listener;
    }

    /**
     * Lề hệ thống: bảng đệm thêm hai bên (vết khuyết màn hình khi xoay ngang) và phía dưới (thanh điều
     * hướng) để nội dung không bị che, còn nền bảng vẫn kéo dài tới mép màn hình.
     */
    void setInsets(int leftPx, int rightPx, int bottomPx) {
        navigationInset = bottomPx;
        sheet.setPadding(leftPx, sheet.getPaddingTop(), rightPx, bottomPx);
        updatePeekHeight();
    }

    /** peekHeight = phần đầu + thanh điều hướng; phần thanh điều hướng nằm dưới đáy, không nhìn thấy. */
    private void updatePeekHeight() {
        if (header.getHeight() == 0) {
            return;
        }
        final int peek = header.getHeight() + navigationInset;
        if (peek != behavior.getPeekHeight()) {
            behavior.setPeekHeight(peek);
            if (peekHeightListener != null) {
                peekHeightListener.onPeekHeightChanged(peek);
            }
        }
    }

    void toggle() {
        behavior.setState(behavior.getState() == BottomSheetBehavior.STATE_EXPANDED
                ? BottomSheetBehavior.STATE_COLLAPSED : BottomSheetBehavior.STATE_EXPANDED);
    }

    void collapse() {
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    /** Vẽ lại mọi thứ từ state: gọi mỗi khi {@link VenueMapState} báo có thay đổi. */
    void render() {
        renderFocus();
        final int count = state.getSelectedCount();
        summary.setText(context.getString(R.string.sheet_selection_summary,
                count, VenueMapState.MAX_SELECTION, PriceFormatter.format(state.getSelectedTotal())));
        // Nút chỉ bật khi đã chọn ít nhất một chỗ (UC-10: lựa chọn rỗng thì không có hộp thoại).
        bookButton.setEnabled(count >= 1);
        bookButton.setText(count >= 1 ? context.getString(R.string.book_button, count)
                : context.getString(R.string.book_button_empty));
        adapter.refresh(context);
    }

    private void renderFocus() {
        final Spot spot = state.getFocusedSpot();
        if (spot == null) {
            focusInfo.setText(R.string.msg_tap_a_spot);
            focusState.setVisibility(View.GONE);
            return;
        }
        focusInfo.setText(context.getString(R.string.sheet_focus_info,
                sectionNames.get(spot.sectionId), spot.rowLabel, spot.label, PriceFormatter.format(spot.price)));
        showStateChip(state.getDisplayState(spot));
    }

    /** Chip luôn có chữ, nên trạng thái không chỉ dựa vào màu (độ tương phản mỗi cặp đã kiểm tra). */
    private void showStateChip(DisplayState displayState) {
        @StringRes final int label;
        @ColorRes final int background;
        @ColorRes final int foreground;
        switch (displayState) {
            case SELECTED:
                label = R.string.state_selected;
                background = R.color.chip_selected_bg;
                foreground = R.color.chip_selected_fg;
                break;
            case MINE:
                label = R.string.state_mine;
                background = R.color.chip_mine_bg;
                foreground = R.color.chip_mine_fg;
                break;
            case BOOKED:
                label = R.string.state_booked;
                background = R.color.chip_unavailable_bg;
                foreground = R.color.chip_unavailable_fg;
                break;
            case LOCKED:
                label = R.string.state_locked;
                background = R.color.chip_unavailable_bg;
                foreground = R.color.chip_unavailable_fg;
                break;
            default:
                label = R.string.state_available;
                background = R.color.chip_available_bg;
                foreground = R.color.chip_available_fg;
                break;
        }
        focusState.setText(label);
        focusState.setTextColor(ContextCompat.getColor(context, foreground));
        chipBackground.setTint(ContextCompat.getColor(context, background));
        focusState.setVisibility(View.VISIBLE);
    }

    /**
     * Dạng thu gọn chỉ hiện phần đầu. Thanh điều hướng kiểu cử chỉ trong suốt nên vùng dưới phần đầu
     * vẫn nhìn thấy được; ẩn danh sách để dòng đầu của nó không lấp ló ở đó (INVISIBLE vẫn giữ chỗ nên
     * chiều cao của bảng không đổi, và trình đọc màn hình bỏ qua danh sách đang ẩn).
     */
    private void updateListVisibility(int sheetState) {
        if (sheetState == BottomSheetBehavior.STATE_COLLAPSED) {
            list.setVisibility(View.INVISIBLE);
            list.setAlpha(0f);
        } else {
            list.setVisibility(View.VISIBLE);
            if (sheetState == BottomSheetBehavior.STATE_EXPANDED) {
                list.setAlpha(1f);
            }
        }
    }

    /** Người dùng trình đọc màn hình nghe "Mở rộng bảng chi tiết" hoặc "Thu gọn bảng chi tiết" khi bấm đúp. */
    private void updateHeaderAction() {
        final boolean expanded = behavior.getState() == BottomSheetBehavior.STATE_EXPANDED;
        ViewCompat.replaceAccessibilityAction(header,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                context.getString(expanded ? R.string.sheet_collapse_description : R.string.sheet_expand_description),
                null);
    }
}
