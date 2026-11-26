package com.example.midtermproject.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
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
    /** Thời gian tổng tiền chạy số từ giá trị cũ tới giá trị mới (Q43). */
    private static final long TOTAL_COUNT_MS = 380L;

    /**
     * Báo cho sơ đồ biết dạng thu gọn của bảng đang che bao nhiêu pixel phía dưới khung sơ đồ. Từ Q47 dạng thu gọn là
     * viên thuốc ở góc, nên con số là chiều cao viên thuốc cộng khoảng cách từ nó tới đáy khung.
     */
    interface PeekHeightListener {
        void onPeekHeightChanged(int peekHeightPx);
    }

    /** Việc người dùng yêu cầu từ bảng; Activity thực hiện bằng cách đi qua state và sơ đồ. */
    interface Callbacks {
        /** Bấm một dòng: đưa chỗ đó về giữa sơ đồ. */
        void onLocateRequested(Spot spot);

        /** Bấm ✕: bỏ chỗ khỏi lựa chọn. */
        void onRemoveRequested(Spot spot);

        /** Bấm "Đặt N vé": Activity hỏi xác nhận rồi đặt. */
        void onBookRequested();
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
    /** Viên thuốc tóm tắt: dạng thu gọn của bảng, chỉ hiện khi bảng ẩn (Q47). */
    private final Button pill;
    private final int pillBaseMarginPx;
    private final Drawable chipBackground;
    private final BottomSheetBehavior<View> behavior;
    /** Tên phân khu theo mã, dựng một lần để hiển thị không phải duyệt danh sách mỗi lần. */
    private final Map<String, String> sectionNames = new HashMap<>();

    /** Tổng tiền đang hiện trên dòng tóm tắt; −1 là chưa hiện lần nào (lần đầu hiện thẳng, không chạy số). */
    private long shownTotal = -1L;
    private ValueAnimator totalAnimator;
    /** Tắt khi người dùng tắt hiệu ứng động của hệ thống: tổng tiền hiện thẳng, không chạy số. */
    private boolean animated = true;
    private int navigationInset;
    private PeekHeightListener peekHeightListener;

    DetailSheet(View sheet, Button pill, Venue venue, VenueMapState state, Callbacks callbacks) {
        this.context = sheet.getContext();
        this.pill = pill;
        this.pillBaseMarginPx = ((ViewGroup.MarginLayoutParams) pill.getLayoutParams()).bottomMargin;
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
        pill.setOnClickListener(v -> behavior.setState(BottomSheetBehavior.STATE_EXPANDED));
        pill.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom - top != oldBottom - oldTop) {
                notifyCoveredHeight();
            }
        });
        // Mở app thì bảng ẩn, chỉ có viên thuốc. Sau khi xoay máy, BottomSheetBehavior tự khôi phục trạng thái cũ
        // (việc khôi phục chạy sau onCreate nên ghi đè dòng này).
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bookButton.setOnClickListener(v -> callbacks.onBookRequested());
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
                updatePill(newState, true);
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
            updatePill(behavior.getState(), false);
        });
        updateListVisibility(behavior.getState());
        updateHeaderAction();
        updatePill(behavior.getState(), false);
        render();
    }

    void setPeekHeightListener(PeekHeightListener listener) {
        this.peekHeightListener = listener;
        notifyCoveredHeight();
    }

    /**
     * Phần đáy khung sơ đồ bị viên thuốc che = chiều cao viên thuốc + lề dưới gốc của nó (lề thanh điều hướng không
     * tính, vì khung sơ đồ đã kết thúc phía trên thanh điều hướng). Viên thuốc đang GONE (bảng mở sẵn sau khi xoay
     * máy) thì chưa có chiều cao, nên đo thử nó.
     */
    private void notifyCoveredHeight() {
        if (peekHeightListener == null) {
            return;
        }
        int height = pill.getHeight();
        if (height == 0) {
            pill.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            height = pill.getMeasuredHeight();
        }
        peekHeightListener.onPeekHeightChanged(height + pillBaseMarginPx);
    }

    /**
     * Lề hệ thống: bảng đệm thêm hai bên (vết khuyết màn hình khi xoay ngang) và phía dưới (thanh điều
     * hướng) để nội dung không bị che, còn nền bảng vẫn kéo dài tới mép màn hình.
     */
    void setInsets(int leftPx, int rightPx, int bottomPx) {
        navigationInset = bottomPx;
        sheet.setPadding(leftPx, sheet.getPaddingTop(), rightPx, bottomPx);
        final ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) pill.getLayoutParams();
        params.bottomMargin = pillBaseMarginPx + bottomPx;
        pill.setLayoutParams(params);
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
        }
    }

    /** Bảng đang mở rộng thì ẩn đi (về viên thuốc), còn lại thì mở rộng (Q47: không còn mức hai dòng ở giữa). */
    void toggle() {
        behavior.setState(behavior.getState() == BottomSheetBehavior.STATE_EXPANDED
                ? BottomSheetBehavior.STATE_HIDDEN : BottomSheetBehavior.STATE_EXPANDED);
    }

    void collapse() {
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    /** Vẽ lại mọi thứ từ state: gọi mỗi khi {@link VenueMapState} báo có thay đổi. */
    void render() {
        renderFocus();
        final int count = state.getSelectedCount();
        renderSummary(count, state.getSelectedTotal());
        // Nút chỉ bật khi đã chọn ít nhất một chỗ (UC-10: lựa chọn rỗng thì không có hộp thoại).
        bookButton.setEnabled(count >= 1);
        bookButton.setText(count >= 1 ? context.getString(R.string.book_button, count)
                : context.getString(R.string.book_button_empty));
        renderPill(count, state.getSelectedTotal());
        adapter.refresh(context);
    }

    /** Chữ trên viên thuốc: "N chỗ · T đ", hoặc "Chưa chọn chỗ" khi chưa chọn gì (viên thuốc vẫn hiện, Q47). */
    private void renderPill(int count, long total) {
        final String text = count == 0 ? context.getString(R.string.pill_empty)
                : context.getString(R.string.pill_summary, count, PriceFormatter.format(total));
        pill.setText(text);
        pill.setContentDescription(context.getString(R.string.pill_description, text));
    }

    /**
     * Viên thuốc chỉ hiện khi bảng ẩn. Hiện thì phóng từ 60% lên 100% và rõ dần (200ms); bảng bắt đầu kéo lên thì viên
     * thuốc mờ đi nhanh hơn (120ms), rồi mới GONE để không còn nhận chạm.
     */
    private void updatePill(int sheetState, boolean animate) {
        final boolean show = sheetState == BottomSheetBehavior.STATE_HIDDEN;
        pill.animate().cancel();
        if (!animate || !animated) {
            pill.setVisibility(show ? View.VISIBLE : View.GONE);
            pill.setAlpha(1f);
            pill.setScaleX(1f);
            pill.setScaleY(1f);
            return;
        }
        if (show) {
            pill.setVisibility(View.VISIBLE);
            pill.setAlpha(0f);
            pill.setScaleX(0.6f);
            pill.setScaleY(0.6f);
            pill.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200L)
                    .setInterpolator(new OvershootInterpolator()).withEndAction(null).start();
        } else if (pill.getVisibility() == View.VISIBLE) {
            pill.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(120L)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> pill.setVisibility(View.GONE)).start();
        }
    }

    /**
     * Dòng "Đã chọn N/8 · Tổng T đ". Khi tổng đổi, số tiền chạy từ giá trị cũ tới giá trị mới trong 380ms, chậm dần ở
     * cuối, để mắt thấy được tổng vừa tăng hay giảm. Đổi tiếp giữa chừng thì chạy tiếp từ số đang hiện.
     */
    private void renderSummary(int count, long total) {
        if (totalAnimator != null) {
            totalAnimator.cancel();
            totalAnimator = null;
        }
        if (shownTotal < 0 || shownTotal == total || !animated) {
            showSummary(count, total);
            return;
        }
        final long from = shownTotal;
        totalAnimator = ValueAnimator.ofFloat(0f, 1f);
        totalAnimator.setDuration(TOTAL_COUNT_MS);
        totalAnimator.setInterpolator(new DecelerateInterpolator());
        totalAnimator.addUpdateListener(animation -> {
            final float f = (float) animation.getAnimatedValue();
            // Làm tròn tới nghìn đồng để các số trung gian trông như giá tiền thật.
            final long value = Math.round((from + (total - from) * f) / 1000.0) * 1000L;
            showSummary(count, f >= 1f ? total : value);
        });
        totalAnimator.start();
    }

    void setAnimated(boolean animated) {
        this.animated = animated;
    }

    private void showSummary(int count, long total) {
        shownTotal = total;
        summary.setText(context.getString(R.string.sheet_selection_summary,
                count, VenueMapState.MAX_SELECTION, PriceFormatter.format(total)));
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
